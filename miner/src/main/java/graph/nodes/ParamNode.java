package graph.nodes;

import visitors.NodeVisitor;

public class ParamNode extends BaseNode implements TypeNode {
    private String name;

    public ParamNode(String name) {
        if (name == null) {
            throw new NullPointerException("[ParamNode] Name is null or empty");
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
