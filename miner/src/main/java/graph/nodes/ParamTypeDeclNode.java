package graph.nodes;

import visitors.NodeVisitor;

public class ParamTypeDeclNode extends BaseNode implements TypeDeclNode {
    private String name;

    public ParamTypeDeclNode(String name) {
        if (name == null) {
            throw new NullPointerException("[ParamTypeDeclNode] Name is null or empty");
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
