package graph.nodes;

import visitors.NodeVisitor;

public class BeansFileNode extends BaseNode {
    String name;

    public BeansFileNode(String name) {
        if (name == null) {
            throw new NullPointerException("[BeansFileNode] Name is null or empty");
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
