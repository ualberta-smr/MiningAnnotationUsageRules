package visitors;


import graph.edges.Edge;
import graph.nodes.Node;

public interface AUGLabelProvider extends AUGElementVisitor<String> {
    default String getLabel(Node node) {
        return node.apply(this);
    }

    default String getLabel(Edge edge) {
        return edge.apply(this);
    }
}
