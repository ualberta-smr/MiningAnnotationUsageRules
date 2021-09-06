package parser.rules.springboot;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class RecoverRetryableServiceVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public RecoverRetryableServiceVisitor(String projectName,
                                          String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[RecoverRetryableServiceVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasRecoverOrRetryableAnn = false;
        boolean hasServiceOrComponentAnn = false;

        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        // Check if there is @Recover or @Retryable on any method
        for (MethodDeclaration method : c.getMethods()) {
            for (AnnotationExpr methodAnn : method.getAnnotations()) {
                if (Helper.annotationExists(methodAnn, "org.springframework.retry.annotation.Recover")
                    || Helper.annotationExists(methodAnn, "org.springframework.retry.annotation.Retryable")) {
                    hasRecoverOrRetryableAnn = true;
                    break;
                }
            }
            if (hasRecoverOrRetryableAnn) break;
        }

        // If there is no @Bean, no need to check further
        if (!hasRecoverOrRetryableAnn) {
            return;
        }

        // Now check for @Service || @Component
        if (Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.stereotype.Service")
            || Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.stereotype.Component")) {
            hasServiceOrComponentAnn = true;
        }


        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasRecoverOrRetryableAnn;
        boolean consequent = hasServiceOrComponentAnn;

        if (antecedent && !consequent) {
            Violation.print("@Recover || @Retryable on method --> @Service || @Component on class", classLocation);
        }
    }
}