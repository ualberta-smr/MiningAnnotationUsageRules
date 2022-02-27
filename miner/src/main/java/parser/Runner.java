package parser;

import miner.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import util.AggregateData;
import util.ResultPrinter;
import util.labeler.RulesDatabase;

public class Runner {
    private static AnnotationUsageGraphBuilder builder;

    public static void main(String[] args) throws IOException {
        // Get the path to all input user projects that use target library
        String projectsDir = getProjectsDirectory();

        // Read in all java files
        long startProcessingTime = System.currentTimeMillis();
        readProjectsFiles(projectsDir);
        long endProcessingTime = System.currentTimeMillis();
        System.out.println("Parsed projects in " + (endProcessingTime - startProcessingTime) + " milliseconds");

        // Parse all annotations
        List<Itemset> allUsages = builder.getAllUsageGraphs();

        System.out.println("*****************************************");
        System.out.println("******** Finish Phase I: Parsing ********");
        System.out.println("*****************************************");
        System.out.println("There are " + allUsages.size() + " usages (in total!)");
        System.out.println("There are " + AnnotationUsageGraphBuilder.projectsWithCrossClassUsage.size() + " projects that have xclass rels");
        System.out.println("There are " + AnnotationUsageGraphBuilder.numOfFieldTypeDecl + " files that have field type decl");
        System.out.println("There are " + AnnotationUsageGraphBuilder.numOfParamTypeDecl + " files that have param type decl");

        // TODO: Comes with v0.0.6
        List<Itemset> usagesWithoutProjectDuplicates = takeOneItemsetFromEachProject(builder.getUsagesAsMap());
        System.out.println("There are " + usagesWithoutProjectDuplicates.size() + " usages (without duplicates in projects!)");
        HashMap<String, List<Itemset>> usagesByAnnotations = divideUsagesByAnnotations(Configuration.libSubApiRegex, usagesWithoutProjectDuplicates);
        getFrequencyDist(usagesByAnnotations);

        int itemsets = 0;
        for (Map.Entry<String, List<Itemset>> entry : usagesByAnnotations.entrySet()) {
            System.out.println("Annotation " + entry.getKey() + " appears in " + entry.getValue().size() + " transactions (usages).");
            itemsets += entry.getValue().size();
        }
        System.out.println("There are " + itemsets + " itemsets with at least 1 MP element");

        /*
         * Association rule mining using FP-Growth algorithm
         */
        if (Configuration.mineBySubApi) {
            mineByAnnotations(usagesByAnnotations);
        } else {
            System.out.println("Mining all projects is deprecated.");
        }
        System.out.println("It took " + (endProcessingTime - startProcessingTime) + " to parse and mine stuff all projects.");
    }

    private static void getFrequencyDist(HashMap<String, List<Itemset>> usagesBySubApi) {


        PrintStream originalOut = System.out;

        try {
            PrintStream dump = new PrintStream("dump_input_itemsets.txt");
            System.setOut(dump);

            for (Map.Entry<String, List<Itemset>> subApiItemsets : usagesBySubApi.entrySet()) {
                String subApi = subApiItemsets.getKey();
                System.out.println("Printing frequencies for sub-API: " + subApi);

                Map<Itemset, Integer> freqAcrossProjs = new HashMap<>();
                for (Itemset itemset : subApiItemsets.getValue()) {
                    freqAcrossProjs.put(itemset, freqAcrossProjs.getOrDefault(itemset, 0) + 1);
                }

    //            Map<Integer, Integer> freqDistrib = new HashMap<>();
    //            for (Integer freq : freqAcrossProjs.values()) {
    //                freqDistrib.put(freq, freqDistrib.getOrDefault(freq, 0) + 1);
    //            }

                int i = 1;
                for (Map.Entry<Itemset, Integer> freqOfItemset : freqAcrossProjs.entrySet()) {
                    String items = "{" + String.join(",", freqOfItemset.getKey().getItems()) + "}";
                    // Prints "id_, {...items}, size, freq"
                    System.out.println("id" + i + ","
                        + freqOfItemset.getKey().size() + ","
                        + freqOfItemset.getValue() + ","
                        + items
                    );
                    ++i;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            System.setOut(originalOut);
        }
    }

    private static List<Itemset> takeOneItemsetFromEachProject(HashMap<String, List<Itemset>> rawProjectUsages) {
        List<Itemset> allUsages = new ArrayList<>();

        Set<String> projectsUsed = new HashSet<>();
        Map<String, Integer> subApiProjectFreq = new HashMap<>();
        for (String subApi : Configuration.subApiLibPrefixes) {
            int freq = 0;

            // For each project
            for (Map.Entry<String, List<Itemset>> usage : rawProjectUsages.entrySet()) {
                if (usage.getValue().size() < 1) {
                    continue;
                }

                Set<Itemset> uniqueItemsetsPerProject = new HashSet<>(usage.getValue());
                boolean found = false;
                for (Itemset itemset : uniqueItemsetsPerProject) {
                    for (String item : itemset.getItems()) {
                        if (item.contains(subApi)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }

                projectsUsed.add(usage.getKey());

                // Calculates once per project
                if (found) {
                    ++freq;
                }
            }

            if (subApiProjectFreq.containsKey(subApi)) {
                System.out.println("Oh uh");
                System.exit(1);
            }

            subApiProjectFreq.put(subApi, freq);
        }

        System.out.println("-------------------------");
        for (String subApi : subApiProjectFreq.keySet()) {
            System.out.println("Sub-API " + subApi + " appears in " + subApiProjectFreq.get(subApi) + " projects");
        }
        System.out.println("-------------------------");


        // Obtain frequent itemsets for each project
        int numProjectsSkipped = 0;
        for (Map.Entry<String, List<Itemset>> usage : rawProjectUsages.entrySet()) {
            if (usage.getValue().size() < 1) {
                ++numProjectsSkipped;
                continue;
            }

            Set<Itemset> uniqueItemsetsPerProject = new HashSet<>(usage.getValue());
            allUsages.addAll(uniqueItemsetsPerProject);
        }
        System.out.println("Skipped " + numProjectsSkipped + " projects because they are empty!");

        return allUsages;
    }

    private static void mineByAnnotations(HashMap<String, List<Itemset>> usagesBySubApi) throws FileNotFoundException {
        Miner miner = new Miner(
                Configuration.minSupp,
                Configuration.minConf,
                -1
        );

        // 1. We do not want to mine patterns of usages of annotations that appear very little,
        //    In other words, they should appear in at least X transactions.
        // 2. Remove usages that have non-target sub-APIs.
        HashMap<String, List<Itemset>> relevantUsages = new HashMap<>();
        usagesBySubApi.entrySet()
            .stream()
            .filter(entry -> entry.getValue().size() >= 10)
            .filter(entry -> Configuration.subApiLibPrefixes.stream().anyMatch(subApi -> entry.getKey().contains(subApi)))
            .forEach(entry -> relevantUsages.put(entry.getKey(), entry.getValue()));

        // Mine
        long startMiningTime = System.currentTimeMillis();
        HashMap<String, CombinedResult> results = miner.trainPerAnnotation(relevantUsages);
        long endMiningTime = System.currentTimeMillis();

        // TODO: comes in 0.0.7
        // Remove rules where rule does not contain at least 1 element of sub-API (target sub-API element)
        for (Map.Entry<String, CombinedResult> resultForSubAPI : results.entrySet()) {
            String subApi = resultForSubAPI.getKey();

            // Rules to remove
            List<FrequentItemset> freqItemsetsSubApi = new ArrayList<>(resultForSubAPI.getValue().getFrequentItemsets());
            freqItemsetsSubApi = freqItemsetsSubApi.stream()
                    .filter(r -> Heuristics.containsTargetSubApiPrefix(r, subApi))
                    .collect(Collectors.toList());

            // Retain only relevant rules
            resultForSubAPI.getValue().getFrequentItemsets().retainAll(freqItemsetsSubApi);
        }

        // Merge all frequent itemsets rules together
        List<FrequentItemset> allFrequentItemsets = results.values()
            .stream()
            .map(CombinedResult::getFrequentItemsets)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        int totalNumOfFreqItemsets = allFrequentItemsets.size();
        System.out.println("... Total number of frequent itemsets -> " + totalNumOfFreqItemsets);

        // Keep only rules with at least 1 MP usage
        long startMpSortTime = System.currentTimeMillis();
        List<FrequentItemset> mpFreqItemsets = allFrequentItemsets.stream().filter(Heuristics::containsTargetAPIPrefix).collect(Collectors.toList());
        long endMpSortTime = System.currentTimeMillis();
        System.out.println("... Filtered frequent itemsets without target APIs -> " + mpFreqItemsets.size());

        // Remove rules that are semantically incorrect
        long startSemanticSortTime = System.currentTimeMillis();
        List<FrequentItemset> clearFreqItemsets = mpFreqItemsets.stream().filter(Heuristics::isSemanticallyOk).collect(Collectors.toList());
        long endSemanticSortTime = System.currentTimeMillis();
        System.out.println("... Remove freq. itemsets that are semantically incorrect -> " + clearFreqItemsets.size());

        // Remove required annotation params because compiler can catch these
        long startRemoveRequiredParams = System.currentTimeMillis();
        // TODO: comes in 0.0.8
        List<FrequentItemset> finalFreqItemsets = Heuristics.filterRequiredParams(clearFreqItemsets);
        long endRemoveRequiredParams = System.currentTimeMillis();
        System.out.println("... Removed required annotation parameters");

        // Remove frequent itemsets of size 2
        // TODO: comes in 0.0.9
        finalFreqItemsets = finalFreqItemsets.stream().filter(fi -> fi.size() >= 2).collect(Collectors.toList());
        System.out.println("... Removed freq. itemsets of size < 2 -> " + finalFreqItemsets.size());

        // Now take one rule per itemset
        List<AssociationRule> uniqueRules = Miner.getOneRulePerFreqItemset(miner.getAllAssociationRules(),  finalFreqItemsets);


        // Check final rules if they have been mined previously and labeled.
        uniqueRules.forEach(r -> {
            String label = RulesDatabase.getLabel(r);
            String version = RulesDatabase.getVersion(r);

            r.setStatus(label);
            r.setVersion(version);
        });

        // Stats on overall mining (all sub-APIs)
        AggregateData data = new AggregateData(
            usagesBySubApi.values().size(), // all input itemsets
            allFrequentItemsets.size(),
            uniqueRules.size(),
            mpFreqItemsets.size(),
            clearFreqItemsets.size(),
            0,
            0,
            finalFreqItemsets.size(),
            0
        );

        // Let's print the stuff
        // String subApiShortName = result.getKey().substring(result.getKey().lastIndexOf('.') + 1).trim();
        ResultPrinter printer = new ResultPrinter(data);

        Date currDay = new java.sql.Date(System.currentTimeMillis());

        for (Map.Entry<String, CombinedResult> result : results.entrySet()) {
            String subApiShortName = result.getKey().substring(result.getKey().lastIndexOf('.') + 1).trim();

            if (Configuration.dumpInputItemsets) {
                List<Set<String>> inputItemsets = usagesBySubApi.get(result.getKey())
                        .stream()
                        .map(Itemset::getItems)
                        .collect(Collectors.toList());

                // Dump all input itemsets for the sub-API
                printer.printInputItemsets(
                    subApiShortName,
                    inputItemsets
                );
            }
        }

        // Dump results for all sub-APIs together
        if (Configuration.dumpFrequentItemsets) {
            // TODO: Was just frequent itemsets.
            printer.printFrequentItemsets("all", finalFreqItemsets);
        }
        if (Configuration.dumpCandidateRules) {
            printer.printCandidateRules("all", uniqueRules);
        }

        // Dump all candidate rules from all sub-APIs at once
        System.out.println("Writing " + uniqueRules.size() + " association rules out of "
            + finalFreqItemsets.size() +
            " post-processed freq. itemsets (in total, mined " +
            totalNumOfFreqItemsets + " freq. itemsets)" +
            "to a JSON file!");
        RulesDatabase.writeToJSON(uniqueRules);

        System.out.println("In total, mined: ");
        System.out.println("\tFrequent itemsets (raw) = " + + totalNumOfFreqItemsets);
        System.out.println("\tFrequent itemsets (final, post-processed) = " + finalFreqItemsets.size());
        System.out.println("\tAssociation rules = " + uniqueRules.size());
    }

    private static HashMap<String, List<Itemset>> divideUsagesByAnnotations(String libSubApiRegex, List<Itemset> allUsages) {
        /*
         * Given list of usages as sets of items, e.g.:
         *     [{ A, B, C }, { A,D }]
         *
         * We want to distribute those into buckets of "@A -> list of usages where @A appears", e.g.:
         *     A : [{ A, B, C }, {A, D}]
         *     B : [{ A, B, C }]
         *     C : [{ A, B, C }]
         *     D : [{ A, D }]
         */
        HashMap<String, List<Itemset>> usagesByAnnotation = new HashMap<>();
        Pattern pattern = Pattern.compile(libSubApiRegex);
        for (Itemset itemset : allUsages) {
            Set<String> items = itemset.getItems();
            Set<String> annotations = new HashSet<>();
            for (String item : items) {
                if (item.contains(Configuration.libPref)) {
                    Matcher matcher = pattern.matcher(item);
                    while (matcher.find()) {
                        annotations.add(matcher.group(0));
                    }
                }
            }

            for (String annotation : annotations) {
                List<Itemset> annotationUsages = usagesByAnnotation.computeIfAbsent(annotation, t -> new ArrayList<>());
                annotationUsages.add(itemset);
            }
        }
        return usagesByAnnotation;
    }

    private static double jaccardSimilarity(Set<String> r1, Set<String> r2) {
        Set<String> s1 = new HashSet<String>(r1);
        Set<String> s2 = new HashSet<String>(r2);

        final int sa = s1.size();
        final int sb = s2.size();
        s1.retainAll(s2);
        final int intersection = s1.size();

        return 1d / (sa + sb - intersection) * intersection;
    }

    private static String getProjectsDirectory() {
        String projectsDir = Configuration.projectsDir;

        if (!new File(projectsDir).exists()) {
            System.out.println("Invalid projects directory!");
            System.exit(1);
        }

        return projectsDir;
    }

    private static void readProjectsFiles(String projectsPath) {
        File project = new File(projectsPath);

        int projectsWithBeans = 0;
        int projectsSkipped = 0;
        int totalNumOfBeans = 0;
        int numOfBeansConsidered = 0;
        int totalProjects = 0;

        // Parse library sources so we get fully-qualified names
        List<File> libs = new ArrayList<>();
        for (String lib : Configuration.librariesPaths) {
            libs.add(new File(lib));
        }

        // Read projects
        builder = new AnnotationUsageGraphBuilder();
        for (File f : Objects.requireNonNull(project.listFiles())) {
            List<File> sourceFiles = new ArrayList<>();
            List<File> configFiles = new ArrayList<>();
            List<File> beans = new ArrayList<>();

            if (f.isDirectory()) {
                ++totalProjects;
                listFilesForFolder(f, sourceFiles, configFiles, beans);
                boolean parsed = false;
                try {
                    parsed = builder.generateUsageGraphsPerProject(f, libs, sourceFiles, configFiles, beans);
                } catch(RuntimeException e) {
                    System.out.println("[runtime exception] Could not parse " + f.getPath());
                }
                // INFO: To get info on how many projs have beans.xml
                if (!parsed) {
                    ++projectsSkipped;
                }
                else {
                    if (beans.size() > 0) {
                        ++projectsWithBeans;
                        numOfBeansConsidered += beans.size();
                    }
                }
            }

            totalNumOfBeans += beans.size();
        }
        System.out.println("There are " + projectsWithBeans + " projects that we used and that are with beans.xml");
        System.out.println("There are " + totalNumOfBeans + " number of beans.xml files");
        System.out.println("There are " + numOfBeansConsidered + " number of beans.xml files that we take into account");
        System.out.println("Could not parse " + builder.getCountUnparseableFiles() + " beans.xml files");
        System.out.println("Finally, I could parse " + builder.getUsagesAsMap().keySet().size() + " out of " + totalProjects + " projects given.");

        System.out.println("*************************\nDid not parse "
                + projectsSkipped + " projects because they are toy projects."
                + "\n*************************"
        );
    }

    private static void listFilesForFolder(final File folder, List<File> sourceFiles, List<File> configFiles, List<File> beans) {
        if (folder.listFiles() == null) {
            return;
        }

        for (final File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isDirectory()) {
                listFilesForFolder(file, sourceFiles, configFiles, beans);
            } else {
                if (file.getName().endsWith(".java")) {
                    sourceFiles.add(file);
                }
                else if (Configuration.libPref.equals("org.eclipse.microprofile")
                    && file.getName().endsWith("microprofile-config.properties")) {
                    configFiles.add(file);
                }
                else if (file.getName().equals("beans.xml") && file.getAbsolutePath().contains("src/main")) {
                    // beans.xml should be inside src/main
                    beans.add(file);
                }
            }
        }
    }
}
