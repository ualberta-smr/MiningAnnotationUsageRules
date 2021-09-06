package parser.rules.springboot;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class PrimaryBeanVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public PrimaryBeanVisitor(String projectName,
                              String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[PrimaryBeanVisitor] projectName, filePath, and importDecls cannot be null!");
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

        // Check if there is @Bean on any method
        for (MethodDeclaration method : c.getMethods()) {
            boolean hasBeanAnn = false;
            boolean hasPrimaryAnn = false;

            for (AnnotationExpr methodAnn : method.getAnnotations()) {
                if (Helper.annotationExists(methodAnn, "org.springframework.context.annotation.Bean")) {
                    hasBeanAnn = true;
                }
                if (Helper.annotationExists(methodAnn, "org.springframework.context.annotation.Primary")) {
                    hasPrimaryAnn = true;
                }
            }

            if (hasPrimaryAnn && !hasBeanAnn) {
                Location methodLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
                Violation.print("@Primary on method --> @Bean on method", methodLocation);
            }
        }
    }
}