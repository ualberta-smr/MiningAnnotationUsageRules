package parser.rules.springboot;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import parser.Location;
import parser.Violation;
import parser.util.FalsePositiveFilters;
import parser.util.Helper;

import java.util.Set;

public class AutowiredComponentVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;
    final private Set<String> methodReturnTypes;
    final private Set<String> allImportClasses;

    public AutowiredComponentVisitor(String projectName,
                                     String filePath,
                                     Set<String> methodReturnTypes,
                                     Set<String> allImportClasses) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[AutowiredComponentVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
        this.methodReturnTypes = methodReturnTypes;
        this.allImportClasses = allImportClasses;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration c, Object arg) {
        if (c.isInterface() || c.isAbstract()) {
            // do not process interfaces
            return;
        }

        super.visit(c, arg);

        boolean hasAutowiredAnn = false;
        boolean hasComponentOrConfigAnn = false;

        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        // Check if there is @Autowired on any method
        for (FieldDeclaration field : c.getFields()) {
            for (AnnotationExpr fieldAnn : field.getAnnotations()) {
                if (Helper.annotationExists(fieldAnn, "org.springframework.beans.factory.annotation.Autowired")) {
                    hasAutowiredAnn = true;
                    break;
                }
            }
            if (hasAutowiredAnn) break;
        }

        // If there is no @Autowired, no need to check further
        if (!hasAutowiredAnn) {
            return;
        }

        // Now check for @Component
        if (Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.stereotype.Component")) {
            hasComponentOrConfigAnn = true;
        }


        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasAutowiredAnn;
        boolean consequent = hasComponentOrConfigAnn;

        if (antecedent && !consequent) {
            // CHECK IF FALSE POSITIVE: check if class type is used as the return type
            if (FalsePositiveFilters.classUsedAsReturnType(c, methodReturnTypes)) {
                return;
            }

            // CHECK IF FALSE POSITIVE: If class extends or implements Spring interface, then probably not a false positive
            if (FalsePositiveFilters.classExtendsSpringTypes(c, methodReturnTypes)) {
                return;
            }

            // CHECK IF FALSE POSITIVE: If class is used as value in @Import (imported other way)
            if (FalsePositiveFilters.classImportedThroughAnn(c, allImportClasses)) {
                return;
            }

            Violation.print("@Autowired on field --> @Component on class", classLocation);
        }
    }
}