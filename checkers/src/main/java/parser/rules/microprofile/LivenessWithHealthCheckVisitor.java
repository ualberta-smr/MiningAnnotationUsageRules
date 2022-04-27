package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class LivenessWithHealthCheckVisitor extends VoidVisitorAdapter<Object> {

    private String projectName;
    private String filepath;

    public LivenessWithHealthCheckVisitor(String projectName,
                                                      String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[LivenessAndHealthCheck] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration c, Object arg) {
        super.visit(c, arg);

        if (c.isInterface() || c.isAbstract()) {
            // do not process interfaces
            return;
        }

        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        // Check if a class is annotation with @AppicationScoped or @Dependent
        boolean hasLiveness = false;
        for (AnnotationExpr annotation : classAnnotations) {
            hasLiveness = Helper.annotationExists(annotation, "org.eclipse.microprofile.health.Liveness");
            if (hasLiveness) {
                break;
            }
        }

        // Check class extensions
        NodeList<ClassOrInterfaceType> impls = c.getImplementedTypes();

        boolean hasHealthCheckImpl = false;
        for (ClassOrInterfaceType impl : impls) {
            hasHealthCheckImpl = Helper.extensionExists(impl, "org.eclipse.microprofile.health.HealthCheck");
            if (hasHealthCheckImpl) {
                break;
            }
        }

        boolean antecedent = hasLiveness;
        boolean consequent = hasHealthCheckImpl;

        if (antecedent && !consequent) {
            // Report class location
            Location classLoc = new Location(
                    this.projectName, this.filepath, c.getName().getBegin().get().line
            );
            Violation.print("@Liveness on class --> Class implements HealthCheck", classLoc);
        }

    }
}
