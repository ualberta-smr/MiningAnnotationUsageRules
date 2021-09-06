package parser.rules.springboot;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class EnableJmsConfigVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public EnableJmsConfigVisitor(String projectName,
                                  String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[EnableJmsConfigVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasEnableJmsAnn = false;
        boolean hasConfigurationAnn = false;

        // Check if @EnableJms exists
        hasEnableJmsAnn = c.getAnnotations()
            .stream()
            .anyMatch(ae -> Helper.annotationExists(ae,
                "org.springframework.jms.annotation.EnableJms"
            ));

        if (!hasEnableJmsAnn) {
            return;
        }

        // Now check for @Configuration
        hasConfigurationAnn = Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.context.annotation.Configuration");

        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasEnableJmsAnn;
        boolean consequent = hasConfigurationAnn;

        if (antecedent && !consequent) {
            Violation.print("@EnableJms on class --> @Configuration on class", classLocation);
        }
    }
}