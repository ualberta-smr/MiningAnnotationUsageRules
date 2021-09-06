package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class ClaimInjectFieldVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public ClaimInjectFieldVisitor(String projectName,
                                   String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[ClaimInjectFieldVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
    }


    @Override
    public void visit(FieldDeclaration field, Object arg) {
        super.visit(field, arg);


        NodeList<AnnotationExpr> fieldAnnotations = field.getAnnotations();

        boolean hasClaim = false;
        for (AnnotationExpr annotation : fieldAnnotations) {
            hasClaim = Helper.annotationExists(annotation, "org.eclipse.microprofile.jwt.Claim");
            if (hasClaim) {
                break;
            }
        }

        boolean hasInject = false;
        for (AnnotationExpr annotation : fieldAnnotations) {
            hasInject = Helper.annotationExists(annotation, "javax.inject.Inject");
            if (hasInject) {
                break;
            }
        }

        // Create a location corresponding to this node
        // String methodName = method.getNameAsString();
        Location methodLocation = new Location(this.projectName, this.filepath, field.getBegin().get().line);
        boolean antecedent = hasClaim;
        boolean consequent = hasInject;

        if (antecedent && !consequent) {
            Violation.print("@Claim --> @Inject on field", methodLocation);
        }
    }
}