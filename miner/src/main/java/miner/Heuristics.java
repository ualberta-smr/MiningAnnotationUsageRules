package miner;

import sideprocessing.Annotation;
import sideprocessing.AnnotationDeclarationReader;

import java.util.*;
import java.util.stream.Collectors;

public class Heuristics {
    public static boolean containsTargetAPIPrefix(FrequentItemset frequentItemset) {
        for (String libPrefix : Configuration.apiLibPrefixes) {
            if (frequentItemset.matchesAnyItem(libPrefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSemanticallyOk(FrequentItemset frequentItemset) {
        Set<String> items = frequentItemset.getItems();

        for (String item : items) {
            // An annotation parameter should be declared before it's used.
            if (item.contains("--(hasParam)-->") && item.startsWith("Annotation")) {
                String requiredAnnotation = item.substring(0, item.indexOf(" "));
                boolean annotationDeclared = items.stream()
                        .anyMatch(rel -> rel.endsWith("--(annotatedWith)--> " + requiredAnnotation));

                if (!annotationDeclared) return false;
            }
        }

        return true;
    }

    public static boolean isSemanticallyOk(AssociationRule rule) {
        Set<String> antecedent = rule.antecedent();

        for (String antItem : antecedent) {
            // An annotation parameter should be declared before it's used.
            if (antItem.contains("hasParam") && antItem.startsWith("Annotation")) {
                String requiredAnnotation = antItem.substring(0, antItem.indexOf(" "));
                boolean annotationDeclared = antecedent.stream()
                    .anyMatch(rel -> rel.endsWith("--(annotatedWith)--> " + requiredAnnotation));

                if (!annotationDeclared) return false;
            }
            // A config value parameter should be declared before it's used.
            // TODO: comes in v0.0.7
            if (antItem.contains("definedIn")) {
                // We don't have to check for Ann_Param --definedIn-->, i.e., whether the parameter is defined here
                // because we for sure know that it's going to be there (remember, itemset per field).
                // Just don't let this definedIn relationship be in the consequent
                return false;
            }
            if (antItem.contains("declaredInBeans")) {
                // We don't have to check for Ann_Param --declaredInBeans-->, i.e., whether the parameter is defined here
                // because we for sure know that it's going to be there (remember, itemset per field).
                // Just don't let this definedIn relationship be in the consequent
                return false;
            }
        }

        return true;
    }

    public static void rearrangeConsequent(AssociationRule rule){
        boolean type = false;
        //All other relationships take precedence in consequent over annotatedWith
        String consequent = rule.consequent();
        boolean correctConsequent = consequent.contains("hasType")|| consequent.contains("hasReturnType") || consequent.contains("extends")
                || consequent.contains("implements") || consequent.contains("definedIn") || consequent.contains("declaredInBeans");

        if(!correctConsequent){
            for(String item: rule.getAllItems()){
                if((item.contains("hasType")|| item.contains("hasReturnType") || item.contains("extends")
                        || item.contains("implements") || item.contains("definedIn") || item.contains("declaredInBeans"))){
                    rule.moveToConsequent(item);
                    type = true;
                    break;
                }
            }
        }


        //class annotatedWith takes precedence over the other (field, method, parameter) annotatedWith
        if(!type && !consequent.contains("Class")){
            boolean cls = false, other = false;
            for(String item: rule.getAllItems()){
                if(item.contains("Class")){
                    cls = true;
                }else if(item.contains("Field") || item.contains("Method") || item.contains("Parameter")){
                    other = true;
                    break;
                }
            }

            if(cls && other){
                for(String item: rule.getAllItems()){
                    if(item.contains("Class")){
                        rule.moveToConsequent(item);
                        break;
                    }
                }
            }
        }
    }


    public static boolean containsTargetSubApiPrefix(FrequentItemset frequentItemset, String subApi) {
        return frequentItemset.getItems().stream().anyMatch(a -> a.contains(subApi));
    }


    /**
     * First, this removes required params because these can be handled by the compiler.
     * Second, when removing required params (i.e., an item), it checks if ant/cons is empty.
     *
     * @return itemsets without required params, potentially removes redundant frequent itemsets
     */
    public static List<FrequentItemset> filterRequiredParams(List<FrequentItemset> frequentItemsets) {
        List<FrequentItemset> filteredFrequentItemsets = new ArrayList<>();

        // Get declarations of all target library annotations from json
        AnnotationDeclarationReader annDeclReader = new AnnotationDeclarationReader();
        Map<String, Annotation> annDict = annDeclReader.getMap();

        for (FrequentItemset frequentItemset : frequentItemsets) {
            Set<String> allItems = new HashSet<>(frequentItemset.getItems());

            List<String> paramsRequired = new ArrayList<>();

            // Check antecedent and consequent
            for (String item : allItems) {
                if (item.contains("Annotation") && item.contains("hasParam")) {
                    // Regex to get the annotation name: (?<=Annotation_).*?(?=\s)
                    String annName = item.substring(item.indexOf("Annotation_") + 11, item.indexOf(" "));
                    String paramName = item.substring(item.indexOf("Param_") + 6).split(":")[0];

                    assert annName.contains(".");

                    if (annDict.containsKey(annName)) {
                        Annotation ann = annDict.get(annName);

                        // Check if `paramName` is in the list of required params
                        boolean isRequiredParam = ann.getRequiredParams()
                            .stream()
                            .anyMatch(p -> p.name().equals(paramName));

                        if (isRequiredParam) {
                            paramsRequired.add(item);
                        }
                    }
                }
            }

            // Now remove relationships where parameter is required
            paramsRequired.forEach(frequentItemset.getItems()::remove);

            // If a frequentItemset is non-empty after processing, then add it back
            if (frequentItemset.getItems().size() > 0) {
                filteredFrequentItemsets.add(frequentItemset);
            }
        }

        return filteredFrequentItemsets;
    }

}
