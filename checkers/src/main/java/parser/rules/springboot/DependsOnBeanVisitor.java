package parser.rules.springboot;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class DependsOnBeanVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public DependsOnBeanVisitor(String projectName,
                                String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[DependsOnBeanVisitor] projectName, filePath, and importDecls cannot be null!");
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
            boolean hasDependsOnAnn = false;

            for (AnnotationExpr methodAnn : method.getAnnotations()) {
                if (Helper.annotationExists(methodAnn, "org.springframework.context.annotation.Bean")) {
                    hasBeanAnn = true;
                }
                if (Helper.annotationExists(methodAnn, "org.springframework.context.annotation.DependsOn")) {
                    hasDependsOnAnn = true;
                }
            }

            if (hasDependsOnAnn && !hasBeanAnn) {
                Location methodLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
                Violation.print("@DependsOn on method --> @Bean on method", methodLocation);
            }
        }
    }
}