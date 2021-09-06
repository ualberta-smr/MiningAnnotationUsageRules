package graph.edges;

import graph.nodes.FieldNode;
import graph.nodes.MethodNode;
import graph.nodes.Node;
import graph.nodes.ReturnNode;
import visitors.EdgeVisitor;

public class ReturnsEdge extends BaseEdge {
    private final boolean isForMethod;

    public ReturnsEdge(Node source, Node target) {
        super(source, target);
        if (!(source instanceof MethodNode || source instanceof FieldNode)
            || !(target instanceof ReturnNode)) {
            throw new RuntimeException("[ReturnsEdge] Source and target types are incorrect!");
        }

        isForMethod = source instanceof MethodNode;
    }

    public boolean isForMethod() {
        return this.isForMethod;
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
