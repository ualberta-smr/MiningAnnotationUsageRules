package graph.nodes;

import visitors.NodeVisitor;

public class AnnotationNode extends BaseNode {
    private String name;

    public AnnotationNode(String name) {
        if (name == null || name.equals("")) {
            throw new NullPointerException("[AnnotationNode] Name cannot be null");
        }
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
