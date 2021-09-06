package parser.rules.springboot;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class EnableRetryConfigVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public EnableRetryConfigVisitor(String projectName,
                                    String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[EnableRetryConfigVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasEnableRetryAnn = false;
        boolean hasConfigorComponentAnn = false;

        // Check if @EnableRetry exists
        hasEnableRetryAnn = c.getAnnotations()
            .stream()
            .anyMatch(ae -> Helper.annotationExists(ae,
                "org.springframework.retry.annotation.EnableRetry"
            ));

        if (!hasEnableRetryAnn) {
            return;
        }

        // Now check for @Configuration
        hasConfigorComponentAnn = Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.context.annotation.Configuration")
            || Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.stereotype.Component");

        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasEnableRetryAnn;
        boolean consequent = hasConfigorComponentAnn;

        if (antecedent && !consequent) {
            Violation.print("@EnableRetry on class --> @Configuration || @Component on class", classLocation);
        }
    }
}