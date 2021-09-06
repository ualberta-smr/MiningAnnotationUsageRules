package visitors;


import graph.edges.*;

public interface EdgeVisitor<R> {
    // Control Flow
    R visit(HasMethodEdge hasMethodEdge);
    R visit(AnnotatedWithEdge annotatedWithEdge);
    R visit(HasParamEdge hasParamEdge);
    R visit(ReturnsEdge returnsEdge);
    R visit(DefinedInConfigEdge definedInConfigEdge);
    R visit(HasFieldEdge hasFieldEdge);
    R visit(BelongsToClassEdge belongsToClassEdge);
    R visit(ExtendsEdge extendsEdge);
    R visit(DeclaredInBeansEdge declaredInBeansEdge);
}
