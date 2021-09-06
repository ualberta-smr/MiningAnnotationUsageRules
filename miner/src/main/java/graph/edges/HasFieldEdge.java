package graph.edges;

import graph.nodes.ClassOrInterfaceNode;
import graph.nodes.FieldNode;
import graph.nodes.Node;
import visitors.EdgeVisitor;

public class HasFieldEdge extends BaseEdge {
    public HasFieldEdge(Node source, Node target) {
        super(source, target);
        if (!(source instanceof ClassOrInterfaceNode) || !(target instanceof FieldNode)) {
            throw new RuntimeException("[HasFieldEdge] Source and target types are incorrect!");
        }
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
