package graph.nodes;

import visitors.NodeVisitor;

/**
 * A node that represents method declaration
 */
public class MethodNode extends BaseNode implements ConstructNode {
    private String name;
    private String returnType;
    private String parameters;

    public MethodNode(String name, String returnType, String parameters) {
        if (name == null || returnType == null || parameters == null ||
            name.equals("") || returnType.equals("")) {
            throw new NullPointerException("[ClassNode] Name or return type is null or empty");
        }
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getParameters() {
        return parameters;
    }

    public String getSignatureWithoutName() {
        String params = parameters;
        if (params.equals("")) {
            params = "void";
        }
        return returnType + " Method" + "(" + params + ")";
    }

    @Override
    public <R> R apply(NodeVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
