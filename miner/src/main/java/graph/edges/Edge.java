package graph.edges;

import graph.nodes.Node;
import visitors.EdgeVisitor;

import java.io.Serializable;

public interface Edge extends Cloneable, Serializable {

    Node getSource();

    Node getTarget();

    default boolean isDirect() {
        return true;
    }

    Edge clone();

    Edge clone(Node newSourceNode, Node newTargetNode);

    <R> R apply(EdgeVisitor<R> visitor);
}
