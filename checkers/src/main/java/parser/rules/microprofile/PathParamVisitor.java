package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class PathParamVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public PathParamVisitor(String projectName,
                            String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[ConstructVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration c, Object arg) {
        if (c.isInterface() || c.isAbstract()) {
            // continue if interface
            return;
        }

        super.visit(c, arg);

        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        // Check if a class is annotation with @Path
        boolean hasPathAnnOnClassOrMethod = false;
        for (AnnotationExpr annotation : classAnnotations) {
            hasPathAnnOnClassOrMethod = Helper.annotationExists(annotation, "javax.ws.rs.Path");
            if (hasPathAnnOnClassOrMethod) {
                break;
            }
        }

        // Check a method being annotated with @Path
        // at the same time, check if any of the method's parameters is annotated with @PathParam
        for (MethodDeclaration method : c.getMethods()) {
            if (!hasPathAnnOnClassOrMethod) {
                for (AnnotationExpr annotation : method.getAnnotations()) {
                    hasPathAnnOnClassOrMethod = Helper.annotationExists(annotation, "javax.ws.rs.Path");
                    if (hasPathAnnOnClassOrMethod) {
                        break;
                    }
                }
            }

            // Check if method param is annotated with @PathParam
            for (Parameter param : method.getParameters()) {
                for (AnnotationExpr annotation : param.getAnnotations()) {
                    boolean hasPathParam = Helper.annotationExists(annotation, "javax.ws.rs.PathParam");

                    boolean antecedent = hasPathParam;
                    boolean consequent = hasPathAnnOnClassOrMethod;

                    if (antecedent && !consequent) {
                        // Create a location corresponding to this node
                        // String methodName = method.getNameAsString();
                        Location methodLoc = new Location(
                            this.projectName, this.filepath, param.getName().getBegin().get().line
                        );

                        Violation.print("@PathParam on method param --> @Path on class or method", methodLoc);
                        break;
                    }
                }
            }
        }
    }
}