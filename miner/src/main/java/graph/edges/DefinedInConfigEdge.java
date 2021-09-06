package graph.edges;

import graph.nodes.ConfigFileNode;
import graph.nodes.Node;
import graph.nodes.ParamNode;
import visitors.EdgeVisitor;

public class DefinedInConfigEdge extends BaseEdge {

    public DefinedInConfigEdge(Node source, Node target) {
        super(source, target);
        // This edge only goes from a type node to a method or annotation node
        if (!(source instanceof ParamNode) || !(target instanceof ConfigFileNode)) {
            throw new RuntimeException("[DefinedInConfigEdge] Source and target types are incorrect!");
        }
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}