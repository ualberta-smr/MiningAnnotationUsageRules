package graph.nodes;

import visitors.NodeVisitor;

public class FieldTypeDeclNode extends BaseNode implements TypeDeclNode {
    private String name;

    public FieldTypeDeclNode(String name) {
        if (name == null) {
            throw new NullPointerException("[FieldTypeDeclNode] Name is null or empty");
        }
        this.name = name;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String getName() {
        return name;
    }
}
