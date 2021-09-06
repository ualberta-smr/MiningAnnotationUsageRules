package graph.edges;

import graph.nodes.ClassOrInterfaceNode;
import graph.nodes.MethodNode;
import graph.nodes.Node;
import visitors.EdgeVisitor;

public class HasMethodEdge extends BaseEdge {
    public HasMethodEdge(Node source, Node target) {
        super(source, target);
        // This edge only goes from a class to a method
        if (!(source instanceof ClassOrInterfaceNode) || !(target instanceof MethodNode)) {
            throw new RuntimeException("[HasMethodEdge] Source and target types are incorrect!");
        }
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
