package graph.edges;

import graph.nodes.*;
import visitors.EdgeVisitor;

public class HasParamEdge extends BaseEdge {
    public HasParamEdge(Node source, Node target) {
        super(source, target);
        // This edge only goes from a type node to a method or annotation node
        if (!(source instanceof MethodNode || source instanceof AnnotationNode || source instanceof ConstructorNode)
                || !(target instanceof ParamNode)) {
            throw new RuntimeException("[HasParamEdge] Source and target types are incorrect!");
        }
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
