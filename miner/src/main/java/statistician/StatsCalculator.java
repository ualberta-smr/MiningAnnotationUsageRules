package statistician;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
//import miner.AssociationRule;
import miner.Configuration;
import util.labeler.RulesDatabase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class StatsCalculator {
    private static Map<String, List<RulesDatabase.LabeledRule>> prevRules;
    private static List<RulesDatabase.LabeledRule> latestRules;

    // Static initialization for the rules
    static {
        prevRules = new HashMap<>();

        Gson gson = new Gson();

        File dir = new File(".");
        File[] foundFiles = dir.listFiles((dir1, name) -> name.startsWith("rules_v"));

        assert foundFiles != null;
        for (File file : foundFiles) {
            String fileName = file.getName();

            // skip rules_v and extract version
            String version = fileName.substring(7, fileName.lastIndexOf("."));

            // read rules
            List<RulesDatabase.LabeledRule> rulesForAVersion = new ArrayList<>();

            try (Reader reader = new FileReader(fileName)) {
                Type rulesListType = new TypeToken<ArrayList<RulesDatabase.LabeledRule>>(){}.getType();

                rulesForAVersion = gson.fromJson(reader, rulesListType);
                if (rulesForAVersion == null) {
                    System.out.println("[static StatsCalculator] JSON file is empty!");
                    rulesForAVersion = new ArrayList<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // put them into map (rules from each specific version of the tool)
            prevRules.put(version, rulesForAVersion);
        }

        // Important: exclude current (latest) version from others to avoid over-counting stats
        String latestVersion = Configuration.version.substring(1);
        if (!prevRules.containsKey(latestVersion)) {
            System.err.println("The latest version does not exist. Did you already mine rules?");
            System.exit(1);
        }
        else if (prevRules.get(latestVersion).stream().anyMatch(lr -> lr.label().contains("unknown"))) {
            // If there is any unlabeled rule, then also exit with an error. Should label things first!
            System.err.println("The latest version does not have all rules labeled. Did you label everything?");
            System.exit(2);
        }
        else {
            // If version is there and all rules for that (current/latest) version are labeled, then we are good.
            // Also, if current version is V_k, then we should only consider versions V_{k-1}, V_{k-2}, ..., V_0
            latestRules = new ArrayList<>(prevRules.get(latestVersion));
            prevRules.remove(latestVersion);
        }
    }

    public static void main(String[] args) {
        System.out.println("Running the statistician to calculate stats ...");
        List<RulesDatabase.LabeledRule> rulesFromLatestVersion = latestRules;
        getStats(rulesFromLatestVersion);
    }

    private static List<RulesDatabase.LabeledRule> getBenchmarkRules() {
        List<RulesDatabase.LabeledRule> rules = new ArrayList<>();

        try (Reader reader = new FileReader("rules_benchmark.json")) {
            Type rulesListType = new TypeToken<ArrayList<RulesDatabase.LabeledRule>>(){}.getType();

            rules = new Gson().fromJson(reader, rulesListType);
            if (rules == null) {
                System.out.println("[getBenchmarkRules] Benchmark rules not provided!");
                System.exit(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rules;
    }

    // Run before dumping current rules to avoid incorrect results
    public static void getStats(List<RulesDatabase.LabeledRule> postprocessedRules) {
        System.out.println("------------------------------------------------------------------");
        // <version, Map<Correctness, Rule>>
        Map<String, Map<String, RulesDatabase.LabeledRule>> stats = new HashMap<>();

        // maps misses/hits of rules' hashcodes in each version (version --> list<hashcodes>)
        Map<String, Map<String, Integer>> missCountByLabelPerVersion = new HashMap<>();
        Map<String, Set<Integer>> misses = new HashMap<>();
        Map<String, Set<Integer>> hits = new HashMap<>();

        List<Integer> currVersionRulesIds = postprocessedRules.stream()
                .map(RulesDatabase.LabeledRule::id)
                .collect(Collectors.toList());

        // Calculate hits/misses based on if we mined or did not mine previously mined rules
        for (Map.Entry<String, List<RulesDatabase.LabeledRule>> rulesInVersion : prevRules.entrySet()) {
            String thatVersion = rulesInVersion.getKey();

            for (RulesDatabase.LabeledRule labeledRule : rulesInVersion.getValue()) {
                int labeledRuleId = labeledRule.id();

                if (currVersionRulesIds.contains(labeledRuleId)) {
                    // We managed to hit labeled (previously mined) rule id
                    Set<Integer> hitIds = hits.computeIfAbsent(thatVersion, t -> new HashSet<>());
                    hitIds.add(labeledRuleId);
                } else {
                    // ... or we did not mine a previously mined rule
                    Set<Integer> missedIds = misses.computeIfAbsent(thatVersion, t -> new HashSet<>());
                    missedIds.add(labeledRuleId);

                    // Distribution of misses by label (e.g., how many missed rules are part.correct)
                    Map<String, Integer> missCountByLabel = missCountByLabelPerVersion.computeIfAbsent(thatVersion, t -> new HashMap<>());
                    missCountByLabel.put(
                        labeledRule.label(),missCountByLabel.getOrDefault(labeledRule.label(), 0) + 1
                    );
                }
            }
        }

        // New rules that are not mined in specific version
        Set<Integer> newRules = new HashSet<>();

        // Check if there are new rules
        Set<Integer> allRulesPreviouslyMined = prevRules.values()
                .stream()
                .flatMap(List::stream)
                .map(RulesDatabase.LabeledRule::id)
                .collect(Collectors.toSet());

        for (RulesDatabase.LabeledRule r : postprocessedRules) {
            int currRuleId = r.id();

            if (!allRulesPreviouslyMined.contains(currRuleId)) {
                newRules.add(currRuleId);
            }
        }

        // New rules relative to each previous version
        Map<String, Set<Integer>> newRulesForVersion = new HashMap<>();
        for (RulesDatabase.LabeledRule r : postprocessedRules) {
            Integer currRuleId = r.id();

            for (Map.Entry<String, List<RulesDatabase.LabeledRule>> thatVersionRules : prevRules.entrySet()) {
                String version = thatVersionRules.getKey();

                // Get all ids
                Set<Integer> labeledRulesIds = thatVersionRules.getValue()
                        .stream()
                        .map(RulesDatabase.LabeledRule::id)
                        .collect(Collectors.toSet());

                // Check if it is not there
                if (!labeledRulesIds.contains(currRuleId)) {
                    Set<Integer> ids = newRulesForVersion.computeIfAbsent(version, t -> new HashSet<>());
                    ids.add(currRuleId);
                }
            }
        }

        Integer totalHits = hits.values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet())
                .size();
        Integer totalMisses = misses.values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet())
                .size();
        Integer totalNewRules = newRules.size();

        assert totalHits + totalNewRules == postprocessedRules.size();

        // TODO: print to a file
        System.out.println("For current version of " + Configuration.version);
        System.out.println("Total rules mined: " + postprocessedRules.size());
        System.out.println("# of hits      : " + totalHits);
        System.out.println("# of misses    : " + totalMisses);
        System.out.println("# of new rules : " + totalNewRules);
        System.out.println("For each tool version");
        for (String prevVersion : prevRules.keySet()) {
            Set<Integer> prevVersionHits = hits.getOrDefault(prevVersion, new HashSet<>());
            Set<Integer> prevVersionMisses = misses.getOrDefault(prevVersion, new HashSet<>());
            Set<Integer> newRulesRelative = newRulesForVersion.getOrDefault(prevVersion, new HashSet<>());
            Map<String, Integer> missCountByLabel = missCountByLabelPerVersion.getOrDefault(prevVersion, new HashMap<>());

            // Print hit/miss/new rule count for each previous version
            System.out.println("\tFor tool version : " + prevVersion);
            System.out.println("\t# of hits      : " + prevVersionHits.size());
            System.out.println("\t# of misses    : " + prevVersionMisses.size());
            System.out.println("\t# of new rules : " + newRulesRelative.size());

            assert prevVersionMisses.size() == missCountByLabel.values().size();
            System.out.println("\tDistribution of hits per label: " + prevVersionMisses.size());
            for (String label : missCountByLabel.keySet()) {
                System.out.println("\t\t# of `" + label + "` misses: " + missCountByLabel.get(label));
            }
            System.out.println("\t------------------");
        }

        // Get count of rules based on labels
        Map<String, Integer> countByLabel = new HashMap<>();
        for (RulesDatabase.LabeledRule r : postprocessedRules) {
            countByLabel.put(r.label(), countByLabel.getOrDefault(r.label(), 0) + 1);
        }
        System.out.println("Label frequency:");
        countByLabel.forEach((k, v) -> System.out.println("\t" + k + ": " + v));

        // TODO: Recall and precision
        // Get recall and precision
        Set<Integer> currVersionCorrectPartCorrect = new HashSet<>();
        for (RulesDatabase.LabeledRule r : postprocessedRules) {
            if (r.label().contains("correct")) {
                currVersionCorrectPartCorrect.add(r.hashCode());
            }
        }

        Set<Integer> allUniqueRulesCorrectPartCorrect = new HashSet<>();
        for (Map.Entry<String, List<RulesDatabase.LabeledRule>> thatVersionRules : prevRules.entrySet()) {
            List<RulesDatabase.LabeledRule> labeledRules = thatVersionRules.getValue();

            labeledRules.forEach(lr -> {
                if (lr.label().contains("correct")) {
                    allUniqueRulesCorrectPartCorrect.add(lr.id());
                }
            });
        }
        int numOfCorrectAndPartCorrect = countByLabel.getOrDefault("correct", 0) + countByLabel.get("part. correct");

        // If not, then some rules are unlabeled
        assert countByLabel.get("unknown") == 0;

        List<RulesDatabase.LabeledRule> benchmark = getBenchmarkRules();

        double recall = getRecall(benchmark, postprocessedRules);
        double precision = numOfCorrectAndPartCorrect / (1.0 * postprocessedRules.size());

        // TODO: Adjust to new way of calculating recall
        System.out.println("Recall    : ???/" + benchmark.size() + " = " + "???");
        System.out.println("Precision : " + numOfCorrectAndPartCorrect + "/"
            + postprocessedRules.size() + " = " + precision);
//        System.out.println("Recall    : " + currVersionCorrectPartCorrect.size() + "/"
//                + allUniqueRulesCorrectPartCorrect.size() + " = " + recall);
//        System.out.println("Precision : " + numOfCorrectAndPartCorrect + "/"
//                + postprocessedRules.size() + " = " + precision);

        System.out.println("------------------------------------------------------------------");
    }

    public static double getRecall(
        List<RulesDatabase.LabeledRule> benchmarkRules,
        List<RulesDatabase.LabeledRule> currRules
    ) {
        int rulesFoundCorrectly = 0; // unique
        int rulesFoundPartially = 0; // unique

        for (RulesDatabase.LabeledRule benchmarkRule : benchmarkRules) {
            Set<String> benchmarkRuleAllItems = new HashSet<>();
            benchmarkRuleAllItems.addAll(benchmarkRule.antecedent());
            benchmarkRuleAllItems.addAll(benchmarkRule.consequent());

            for (RulesDatabase.LabeledRule currRule : currRules) {
                Set<String> currRuleAllItems = new HashSet<>();
                currRuleAllItems.addAll(currRule.antecedent());
                currRuleAllItems.addAll(currRule.consequent());

                if (benchmarkRule.antecedent().equals(currRule.antecedent())
                    && benchmarkRule.consequent().equals(currRule.consequent())) {
                    // Completely equal --> correct. Hashcode comparison would be faster
                    rulesFoundCorrectly++;
                    break; // once we found this rule from the benchmark, continue with the next one
                }
                else if (currRuleAllItems.containsAll(benchmarkRuleAllItems)) {
                    // Partially equal --> partially correct.
                    rulesFoundPartially++;
                    break; // once we found this rule from the benchmark, continue with the next one
                }
            }
        }

        return (rulesFoundPartially + rulesFoundCorrectly) / (double) benchmarkRules.size();
    }

}
