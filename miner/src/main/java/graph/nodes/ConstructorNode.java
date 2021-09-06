package graph.nodes;

import visitors.NodeVisitor;

public class ConstructorNode extends BaseNode implements ConstructNode {
    private String name;
    private String parameters;

    public ConstructorNode(String name, String parameters) {
        if (name == null || parameters == null || name.equals("")) {
            throw new NullPointerException("[ConstructorNode] Name or return type is null or empty");
        }
        this.name = name;
        this.parameters = parameters;
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
