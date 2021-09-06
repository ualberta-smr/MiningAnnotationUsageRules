package graph.nodes;

import visitors.NodeVisitor;

import java.io.Serializable;
import java.util.Optional;

public interface Node extends Cloneable, Serializable {
    int getId();

    default boolean isCoreAction() {
        return false;
    }

    default Optional<String> getAPI() {
        return Optional.empty();
    }

    Node clone();

    <R> R apply(NodeVisitor<R> visitor);
}
