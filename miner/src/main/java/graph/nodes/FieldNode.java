package graph.nodes;

import visitors.NodeVisitor;

public class FieldNode extends BaseNode implements ConstructNode {
    private String name;

    public FieldNode(String name) {
        if (name == null || name.equals("")) {
            throw new NullPointerException("[FieldNode] Cannot have null or empty name");
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
