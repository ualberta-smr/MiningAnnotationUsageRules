package util.labeler;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//import miner.AssociationRule;
import miner.Configuration;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class RulesFromExcelReader {
    // Just to get hashcodes of labeled rules. Used to semi-automatically convert rules from Excel to JSON.
    public static void main(String[] args) {
        String jsonFileName = "rules_v0.0.0.json";

        Gson gson = new Gson();
        List<RulesDatabase.LabeledRule> rules = new ArrayList<>();
        try (Reader reader = new FileReader(jsonFileName)) {
            Type rulesListType = new TypeToken<ArrayList<RulesDatabase.LabeledRule>>(){}.getType();

            rules = gson.fromJson(reader, rulesListType);
            if (rules == null) {
                System.out.println("[static StatsCalculator] JSON file is empty!");
                rules = new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convert rules to JSON
//        List<RulesDatabase.LabeledRule> rulesWithCorrectCodes = new ArrayList<>();
//        for (RulesDatabase.LabeledRule lr : rules) {
//            AssociationRule ar = AssociationRule.toAssociationRule(new ArrayList<>(lr.antecedent()), new ArrayList<>(lr.consequent()), 0.5);
//            ar.setStatus(lr.label());
//            System.out.println("Hashcode is " + ar.hashCode() + " for id " + lr.id());
//            rulesWithCorrectCodes.add(RulesDatabase.LabeledRule.toLabeledRule(ar));
//        }

        try (Writer writer = new FileWriter(jsonFileName)) {
            gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

//            gson.toJson(rulesWithCorrectCodes, writer);
        } catch (IOException e) {
            System.err.println("Could not write rules to the JSON file");
        }

    }
}

