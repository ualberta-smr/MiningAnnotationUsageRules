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

public class JaxRsAnnotationsVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public JaxRsAnnotationsVisitor(String projectName,
                                   String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[JaxRsAnnotationsVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration c, Object arg) {
        if (c.isInterface() || c.isAbstract()) {
            // do not process interfaces
            return;
        }

        super.visit(c, arg);

        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        String[] jaxRsAnns = new String[] {
            "javax.ws.rs.PUT",
            "javax.ws.rs.GET",
            "javax.ws.rs.POST",
            "javax.ws.rs.DELETE",
            "javax.ws.rs.HEAD",
            "javax.ws.rs.OPTIONS",
            "javax.ws.rs.PATCH",
            "javax.ws.rs.BeanParam",
//            "javax.ws.rs.Consumes",
//            "javax.ws.rs.Produces",
            "javax.ws.rs.HeaderParam",
            "javax.ws.rs.MatrixParam",
            "javax.ws.rs.QueryParam",
            "javax.ws.rs.FormParam",
            "javax.ws.rs.PathParam"
        };

        boolean hasPathAnnOnClassOrMethod = false;
        boolean hasAnyOtherJaxRsAnnotation = false;

        // Determine if antecedent is true
        // Check class annotations
        for (AnnotationExpr annotation : classAnnotations) {
            hasAnyOtherJaxRsAnnotation = Helper.anyOfAnnotationsExist(annotation, jaxRsAnns);
            if (hasAnyOtherJaxRsAnnotation) {
                break;
            }
        }

        // Check method annotations and method parameter annotations
        for (MethodDeclaration method : c.getMethods()) {
            // Check if method param is annotated with @PathParam
            for (Parameter param : method.getParameters()) {
                for (AnnotationExpr annotation : param.getAnnotations()) {
                    hasAnyOtherJaxRsAnnotation = Helper.anyOfAnnotationsExist(annotation, jaxRsAnns);

                    if (hasAnyOtherJaxRsAnnotation) {
                        break;
                    }
                }
                if (hasAnyOtherJaxRsAnnotation) {
                    break;
                }
            }
            if (hasAnyOtherJaxRsAnnotation) {
                break;
            }
            for (AnnotationExpr annotation : method.getAnnotations()) {
                hasAnyOtherJaxRsAnnotation = Helper.anyOfAnnotationsExist(annotation, jaxRsAnns);
                if (hasAnyOtherJaxRsAnnotation) {
                    break;
                }
            }
            if (hasAnyOtherJaxRsAnnotation) {
                break;
            }
        }

        // Now determine if consequent is true or false
        // Check if a class is annotation with @Path
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
            if (hasPathAnnOnClassOrMethod) {
                break;
            }
        }

        Location methodLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasAnyOtherJaxRsAnnotation;
        boolean consequent = hasPathAnnOnClassOrMethod;

        // Attempt to deep scan before reporting violation
        // 1. Scan class hierarchy
        if (!consequent && Helper.annExistsinClassHierarchy(c, "javax.ws.rs.Path")) {
            consequent = true;
        }

        // 2. If custom annotation, scan the annotation declaration
        // TODO


        // 3. Ideally, find all usages of the class and check if @Path is there

        if (antecedent && !consequent) {
            Violation.print("@javax.ws.rs.* --> @Path on class or method", methodLocation);
        }
    }
}