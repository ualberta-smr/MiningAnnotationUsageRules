package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class JsonWebTokenFieldVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public JsonWebTokenFieldVisitor(String projectName,
                                    String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[ConstructVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
    }


    @Override
    public void visit(ClassOrInterfaceDeclaration c, Object arg) {
        super.visit(c, arg);

        final String fullFieldTypeName = "org.eclipse.microprofile.jwt.JsonWebToken";

        for (FieldDeclaration field : c.getFields()) {
            NodeList<AnnotationExpr> fieldAnnotations = field.getAnnotations();

            boolean hasInject = false;
            for (AnnotationExpr annotation : fieldAnnotations) {
                hasInject = Helper.annotationExists(annotation, "javax.inject.Inject");
                if (hasInject) {
                    break;
                }
            }
            // Create a location corresponding to this node
            // String methodName = method.getNameAsString();
            Location fieldLocation = new Location(this.projectName, this.filepath, field.getBegin().get().line);

            // Get return typeNode of the method
            boolean antecedent = Helper.fieldTypeExists(field, fullFieldTypeName);
            boolean consequent = hasInject;

            // If no @Inject is there, check if field is injected through a constructor
            if (!consequent && Helper.fieldExistsAsConstructorParam(c, fullFieldTypeName)) {
                // if injected through constructor, then we can ignore @Inject
                consequent = true;
            }

            // Check if field is injected through a setter
            // TODO


            if (antecedent && !consequent) {
                Violation.print("Field of type JsonWebToken --> @Inject on field", fieldLocation);
            }
        }

    }
}