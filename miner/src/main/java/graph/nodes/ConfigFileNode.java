package graph.nodes;

import visitors.NodeVisitor;

// Represents a configuration file name
public class ConfigFileNode extends BaseNode {
    String name;

    public ConfigFileNode(String name) {
        if (name == null) {
            throw new NullPointerException("[ConfigFileNode] Name is null or empty");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
