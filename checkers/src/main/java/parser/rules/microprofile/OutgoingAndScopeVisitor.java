package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class OutgoingAndScopeVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public OutgoingAndScopeVisitor(String projectName,
                                   String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[OutgoingAndScopeVisitor] projectName, filePath, and importDecls cannot be null!");
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
        boolean hasOutgoing = false;
        for (MethodDeclaration method : c.getMethods()) {
            for (AnnotationExpr annotation : method.getAnnotations()) {
                hasOutgoing = Helper.annotationExists(annotation, "org.eclipse.microprofile.reactive.messaging.Outgoing");

                if (hasOutgoing) {
                    break;
                }
            }
            if (hasOutgoing) {
                break;
            }
        }

        // Check if a class is annotation with @AppicationScoped or @Dependent
        boolean hasApplicationScopedOrDependent = false;
        for (AnnotationExpr annotation : classAnnotations) {
            hasApplicationScopedOrDependent = Helper.annotationExists(annotation, "javax.enterprise.context.ApplicationScoped")
                || Helper.annotationExists(annotation, "javax.enterprise.context.Dependent");
            if (hasApplicationScopedOrDependent) {
                break;
            }
        }

        boolean antecedent = hasOutgoing;
        boolean consequent = hasApplicationScopedOrDependent;

        if (antecedent && !consequent) {
            // Report class location
            Location classLoc = new Location(
                this.projectName, this.filepath, c.getName().getBegin().get().line
            );
            Violation.print("@Outgoing on method --> @ApplicationScoped | @Dependent on class", classLoc);
        }

    }
}