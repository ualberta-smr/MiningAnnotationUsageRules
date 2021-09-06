package graph.edges;

import graph.nodes.*;
import visitors.EdgeVisitor;

public class ExtendsEdge extends BaseEdge {
    private final boolean isClassExtension;

    public ExtendsEdge(Node source, Node target) {
        super(source, target);

        // This edge only goes from a class to class
        if (!(source instanceof ClassOrInterfaceNode && target instanceof ClassOrInterfaceNode)) {
            throw new RuntimeException("[ExtendsEdge] Source and target types are incorrect!");
        }

        // INFO: Assumes only interface OR class (i.e., *must* be one of these). If false, then interface
        isClassExtension = ((ClassOrInterfaceNode) target).isClass();
    }

    public boolean isClassExtension() {
        return isClassExtension;
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
