package miner;

import java.util.*;

public class AssociationRule {
    private Set<String> antecedent;
    private String consequent;
    private Double confidence;
    private Set<String> packagesUsed;
    private String fromVersion = Configuration.version;
    private String status = "unknown"; // "correct|partially correct|best practice|not a rule|unknown"

    public AssociationRule() {
        packagesUsed = new HashSet<>();
    }

    private void addPackageUsed(String api) {
        packagesUsed.add(api);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setVersion(String version) {
        this.fromVersion = version;
    }

    public Set<String> getPackagesUsed() {
        return packagesUsed;
    }

    public String status() {
        return status;
    }

    public String version() {
        return fromVersion;
    }

    public Set<String> antecedent() {
        return antecedent;
    }

    public String consequent() {
        return consequent;
    }

    public Set<String> consequentAsSingletonSet() {
        return Collections.singleton(this.consequent);
    }

    public Double confidence() {
        return confidence;
    }

    public void setAntecedent(Set<String> antecedent) {
        this.antecedent = antecedent;
    }

    public void setConsequent(String consequent) {
        // Sanity check
        for (String ant : this.antecedent) {
            if (consequent.equals(ant)) {
                System.err.println("[setConsequent] Consequent element is already inside antecedent!!!");
                System.exit(1);
            }
        }
        this.consequent = consequent;
    }

    public void moveToConsequent(String antecedent){
        this.antecedent().add(this.consequent());
        this.antecedent().remove(antecedent);
        this.setConsequent(antecedent);
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

//    @Deprecated
//    public void addConsequent(String cons, Double conf) {
//        if (this.consequent.containsKey(cons)) {
//            System.err.println("[addConsequent] Cannot override existing key in consequent!!!");
//            System.exit(1);
//        }
//        if (this.antecedent.contains(cons)) {
//            System.err.println("[addConsequent] Consequent element is already inside antecedent!!!");
//            System.exit(1);
//        }
//        this.consequent.put(cons, conf);
//    }

    public boolean isInAntecedent(String item) {
        return antecedent.contains(item);
    }

    public boolean isConsequent(String item) {
        return consequent.equals(item);
    }

    public boolean insideAntecedentItem(String key) {
        return antecedent.stream().anyMatch(item -> item.contains(key));
    }

    public boolean insideConsequentItem(String key) {
        return consequent.contains(key);
    }

    public boolean isInsideRule(String key) {
        return insideAntecedentItem(key) || insideConsequentItem(key);
    }

    public Set<String> getAllItems() {
        Set<String> allItems = new HashSet<>();
        allItems.addAll(antecedent);
        allItems.add(consequent);
        return allItems;
    }

    public void print() {
        System.out.println("--- start rule ---");
        System.out.println("Status: " + this.status());
        System.out.println("\tAntecedent:");
        for (String item : this.antecedent) {
            System.out.println("\t\t" + item);
        }
        System.out.println("\tConsequent:");
        System.out.println("\t\t" + this.consequent + ", conf = " + this.confidence);
        System.out.println("--- end rule ---");
    }

    @Override
    public String toString() {
        // INFO: Used to store in JSON files. Does not display `status` on purpose!
        String ruleAsString = "--- start rule ---\n";
        ruleAsString += "\tAntecedent:\n";
        for (String item : this.antecedent) {
            ruleAsString += "\t\t" + item + "\n";
        }
        ruleAsString += "\tConsequent:\n";
        ruleAsString += "\t\t" + this.consequent + ", conf = " + this.confidence + "\n";
        ruleAsString += "--- end rule ---\n";

        return ruleAsString;
    }

    public static AssociationRule toAssociationRule(List<String> antecedent, List<String> consequent, double conf) {
        // Set consequent as map from consequent (one) to confidence
        if (consequent.size() != 1) {
            System.err.println("[LOGICAL ERROR] Consequent supposed to contain exactly one item!");
            System.exit(1);
        }

        // Create and set
        AssociationRule rule = new AssociationRule();
        rule.setAntecedent(new HashSet<>(antecedent));
        rule.setConsequent(consequent.get(0));
        rule.setConfidence(conf);

        // @Deprecated
//        // Add packages from antecedent
//        for (String s : antecedent) {
//            if (s.contains("Annotation")) {
//                if (!s.contains(".")) {
//                    continue;
//                }
//                if (s.lastIndexOf(".") < s.indexOf("Annotation_") + 11) {
//                    System.out.println(s);
//                    continue;
//                }
//                String api = s.substring(s.indexOf("Annotation_") + 11, s.lastIndexOf("."));
//                rule.addPackageUsed(api);
//            }
//        }
//
//        // Add packages from consequent
//        for (String s : consequent) {
//            if (s.contains("Annotation")) {
//                if (!s.contains(".")) {
//                    continue;
//                }
//                if (s.lastIndexOf(".") < s.indexOf("Annotation_") + 11) {
//                    System.out.println(s);
//                    continue;
//                }
//                String api = s.substring(s.indexOf("Annotation_") + 11, s.lastIndexOf("."));
//                rule.addPackageUsed(api);
//            }
//        }

        // Return
        return rule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssociationRule that = (AssociationRule) o;
        return antecedent.equals(that.antecedent)
            && consequent.equals(that.consequent)
            && Math.abs(confidence - that.confidence) < 0.000001;
    }

    @Override
    public int hashCode() {
        Set<String> LHS = antecedent();
        String RHS = consequent(); // ignore confidence, we are interested in items only
        Double CONF = confidence();

        // hashCode(hashCode(LHS) + hashCode(RHS) + hashCode(CONF))
        return Objects.hash(LHS, RHS, CONF);
    }
}
