package miner;

import graph.Location;
import org.apache.spark.sql.Row;
import scala.collection.JavaConversions;
import scala.collection.mutable.WrappedArray;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FrequentItemset extends Itemset {
    private long frequency = -1;
    private String status = "unknown"; // "correct|partially correct|best practice|not a usage|unknown"
    private String fromVersion = Configuration.version;

    public FrequentItemset(Set<String> items, Location l, long frequency) {
        super(items, l);
        this.frequency = frequency;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String status() {
        return this.status;
    }

    public void setVersion(String version) {
        this.fromVersion = version;
    }

    public String version() {
        return this.fromVersion;
    }

    public long freq() {
        return frequency;
    }

    public static FrequentItemset toFrequentItemset(List<String> items, long frequency) {
        // FIXME: How to add location?
        return new FrequentItemset(new HashSet<>(items), null, frequency);
    }

    public void print() {
        System.out.println("--- start freq. itemset ---");
        System.out.println("{");
        for (String item : items) {
            System.out.println("\t" + item);
        }
        System.out.println("} with support " + this.frequency);
        System.out.println("--- end freq. itemset ---");
    }

}
