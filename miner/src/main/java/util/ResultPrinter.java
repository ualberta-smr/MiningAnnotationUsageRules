package util;

//import miner.AssociationRule;
import miner.AssociationRule;
import miner.Configuration;
import miner.FrequentItemset;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class ResultPrinter {

    private final Date currDay;
    private final AggregateData data;

    public ResultPrinter(AggregateData data) {
         this.currDay = new java.sql.Date(System.currentTimeMillis());
         this.data = data;
    }

    private void printRuleProcessingResults() {
        System.out.println("There are " + data.numOfMPRules() + " rules that involve >=1 MP element.");
        System.out.println("There are " + data.numOfClearRules() + " rules that are semantically correct.");
        System.out.println("There are " + data.numOfMaximalRules() + " maximal rules.");
        System.out.println("There are " + data.numOfCombinedRules() + " rules that are combined.");
        System.out.println("There are " + data.numOfFinalRules() + " rules that are final.");
        System.out.println("There are " + data.numOfClusters() + " clusters.");
        System.out.println();
    }

    public void printHeader() {
        System.out.println("---- Configuration ----");
        System.out.println("Dataset dir    : " + Configuration.projectsDir);
        System.out.println("Target APIs    : " + Arrays.toString(Configuration.apiLibPrefixes));
        System.out.println("Min support    : " + Configuration.minSupp);
        System.out.println("Min confidence : " + Configuration.minConf);
        System.out.println();

        // Printing stats here
        System.out.println("Finished mining "
                + data.numOfFreqItemsets() + " frequent itemsets and "
                + data.numOfCandRules() + " raw rules from "
                + data.numOfInputItemsets() + " projects.");

        this.printRuleProcessingResults();
    }

    public void printFrequentItemsets(
        String filename,
        List<FrequentItemset> freqItemsets
    ) throws FileNotFoundException {
        PrintStream originalOut = System.out;
        PrintStream freqItemsetsDump = new PrintStream("results/runs/" + currDay + "_" + filename + "_freqItemsets.txt");

        try {
            System.setOut(freqItemsetsDump);

            // Print header first with all aggregate stats
            this.printHeader();

            System.out.print("Frequent itemsets:");
            for (FrequentItemset fi : freqItemsets) {
                fi.print();
            }
            System.out.println("**** finish dump ****");
        }
        finally {
            System.setOut(originalOut);
        }
    }

    public void printCandidateRules(
        String filename,
        List<AssociationRule> finalRules
    ) throws FileNotFoundException {
        PrintStream originalOut = System.out;
        PrintStream candRulesDump = new PrintStream("results/runs/" + currDay + "_" + filename + "_candidateRules.txt");

        try {
            System.setOut(candRulesDump);

            // Print header first with all aggregate stats
            this.printHeader();

            System.out.print("Association rules (maximal and combined):");
            for (AssociationRule r : finalRules) {
                r.print();
            }
            System.out.println("**** finish dump ****");
        }
        finally {
            System.setOut(originalOut);
        }
    }

    public void printInputItemsets(
        String filename,
        List<Set<String>> inputItemsets
    ) throws FileNotFoundException {
        PrintStream originalOut = System.out;
        PrintStream inputItemsetsDump = new PrintStream("results/runs/" + currDay + "_" + filename + "_inputItemsets.txt");

        try {
            System.setOut(inputItemsetsDump);

            // Print header first with all aggregate stats
            this.printHeader();

            System.out.print("Input itemsets:");
            for (Set<String> itemset : inputItemsets) {
                System.out.println("{");
                for (String item : itemset) {
                    System.out.println("\t" + item);
                }
                System.out.println("}");
            }
            System.out.println("**** finish dump ****");
        }
        finally {
            System.setOut(originalOut);
        }

    }
}
