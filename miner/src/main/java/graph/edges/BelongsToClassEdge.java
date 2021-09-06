package graph.edges;

import graph.nodes.ClassOrInterfaceNode;
import graph.nodes.FieldNode;
import graph.nodes.MethodNode;
import graph.nodes.Node;
import visitors.EdgeVisitor;

public class BelongsToClassEdge extends BaseEdge {
    public BelongsToClassEdge(Node source, Node target) {
        super(source, target);
        if (!(source instanceof FieldNode || source instanceof MethodNode) || !(target instanceof ClassOrInterfaceNode)) {
            throw new RuntimeException("[BelongsToClass] Source and target types are incorrect!");
        }
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
