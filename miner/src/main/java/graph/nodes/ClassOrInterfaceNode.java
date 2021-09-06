package graph.nodes;

import visitors.NodeVisitor;

/**
 * A node that represents class declaration
 */
public class ClassOrInterfaceNode extends BaseNode implements ConstructNode {
    private String name;
    public enum Type {
        CLASS,
        INTERFACE
    }
    private Type type;

    public ClassOrInterfaceNode(String name, Type type) {
        if (name == null) {
            throw new NullPointerException("[ClassNode] Cannot have null or empty name");
        }
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isClass() {
        return this.type.equals(Type.CLASS);
    }

    public boolean isInterface() {
        return this.type.equals(Type.INTERFACE);
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
