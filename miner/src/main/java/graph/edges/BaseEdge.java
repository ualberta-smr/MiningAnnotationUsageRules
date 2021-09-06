package graph.edges;

import graph.nodes.Node;
import visitors.AUGLabelProvider;
import visitors.BaseAUGLabelProvider;

public abstract class BaseEdge implements Edge {
    private Node source;
    private Node target;
//    private APIUsageGraph graph;

    protected BaseEdge(Node source, Node target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Try fetching this information from the corresponding graph, because this would allow us to get rid of these
     * fields and to safely reuse edges between multiple graphs, instead of cloning them.
     */
    @Deprecated
    @Override
    public Node getSource() {
        return source;
    }

    /**
     * Try fetching this information from the corresponding graph, because this would allow us to get rid of these
     * fields and to safely reuse edges between multiple graphs, instead of cloning them.
     */
    @Deprecated
    @Override
    public Node getTarget() {
        return target;
    }

    @Override
    public Edge clone() {
        try {
            return (Edge) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("All edges must be cloneable.", e);
        }
    }

    @Override
    public Edge clone(Node newSourceNode, Node newTargetNode) {
        BaseEdge clone = (BaseEdge) clone();
        clone.source = newSourceNode;
        clone.target = newTargetNode;
        return clone;
    }

    @Override
    public String toString() {
        AUGLabelProvider labelProvider = new BaseAUGLabelProvider();
        return labelProvider.getLabel(getSource())
                + " --(" + labelProvider.getLabel(this) + ")--> "
                + labelProvider.getLabel(getTarget());
    }
}
