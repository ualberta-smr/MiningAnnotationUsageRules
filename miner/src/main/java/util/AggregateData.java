package util;

// More like struct
public class AggregateData {
    private final int numOfInputItemsets;
    private final int numOfFreqItemsets;
    private final int numOfCandRules;
    private final int numOfMPRules;
    private final int numOfClearRules;
    private final int numOfMaximalRules;
    private final int numOfCombinedRules;
    private final int numOfFinalRules;
    private final int numOfClusters;

    public AggregateData(
        int numOfInputItemsets,
        int numOfFreqItemsets,
        int numOfCandRules,
        int numOfMPRules,
        int numOfClearRules,
        int numOfMaximalRules,
        int numOfCombinedRules,
        int numOfFinalRules,
        int numOfClusters
    ) {
        this.numOfInputItemsets = numOfInputItemsets;
        this.numOfFreqItemsets = numOfFreqItemsets;
        this.numOfCandRules = numOfCandRules;
        this.numOfMPRules = numOfMPRules;
        this.numOfClearRules = numOfClearRules;
        this.numOfMaximalRules = numOfMaximalRules;
        this.numOfCombinedRules = numOfCombinedRules;
        this.numOfFinalRules = numOfFinalRules;
        this.numOfClusters = numOfClusters;
    }

    public int numOfInputItemsets() {
        return numOfInputItemsets;
    }

    public int numOfFreqItemsets() {
        return numOfFreqItemsets;
    }

    public int numOfCandRules() {
        return numOfCandRules;
    }

    public int numOfMPRules() {
        return numOfMPRules;
    }

    public int numOfClearRules() {
        return numOfClearRules;
    }

    public int numOfMaximalRules() {
        return numOfMaximalRules;
    }

    public int numOfCombinedRules() {
        return numOfCombinedRules;
    }

    public int numOfFinalRules() {
        return numOfFinalRules;
    }

    public int numOfClusters() {
        return numOfClusters;
    }
}