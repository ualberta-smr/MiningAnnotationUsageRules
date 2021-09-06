package graph.edges;

import graph.nodes.BeansFileNode;
import graph.nodes.ClassOrInterfaceNode;
import graph.nodes.Node;
import visitors.EdgeVisitor;

public class DeclaredInBeansEdge extends BaseEdge {
    public DeclaredInBeansEdge(Node source, Node target) {
        super(source, target);
        if (!(source instanceof ClassOrInterfaceNode) || !(target instanceof BeansFileNode)) {
            throw new RuntimeException("[DeclaredInBeansEdge] Source and target types are incorrect!");
        }
    }

    @Override
    public <R> R apply(EdgeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
