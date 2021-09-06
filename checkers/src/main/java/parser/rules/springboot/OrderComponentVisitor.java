package parser.rules.springboot;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.FalsePositiveFilters;
import parser.util.Helper;

import java.util.Set;

public class OrderComponentVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;
    final private Set<String> methodReturnTypes;

    public OrderComponentVisitor(String projectName,
                                 String filePath,
                                 Set<String> methodReturnTypes) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[OrderComponentVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
        this.methodReturnTypes = methodReturnTypes;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration c, Object arg) {
        if (c.isInterface() || c.isAbstract()) {
            // do not process interfaces
            return;
        }

        super.visit(c, arg);

        boolean hasOrderAnn = false;
        boolean hasComponentAnn = false;

        // Check if @EnableWebSecurity exists
        hasOrderAnn = c.getAnnotations()
            .stream()
            .anyMatch(ae -> Helper.annotationExists(ae,
                "org.springframework.core.annotation.Order"
            ));

        if (!hasOrderAnn) {
            return;
        }

        // Now check for @Configuration
        hasComponentAnn = Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.stereotype.Component");

        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasOrderAnn;
        boolean consequent = hasComponentAnn;

        if (antecedent && !consequent) {

            // CHECK IF FALSE POSITIVE: If class extends or implements Spring interface, then probably not a false positive
            if (FalsePositiveFilters.classExtendsSpringTypes(c, methodReturnTypes)) {
                return;
            }
            if (FalsePositiveFilters.classUsedAsReturnType(c, methodReturnTypes)) {
                return;
            }

            Violation.print("@Order on class --> @Component on class", classLocation);
        }
    }
}