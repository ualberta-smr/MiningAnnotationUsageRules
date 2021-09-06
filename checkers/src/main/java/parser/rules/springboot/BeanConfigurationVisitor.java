package parser.rules.springboot;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.FalsePositiveFilters;
import parser.util.Helper;

import java.util.HashSet;
import java.util.Set;

public class BeanConfigurationVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;
    private Set<String> allImportClasses;

    public BeanConfigurationVisitor(String projectName,
                                    String filePath,
                                    Set<String> allImportClasses) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[BeanConfigurationVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
        this.allImportClasses = allImportClasses;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration c, Object arg) {
        if (c.isInterface() || c.isAbstract()) {
            // do not process interfaces
            return;
        }

        super.visit(c, arg);

        boolean hasBeanAnn = false;
        boolean hasComponentOrConfigAnn = false;

        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        // Check if there is @Bean on any method
        for (MethodDeclaration method : c.getMethods()) {
            for (AnnotationExpr methodAnn : method.getAnnotations()) {
                if (Helper.annotationExists(methodAnn, "org.springframework.context.annotation.Bean")) {
                    hasBeanAnn = true;
                    break;
                }
            }
            if (hasBeanAnn) break;
        }

        // If there is no @Bean, no need to check further
        if (!hasBeanAnn) {
            return;
        }

        // Now check for @Configuration || @Component
        if (Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.context.annotation.Configuration")
            || Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.stereotype.Component")) {
            hasComponentOrConfigAnn = true;
        }


        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasBeanAnn;
        boolean consequent = hasComponentOrConfigAnn;

        if (antecedent && !consequent) {
            // Check if violation is potentially false positive
            if (FalsePositiveFilters.classImportedThroughAnn(c, allImportClasses)) {
                return;
            }
            if (FalsePositiveFilters.classExtendsSpringTypes(c, new HashSet<>())) {
                // method return types are not used anyways
                return;
            }
            if (FalsePositiveFilters.classContainsConditionals(c)) {
                // contains annotations like @AutoConfigureBefore which imply that spring.factories contain the class
                return;
            }

            Violation.print("@Bean on method --> @Component || @Configuration on class", classLocation);
        }
    }
}