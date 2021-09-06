package parser.rules.springboot;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class EnableTransactMgmtClassVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public EnableTransactMgmtClassVisitor(String projectName,
                                          String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[EnableTransactMgmtClassVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasEnableTransactMgmtAnn = false;
        boolean hasConfigurationAnn = false;

        // Check if @EnableTransactionManagement exists
        hasEnableTransactMgmtAnn = c.getAnnotations()
            .stream()
            .anyMatch(ae -> Helper.annotationExists(ae,
                "org.springframework.transaction.annotation.EnableTransactionManagement"
            ));

        if (!hasEnableTransactMgmtAnn) {
            return;
        }

        // Now check for @Configuration
        hasConfigurationAnn = Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.context.annotation.Configuration");

        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasEnableTransactMgmtAnn;
        boolean consequent = hasConfigurationAnn;

        if (antecedent && !consequent) {
            Violation.print("@EnableTransactionManagement on class --> @Configuration on class", classLocation);
        }
    }
}