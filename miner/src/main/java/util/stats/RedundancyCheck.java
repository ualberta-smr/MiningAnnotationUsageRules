package util.stats;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//import miner.AssociationRule;
import miner.Configuration;
import util.labeler.RulesDatabase;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class RedundancyCheck {
    // Just to get hashcodes of labeled rules. Used to semi-automatically convert rules from Excel to JSON.
    public static void main(String[] args) {
        String jsonFileName = "rules_v0.0.5.json";

        Gson gson = new Gson();
        List<RulesDatabase.LabeledRule> rules = new ArrayList<>();
        try (Reader reader = new FileReader(jsonFileName)) {
            Type rulesListType = new TypeToken<ArrayList<RulesDatabase.LabeledRule>>(){}.getType();

            rules = gson.fromJson(reader, rulesListType);
            if (rules == null) {
                System.out.println("[RedundancyCheck] JSON file is empty!");
                rules = new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        List<RulesDatabase.LabeledRule> correctOrPartCorrectRules = rules.stream()
            .filter(lr -> lr.label().contains("correct"))
            .collect(Collectors.toList());

        Set<Integer> uniqueCorrectOrPartCorrectRules = new HashSet<>();
        Map<Integer, Integer> idToRedundancyCount = new HashMap<>();

        for (RulesDatabase.LabeledRule lr : correctOrPartCorrectRules) {
            if (lr.sameAs() == 0 && !lr.label().equals("not a rule")) {
                System.out.println("Something is wrong. Check " + lr.sameAs());
                System.exit(1);
            }
            else if (lr.sameAs() == -1) {
                uniqueCorrectOrPartCorrectRules.add(lr.id());
            }
            else {
                idToRedundancyCount.put(lr.sameAs(), idToRedundancyCount.getOrDefault(lr.sameAs(), 0) + 1);
            }
        }

        System.out.println("Number of unique rules = " + uniqueCorrectOrPartCorrectRules.size());
        System.out.println("Number of redundant rules (excluding originals) = " + idToRedundancyCount.values().stream().reduce(0, Integer::sum));
        System.out.println("Number of correct or part. correct rules = " + correctOrPartCorrectRules.size());

        for (RulesDatabase.LabeledRule lr : correctOrPartCorrectRules) {
            if (uniqueCorrectOrPartCorrectRules.contains(lr.id())) {
                System.out.println("{");
                System.out.println("\tIf:");
                for (String ant : lr.antecedent()) {
                    System.out.println("\t" + ant + ",");
                }
                System.out.println("\tThen:");
                for (String con : lr.consequent()) {
                    System.out.println("\t" + con);
                }
                System.out.println("}");
            }
        }
    }

    public boolean areValid(List<RulesDatabase.LabeledRule> rules) {
        // Check first
        for (RulesDatabase.LabeledRule lr : rules) {
            if (lr.label().equals("unknown")) {
                System.out.println("[INCOMPLETE] The following rule ID is `unknown`: " + lr.id());
                return false;
            }
            if (lr.label().contains("correct") && lr.sameAs() == 0) {
                System.out.println("[INCOMPLETE] Did not label rule with ID: " + lr.id());
                return false;
            }
        }
        return true;
    }
}

