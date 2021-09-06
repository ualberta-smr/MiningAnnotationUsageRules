package util.labeler;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//import miner.AssociationRule;
import miner.Configuration;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class LabelAfterMining {

    private static Set<Set<String>> elementsOfPartCorrectUsages = new HashSet<>();

    static {
        Set<String> u1 = new HashSet<>();
        u1.add("Method --(annotatedWith)--> Annotation_org.eclipse.microprofile.openapi.annotations.Operation");
        u1.add("Method --(hasType)--> javax.ws.rs.core.Response");

        Set<String> u2 = new HashSet<>();
        u2.add("Method --(annotatedWith)--> Annotation_org.eclipse.microprofile.openapi.annotations.Operation");
        u2.add("Class --(annotatedWith)--> Annotation_javax.ws.rs.Path");

        Set<String> u3 = new HashSet<>();
        u3.add("Field --(hasType)--> org.eclipse.microprofile.jwt.JsonWebToken");
        u3.add("Field --(annotatedWith)--> Annotation_javax.inject.Inject");

        Set<String> u4 = new HashSet<>();
        u4.add("Field --(annotatedWith)--> Annotation_org.eclipse.microprofile.jwt.Claim");
        u4.add("Field --(annotatedWith)--> Annotation_javax.inject.Inject");

        Set<String> u5 = new HashSet<>();
        u5.add("Method --(annotatedWith)--> Annotation_javax.ws.rs.Path");
        u5.add("Method --(annotatedWith)--> Annotation_javax.ws.rs.Consumes");

        Set<String> u6 = new HashSet<>();
        u6.add("Field --(annotatedWith)--> Annotation_org.eclipse.microprofile.config.inject.ConfigProperty");
        u6.add("Field --(annotatedWith)--> Annotation_javax.inject.Inject");

        Set<String> u7 = new HashSet<>();
        u7.add("Class --(annotatedWith)--> Annotation_org.eclipse.microprofile.graphql.GraphQLApi");
        u7.add("Method --(annotatedWith)--> Annotation_org.eclipse.microprofile.graphql.Query");

        Set<String> u8 = new HashSet<>();
        u8.add("Class --(annotatedWith)--> Annotation_org.eclipse.microprofile.faulttolerance.Asynchronous");
        u8.add("Method --(hasType)--> java.util.concurrent.CompletionStage");

        Set<String> u9 = new HashSet<>();
        u9.add("Method --(annotatedWith)--> Annotation_org.eclipse.microprofile.faulttolerance.Fallback");
        u9.add("Annotation_org.eclipse.microprofile.faulttolerance.Fallback --(hasParam)--> Param_fallbackMethod:java.lang.String");

        Set<String> u10 = new HashSet<>();
        u10.add("Method --(annotatedWith)--> Annotation_org.eclipse.microprofile.reactive.messaging.Incoming");
        u10.add("Class --(annotatedWith)--> Annotation_javax.enterprise.context.ApplicationScoped");

        Set<String> u11 = new HashSet<>();
        u11.add("Method --(annotatedWith)--> Annotation_org.eclipse.microprofile.reactive.messaging.Outgoing");
        u11.add("Class --(annotatedWith)--> Annotation_javax.enterprise.context.ApplicationScoped");

        Set<String> u12 = new HashSet<>();
        u12.add("Class --(annotatedWith)--> Annotation_org.eclipse.microprofile.rest.client.inject.RegisterRestClient");
        u12.add("Class --(annotatedWith)--> Annotation_javax.ws.rs.Path");


        Set<String> u13 = new HashSet<>();
        u13.add("Class --(annotatedWith)--> Annotation_org.eclipse.microprofile.health.Liveness");
        u13.add("Class --(extends/implements)--> Class_org.eclipse.microprofile.health.HealthCheck");


        Set<String> u14 = new HashSet<>();
        u14.add("Class --(annotatedWith)--> Annotation_org.eclipse.microprofile.health.Readiness");
        u14.add("Class --(extends/implements)--> Class_org.eclipse.microprofile.health.HealthCheck");


        Set<String> u15 = new HashSet<>();
        u15.add("Class --(annotatedWith)--> Annotation_org.eclipse.microprofile.health.Health");
        u15.add("Class --(extends/implements)--> Class_org.eclipse.microprofile.health.HealthCheck");


        Set<String> u16 = new HashSet<>();
        u16.add("Annotation_org.eclipse.microprofile.faulttolerance.Fallback --(hasParam)--> Param_value:java.lang.Class");
        u16.add("Method --(annotatedWith)--> Annotation_org.eclipse.microprofile.faulttolerance.Fallback");

        Set<String> u17 = new HashSet<>();
        u17.add("Field --(annotatedWith)--> Annotation_org.eclipse.microprofile.rest.client.inject.RestClient");
        u17.add("Field --(annotatedWith)--> Annotation_javax.inject.Inject");

        Set<String> u18 = new HashSet<>();
        u18.add("Class --(annotatedWith)--> Annotation_org.eclipse.microprofile.openapi.annotations.tags.Tag");
        u18.add("Class --(annotatedWith)--> Annotation_javax.ws.rs.Path");

        elementsOfPartCorrectUsages.add(u1);
        elementsOfPartCorrectUsages.add(u2);
        elementsOfPartCorrectUsages.add(u3);
        elementsOfPartCorrectUsages.add(u4);
        elementsOfPartCorrectUsages.add(u5);
        elementsOfPartCorrectUsages.add(u6);
        elementsOfPartCorrectUsages.add(u7);
        elementsOfPartCorrectUsages.add(u8);
        elementsOfPartCorrectUsages.add(u9);
        elementsOfPartCorrectUsages.add(u10);
        elementsOfPartCorrectUsages.add(u11);
        elementsOfPartCorrectUsages.add(u12);
        elementsOfPartCorrectUsages.add(u13);
        elementsOfPartCorrectUsages.add(u14);
        elementsOfPartCorrectUsages.add(u15);
        elementsOfPartCorrectUsages.add(u16);
        elementsOfPartCorrectUsages.add(u17);
        elementsOfPartCorrectUsages.add(u18);
    }

    // Just to get hashcodes of labeled rules. Used to semi-automatically convert rules from Excel to JSON.
    public static void main(String[] args) {
        String jsonFileName = "rules_" + Configuration.version + ".json";

        Gson gson = new Gson();
        List<RulesDatabase.LabeledRule> rules = new ArrayList<>();
        try (Reader reader = new FileReader(jsonFileName)) {
            Type rulesListType = new TypeToken<ArrayList<RulesDatabase.LabeledRule>>(){}.getType();

            rules = gson.fromJson(reader, rulesListType);
            if (rules == null) {
                System.out.println("[LabelAfterMining] JSON file is empty!");
                rules = new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convert rules to JSON
        for (RulesDatabase.LabeledRule lr : rules) {
            if (!lr.label().equals("unknown")) {
                // already labeled
                continue;
            }

            // TODO: Make label private after finishing!
            Set<String> items = new HashSet<>();
            items.addAll(lr.antecedent());
            items.addAll(lr.consequent());

            // To check correct rules
            if (items.size() <= 3) {
                System.out.println(lr.id());
            }

            // If there is at least X and Y of a rule, then that's partially correct (given correct rule is {X --> Y})
            boolean matchesAnyRule = elementsOfPartCorrectUsages.stream().anyMatch(items::containsAll);

            if (items.size() > 2 && matchesAnyRule) {
                lr.label = "part. correct";
            }
        }

        try (Writer writer = new FileWriter(jsonFileName)) {
            gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

            gson.toJson(rules, writer);
        } catch (IOException e) {
            System.err.println("Could not write rules to the JSON file");
        }

    }
}

