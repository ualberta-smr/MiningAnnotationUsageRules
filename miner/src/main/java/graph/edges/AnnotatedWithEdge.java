package graph.edges;

import graph.nodes.*;
import visitors.EdgeVisitor;

public class AnnotatedWithEdge extends BaseEdge {
    public AnnotatedWithEdge(Node source, Node target) {
        super(source, target);

        // This edge only goes from a construct to an annotation
        if (!(source instanceof ConstructNode || source instanceof ParamNode || source instanceof TypeDeclNode) || !(target instanceof AnnotationNode)) {
            throw new RuntimeException("[AnnotatedWithEdge] Source and target types are incorrect!");
        }
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
