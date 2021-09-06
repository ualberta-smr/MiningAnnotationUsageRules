package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class RestClientInjectFieldVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public RestClientInjectFieldVisitor(String projectName,
                                        String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[RestClientInjectFieldVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
    }


    @Override
    public void visit(FieldDeclaration field, Object arg) {
        super.visit(field, arg);


        NodeList<AnnotationExpr> fieldAnnotations = field.getAnnotations();

        boolean hasRestClient = false;
        for (AnnotationExpr annotation : fieldAnnotations) {
            hasRestClient = Helper.annotationExists(annotation, "org.eclipse.microprofile.rest.client.inject.RestClient");
            if (hasRestClient) {
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
        boolean antecedent = hasRestClient;
        boolean consequent = hasInject;

        if (antecedent && !consequent) {
            Violation.print("@RestClient --> @Inject on field", methodLocation);
        }
    }
}