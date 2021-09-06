package miner;

import explorer.FrequentItemsetAnalysis;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.fpm.AssociationRules;
import org.apache.spark.mllib.fpm.FPGrowth;
import org.apache.spark.mllib.fpm.FPGrowthModel;
import visitors.BaseAUGLabelProvider;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Miner {
    private JavaSparkContext sc;
    private final double minSupp;
    private final double minConf;
    private final int minNumOfItemsets;
    private long datasetSize;
    private long initialNumOfProjects;
    private long totalItemsets;
    private long uniqueItemsets;
    private List<Set<String>> inputData;

    private List<FrequentItemset> frequentItemsets = new ArrayList<>();
    final private List<AssociationRule> allAssociationRules = new ArrayList<>();

    public Miner(double minSupp, double minConf, int minNumOfItemsets) {
        if (minSupp < 10) {
            System.out.println("[Miner()] Support too low!");
        }
        if (minConf < 0.3) {
            System.out.println("[Miner()] Confidence too low!");
        }

        this.minSupp = minSupp;
        this.minConf = minConf;

        SparkConf conf = new SparkConf();
        conf.setAppName("annotation-miner-v3");
        conf.set("spark.master", "local");
        this.sc = new JavaSparkContext(conf);
        this.sc.setLogLevel("ERROR");
        this.minNumOfItemsets = minNumOfItemsets;
    }

    public List<Set<String>> getInputData() {
        return inputData;
    }

    public void trainPerProject(HashMap<String, List<Itemset>> rawProjectUsages) {
        int numProjectsSkipped = 0;
        List<Set<String>> allUsages = new ArrayList<>();
        this.initialNumOfProjects = rawProjectUsages.size();

        // Obtain frequent itemsets for each project
        for (Map.Entry<String, List<Itemset>> usage : rawProjectUsages.entrySet()) {
            if (usage.getValue().size() < this.minNumOfItemsets) {
                ++numProjectsSkipped;
                continue;
            }

            // Convert to List<Set<String>>
            List<Set<String>> projectUsages = usage.getValue()
                .stream()
                .map(Itemset::getItems)
                .collect(Collectors.toList());

            Map<Set<String>, Integer> itemsetOccurenceCount = new HashMap<>();
            for (Set<String> itemset : projectUsages) {
               itemsetOccurenceCount.put(itemset, itemsetOccurenceCount.getOrDefault(itemset, 0) + 1);
            }

            allUsages.addAll(itemsetOccurenceCount.keySet());
        }
        this.datasetSize = rawProjectUsages.size() - numProjectsSkipped;

        this.totalItemsets = rawProjectUsages
            .values()
            .stream()
            .map(List::size)
            .reduce(0, Integer::sum);
        this.uniqueItemsets = allUsages.size();
        this.inputData = allUsages;

        // Run the model
        List<FPGrowth.FreqItemset<String>> freqItemsets = this.generateItemsets(allUsages);

        // Remove itemsets without library prefix (import) and convert them to objects of type FrequentItemset
        freqItemsets = freqItemsets.stream()
            .filter(fi -> fi.javaItems().stream().anyMatch(item -> Arrays.stream(Configuration.apiLibPrefixes).anyMatch(item::contains)))
            .collect(Collectors.toList());

        // Convert
        this.frequentItemsets = freqItemsets.stream()
            .map(fpi -> FrequentItemset.toFrequentItemset(fpi.javaItems(), fpi.freq()))
            .collect(Collectors.toList());
    }

    public HashMap<String, CombinedResult> trainPerSubApi(HashMap<String, List<Itemset>> allSubApiUsages) {
        // Represents number of sub-APIs
        this.initialNumOfProjects = allSubApiUsages.entrySet().size();

        // Return stuff
        HashMap<String, CombinedResult> results = new HashMap<>();

        SparkConf conf = new SparkConf();
        conf.setAppName("annotation-miner-v2");
        conf.set("spark.master", "local");
        conf.set("spark.driver.allowMultipleContexts", "true");
        conf.set("spark.network.timeout", "240");
        for (Map.Entry<String, List<Itemset>> subApiUsages : allSubApiUsages.entrySet()) {
            System.out.println("Now processing " + subApiUsages.getKey() + " sub-API usages");
            this.sc = new JavaSparkContext(conf);
            this.sc.setLogLevel("ERROR");

            // Obtain frequent itemsets for each project
            this.datasetSize = subApiUsages.getValue().size();
            this.totalItemsets = subApiUsages.getValue().size();
            this.uniqueItemsets = -1;
            this.inputData = subApiUsages.getValue().stream().map(Itemset::getItems).collect(Collectors.toList());

            // Run the model
            List<FPGrowth.FreqItemset<String>> freqItemsets = this.generateItemsets(this.inputData);

            // Generate association rules from NON-maximal frequent itemsets
            List<AssociationRules.Rule<String>> assocRules = this.generateCandidateRules(freqItemsets);

            // Remove itemsets without library prefix (import) and convert them to objects of type FrequentItemset
            freqItemsets = freqItemsets.stream()
                .filter(fi -> fi.javaItems().stream().anyMatch(item -> Arrays.stream(Configuration.apiLibPrefixes).anyMatch(item::contains)))
                .collect(Collectors.toList());

            // Get only maximal itemsets
            freqItemsets = getMaximalFreqItemsets(freqItemsets);

            // Convert
            this.frequentItemsets = freqItemsets.stream()
                .map(fpi -> FrequentItemset.toFrequentItemset(fpi.javaItems(), fpi.freq()))
                .collect(Collectors.toList());

            // Get association rules (reused later)
            List<AssociationRule> associationRules = assocRules.stream()
                .map(ar -> AssociationRule.toAssociationRule(
                    ar.javaAntecedent(),
                    ar.javaConsequent(),
                    ar.confidence())
                )
                .collect(Collectors.toList());

            // Keep all association rules in a separate place
            this.allAssociationRules.addAll(associationRules);

            results.put(subApiUsages.getKey(), new CombinedResult(this.frequentItemsets, this.datasetSize));

            System.out.println("Finishes processing, now unpersisting RDDs...");
            this.sc.getPersistentRDDs().values().forEach(t -> t.unpersist(true));
            this.sc.close();
        }

        return results;
    }

    public static List<AssociationRule> getOneRulePerFreqItemset(List<AssociationRule> rules, List<FrequentItemset> frequentItemsets) {
        // Gets one association rule with highest confidence where
        // the elements in the rule match (==) the elements in the frequent itemset
        Map<FrequentItemset, AssociationRule> oneRulePerItemset = new HashMap<>();
        Set<FrequentItemset> itemsetsWithoutExistingRules = new HashSet<>();

        for (FrequentItemset fi : frequentItemsets) {
            boolean foundRule = false;

            // See if any association rule exists ...
            for (AssociationRule rule : rules) {
                // If items are same and rule is semantically correct ...
                if (rule.getAllItems().equals(fi.getItems()) && Heuristics.isSemanticallyOk(rule)) {
                    // ... we now found at least one rule for this frequent itemset
                    foundRule = true;

                    // ... and no rule corresp. exists, just put it in the map
                    if (!oneRulePerItemset.containsKey(fi)) {
                        oneRulePerItemset.put(fi, rule);
                    }
                    // ... and confidence is higher than the existing one, then update the rule
                    else if (rule.confidence() > oneRulePerItemset.get(fi).confidence()) {
                        oneRulePerItemset.put(fi, rule);
                    }
                }
            }

            // If no association rule (due to removal of args and previous edits of frequent itemsets), then create one
            if (!foundRule) itemsetsWithoutExistingRules.add(fi);
        }

        // Put according to priority
        // 1. hasParam, definedIn, declaredInBeans
        // 2. hasType, hasReturnType, extends, implements
        // 3. annotatedWith
        String[] priorityStructuresForConsequent = {
            "hasParam",
            "definedIn",
            "declaredInBeans"
        };

        // Supply missing rules from some frequent itemsets
        for (FrequentItemset fi : itemsetsWithoutExistingRules) {
            // Make sure the frequent itemset is not there
            assert !oneRulePerItemset.containsKey(fi);

            Set<String> items = fi.getItems();
            int initialSize = items.size();

            // Set left-hand and right-hand (if-then) sides
            List<String> antecedent = new ArrayList<>();
            List<String> consequent = new ArrayList<>();

            boolean foundConsequent = false;
            for (String priorityStructure : priorityStructuresForConsequent) {
                for (String item : items) {
                    if (item.contains(priorityStructure)) {
                        foundConsequent = true;
                        consequent.add(item);
                        break;
                    }
                }
                if (foundConsequent) break;
            }

            // If there is no hasParam/definedIn/declaredInBeans, then just add class relationship, preferably
            if (!foundConsequent) {
                for (String item : items) {
                    if (item.contains("Class")) {
                        consequent.add(item);
                        foundConsequent = true;
                        break;
                    }
                }
            }

            // If class is not there, then just add any
            if (!foundConsequent) {
                for (String item : items) {
                    // Just add 1 item
                    consequent.add(item);
                    break;
                }
            }

            // Remove consequent from antecedent and put it in `consequent` array
            items.remove(consequent.get(0));
            antecedent = new ArrayList<>(items);

            // To make sure we did not forget to remove or add things
            assert antecedent.size() + consequent.size() == initialSize;
            oneRulePerItemset.put(fi, AssociationRule.toAssociationRule(antecedent, consequent, -1.0));
        }

        return new ArrayList<>(oneRulePerItemset.values());
    }

    private List<FPGrowth.FreqItemset<String>> getMaximalFreqItemsets(List<FPGrowth.FreqItemset<String>> input) {
        List<FPGrowth.FreqItemset<String>> originals = new ArrayList<>(input);
        List<FPGrowth.FreqItemset<String>> toDelete = new ArrayList<>();
        HashSet<Integer> marked = new HashSet<>();

        for (int i = 0; i < originals.size(); ++i) {
            FPGrowth.FreqItemset<String> I1 = originals.get(i);
            if (marked.contains(i)) {
                continue;
            }

            for (int j = i + 1; j < originals.size(); ++j) {
                FPGrowth.FreqItemset<String> I2 = originals.get(j);
                if (marked.contains(j) || i == j) {
                    continue;
                }

                // Check if R1 is superset of R2
                if (I1.javaItems().containsAll(I2.javaItems())) {
                    toDelete.add(I2);
                    marked.add(j);
                }
                else if (I2.javaItems().containsAll(I1.javaItems())) {
                    toDelete.add(I1);
                    marked.add(i);
                    break;
                }
            }
        }

        originals.removeAll(toDelete);

        return originals;
    }

    public void printStats() {
        System.out.println("\nTook one itemsets if multiple same ones from each project!");
        System.out.println("\tTotal itemsets    : " + this.totalItemsets);
        System.out.println("\tUnique itemsets   : " + this.uniqueItemsets);
        System.out.println("\tFrequent itemsets : " + this.frequentItemsets.size());
        System.out.println("\tProjects used     : " + this.datasetSize);
        System.out.println("\tProjects given    : " + this.initialNumOfProjects);
    }

    public void train(HashMap<String, List<Itemset>> rawUsages) {
        trainPerProject(rawUsages);
    }

    private List<FPGrowth.FreqItemset<String>> generateItemsets(List<Set<String>> usages) {
        // Create RDD for our transaction set
        JavaRDD<Set<String>> transactions = sc.parallelize(usages);

        // Run FPGrowth model

//        System.out.println("[fitItemsets()] Relative support is " + this.minSupp / this.datasetSize);
//        FPGrowth fpg = new FPGrowth().setMinSupport(this.minSupp / this.datasetSize);
        System.out.println("[fitItemsets()] Relative support is " + this.minSupp);
        FPGrowth fpg = new FPGrowth().setMinSupport(this.minSupp);

        FPGrowthModel<String> model = fpg.run(transactions);

        // Get frequent itemsets
        return model.freqItemsets().toJavaRDD().collect();
    }

    public List<AssociationRules.Rule<String>> generateCandidateRules(List<FPGrowth.FreqItemset<String>> freqItemsets) {
        // Convert frequent itemsets to RDD
        JavaRDD<FPGrowth.FreqItemset<String>> freqItemsetsRDD = sc.parallelize(freqItemsets);

        // Get association rules
        AssociationRules arules = new AssociationRules().setMinConfidence(this.minConf);
        JavaRDD<AssociationRules.Rule<String>> results = arules.run(freqItemsetsRDD);

        return results.collect();
    }

    public List<FrequentItemset> getFrequentItemsets() {
        return this.frequentItemsets;
    }

    public List<AssociationRule> getAllAssociationRules() {
        return this.allAssociationRules;
    }

    public long getDatasetSize() {
        return datasetSize;
    }
}
