package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class RegisterRestClientPathVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public RegisterRestClientPathVisitor(String projectName,
                                        String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[RegisterRestClientPathVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasRegisterRestClient = false;
        for (AnnotationExpr annotation : classAnnotations) {
            hasRegisterRestClient = Helper.annotationExists(annotation, "org.eclipse.microprofile.rest.client.inject.RegisterRestClient");
            if (hasRegisterRestClient) {
                break;
            }
        }

        //check @Path on method
        boolean hasPath = false;
        for (MethodDeclaration method : c.getMethods()) {
            for (AnnotationExpr annotation : method.getAnnotations()) {
                hasPath = Helper.annotationExists(annotation, "javax.ws.rs.Path");
                if (hasPath) {
                    break;
                }
            }
            if(hasPath){
                break;
            }
        }

        //check @Path on class
        if(!hasPath){
            for (AnnotationExpr annotation : classAnnotations) {
                hasPath = Helper.annotationExists(annotation, "javax.ws.rs.Path");
                if (hasPath) {
                    break;
                }
            }
        }


        boolean antecedent = hasRegisterRestClient;
        boolean consequent = hasPath;

        if (antecedent && !consequent) {
            // Report class location
            Location classLoc = new Location(
                    this.projectName, this.filepath, c.getName().getBegin().get().line
            );
            Violation.print("@RegisterRestClient on class --> @Path on class or method", classLoc);
        }

    }
}
