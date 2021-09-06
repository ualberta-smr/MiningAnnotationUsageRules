package parser.rules.springboot;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.FalsePositiveFilters;
import parser.util.Helper;

import java.util.HashSet;

public class EnableWebSecurityClassVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public EnableWebSecurityClassVisitor(String projectName,
                                         String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[EnableWebSecurityClassVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasEnableWebSecurityAnn = false;
        boolean hasConfigAnn = false;

        // Check if @EnableWebSecurity exists
        hasEnableWebSecurityAnn = c.getAnnotations()
            .stream()
            .anyMatch(ae -> Helper.annotationExists(ae,
                "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity"
            ));

        if (!hasEnableWebSecurityAnn) {
            return;
        }

        // Now check for @Configuration
        hasConfigAnn = c.getAnnotations()
            .stream()
            .anyMatch(ae -> Helper.annotationExists(ae,
                "org.springframework.context.annotation.Configuration"
            ));

        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasEnableWebSecurityAnn;
        boolean consequent = hasConfigAnn;

        if (antecedent && !consequent) {

            if (FalsePositiveFilters.classExtendsSpringTypes(c, new HashSet<>())) {
                return;
            }

            Violation.print("@EnableWebSecurity on class --> @Configuration on class", classLocation);
        }
    }
}