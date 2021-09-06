package explorer;

import miner.Configuration;
import miner.FrequentItemset;
import org.apache.spark.mllib.fpm.FPGrowth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FrequentItemsetAnalysis {
    public static void run(List<FrequentItemset> freqItemsets) {
        System.out.println("\n************** Analyzing frequent itemsets **************");
        System.out.println("There are " + freqItemsets.size() + " frequent itemsets");

        // Get maximum and minimum support
        long minSupp = (long) Configuration.minSupp + 100000;
        long maxSupp = 0L;
        for (FrequentItemset freqItemset : freqItemsets) {
            minSupp = Math.min(freqItemset.freq(), minSupp);
            maxSupp = Math.max(freqItemset.freq(), maxSupp);
        }
        System.out.println("Minimum support is " + minSupp);
        System.out.println("Maximum support is " + maxSupp);

        // Get maximum and minimum size
        long minSize = 10000000;
        long maxSize = 0L;
        for (FrequentItemset freqItemset : freqItemsets) {
            minSize = Math.min(freqItemset.getItems().size(), minSize);
            maxSize = Math.max(freqItemset.getItems().size(), maxSize);
        }
        System.out.println("Minimum size is " + minSize);
        System.out.println("Maximum size is " + maxSize);

        System.out.println("Printing frequent itemset sizes and their frequency:");
        HashMap<Integer, Integer> frequenciesOfSizes = new HashMap<>();
        for (FrequentItemset freqItemset : freqItemsets) {
            int freqItemsetSize = freqItemset.getItems().size();
            frequenciesOfSizes.put(
                freqItemsetSize,
                frequenciesOfSizes.getOrDefault(freqItemsetSize, 0) + 1
            );
        }
        System.out.println(frequenciesOfSizes);

        List<FrequentItemset> freqItemsetsOriginal = new ArrayList<>(freqItemsets);
        Collections.shuffle(freqItemsetsOriginal);
//        for (int i = 0; i < Math.min(30, freqItemsetsOriginal.size()); ++i) {
//            if (freqItemsetsOriginal.get(i).getItems().size() >= 4) {
//                System.out.println("{");
//                for (String item : freqItemsetsOriginal.get(i).getItems()) {
//                    System.out.println(item);
//                }
//                System.out.println("} with support = " + freqItemsetsOriginal.get(i).freq());
//            }
//        }

        for (int i = 0; i < Math.min(30, freqItemsetsOriginal.size()); ++i) {
            System.out.println("{");
            for (String item : freqItemsetsOriginal.get(i).getItems()) {
                System.out.println("\t" + item);
            }
            System.out.println("} with support = " + freqItemsetsOriginal.get(i).freq());
        }
        System.out.println("********** Finish analyzing frequent itemsets ***********\n");
    }
}
