package miner;

import graph.Location;
import graph.edges.Edge;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Itemset implements Serializable {
    Location location;
    Set<String> items;
    boolean frozen = false;

    public Itemset(Location l) {
//        if (l == null) {
//            System.out.println("[Itemset()] Location object cannot be null");
//            System.exit(1);
//        }
        this.location = l;
        this.items = new HashSet<>();
    }

    public Itemset(Set<String> items, Location l) {
        if (items == null || items.isEmpty()) {
            System.out.println("[Itemset()] Items cannot be null or empty!");
            System.exit(1);
        }
//        if (l == null) {
//            System.out.println("[Itemset()] Location object cannot be null");
//            System.exit(1);
//        }
        this.location = l;
        this.items = items;
        this.frozen = true;
    }

    public Set<String> getItems() {
        return items;
    }

    public void add(String e) {
        if (frozen) {
            System.err.println("CANNOT ADD TO FROZEN SET");
            return;
        }

        items.add(e);
    }

    public void add(Edge e) {
        if (frozen) {
            System.err.println("CANNOT ADD TO FROZEN SET");
            return;
        }

        items.add(e.toString());
    }


    public boolean isEmpty() {
        return items.size() == 0;
    }

    public int size() {
        return items.size();
    }

    public Location getLocation() {
        return location;
    }

    public boolean contains(String item) {
        return this.items.contains(item);
    }

    public boolean matchesAnyItem(String query) {
        return this.items.stream().anyMatch(item -> item.contains(query));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        Itemset that = (Itemset) obj;
        return Objects.equals(items, that.getItems());
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

}
