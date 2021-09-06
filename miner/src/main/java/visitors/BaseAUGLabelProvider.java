package visitors;


import graph.edges.*;
import graph.nodes.*;

public class BaseAUGLabelProvider implements AUGLabelProvider {

    @Override
    public String visit(HasMethodEdge hasMethodEdge) {
        return "hasMethod";
    }

    @Override
    public String visit(AnnotatedWithEdge annotatedWithEdge) {
        return "annotatedWith";
    }

    @Override
    public String visit(HasParamEdge hasParamEdge) {
        return "hasParam";
    }

    @Override
    public String visit(ReturnsEdge returnsEdge) {
        return (returnsEdge.isForMethod()) ? "hasReturnType" : "hasType";
    }

    @Override
    public String visit(DefinedInConfigEdge definedInConfigEdge) {
        return "definedIn";
    }

    @Override
    public String visit(HasFieldEdge hasFieldEdge) {
        return "hasField";
    }

    @Override
    public String visit(BelongsToClassEdge belongsToClassEdge) {
        return "belongsToClass";
    }

    @Override
    public String visit(ExtendsEdge extendsEdge) {
        if (extendsEdge.isClassExtension()) {
            return "extends";
        } else {
            return "implements";
        }
    }

    @Override
    public String visit(DeclaredInBeansEdge declaredInBeansEdge) {
        return "declaredInBeans";
    }

    @Override
    public String visit(ClassOrInterfaceNode classOrInterfaceNode) {
        // TODO: Class extensions?
        String name = classOrInterfaceNode.getName();
        if (name.equals("")) {
            if (classOrInterfaceNode.isClass()) {
                return "Class";
            } else {
                return "Interface";
            }
        }
        else {
            if (classOrInterfaceNode.isClass()) {
                return "Class_" + classOrInterfaceNode.getName();
            } else {
                return "Interface_" + classOrInterfaceNode.getName();
            }
        }
    }

    @Override
    public String visit(MethodNode methodNode) {
        return "Method";// + methodNode.getReturnType();
    }

    @Override
    public String visit(AnnotationNode annotationNode) {
        return "Annotation_" + annotationNode.getName();
    }

    @Override
    public String visit(ReturnNode returnNode) {
        return returnNode.getName();
    }

    @Override
    public String visit(ParamNode paramNode) {
        if (paramNode.getName().equals("")) {
            return "Param_void";
        }
        else return "Param_" + paramNode.getName();
    }

    @Override
    public String visit(ConfigFileNode configFileNode) {
        return "ConfigFile_" + configFileNode.getName();
    }

    @Override
    public String visit(FieldNode fieldNode) {
        return "Field";
    }

    @Override
    public String visit(ConstructorNode constructorNode) {
        return "Constructor";
    }

    @Override
    public String visit(BeansFileNode beansFileNode) {
        return beansFileNode.getName();
    }

    @Override
    public String visit(FieldTypeDeclNode fieldTypeDeclNode) {
        return "FieldTypeDecl";
    }

    @Override
    public String visit(ParamTypeDeclNode paramTypeDeclNode) {
        return "ParamTypeDecl";
    }
}
