package miner;

import java.util.List;

public class CombinedResult {
    List<FrequentItemset> frequentItemsets;
    long inputSize;

    public CombinedResult(List<FrequentItemset> frequentItemsets, long inputSize) {
        this.frequentItemsets = frequentItemsets;
        this.inputSize = inputSize;
    }

    public List<FrequentItemset> getFrequentItemsets() {
        return frequentItemsets;
    }

//    public List<AssociationRule> getAssociationRules() {
//        return associationRules;
//    }

    public long getInputSize() {
        return inputSize;
    }
}
