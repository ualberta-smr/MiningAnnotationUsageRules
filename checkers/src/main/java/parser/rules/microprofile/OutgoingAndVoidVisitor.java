package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class OutgoingAndVoidVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public OutgoingAndVoidVisitor(String projectName,
                                  String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[OutgoingAndVoidVisitor] projectName, filePath, and importDecls cannot be null!");
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

        // Check if a method is annotated with @Outgoing or @Incoming
        boolean hasOutgoingOrIncoming = false;
        for (MethodDeclaration method : c.getMethods()) {
            for (AnnotationExpr annotation : method.getAnnotations()) {
                hasOutgoingOrIncoming = Helper.annotationExists(annotation, "org.eclipse.microprofile.reactive.messaging.Outgoing")
                    || Helper.annotationExists(annotation, "org.eclipse.microprofile.reactive.messaging.Incoming");

                if (hasOutgoingOrIncoming) {
                    break;
                }
            }

            boolean antecedent = hasOutgoingOrIncoming;
            boolean consequent = Helper.methodReturnTypeExists(method, "void");

            if (antecedent && !consequent) {
                // Report class location
                Location classLoc = new Location(
                    this.projectName, this.filepath, c.getName().getBegin().get().line
                );
                Violation.print("@Outgoing|@Incoming on method --> method returns void", classLoc);
            }
        }
    }
}