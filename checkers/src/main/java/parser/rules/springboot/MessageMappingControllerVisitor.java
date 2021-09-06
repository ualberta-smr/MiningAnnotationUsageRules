package parser.rules.springboot;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class MessageMappingControllerVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public MessageMappingControllerVisitor(String projectName,
                                           String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[MessageMappingControllerVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasMessageMappingOnMethod = false;
        boolean hasControllerOnClass = false;

        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        // Check if there is @Bean on any method
        for (MethodDeclaration method : c.getMethods()) {
            for (AnnotationExpr methodAnn : method.getAnnotations()) {
                if (Helper.annotationExists(methodAnn, "org.springframework.messaging.handler.annotation.MessageMapping")) {
                    hasMessageMappingOnMethod = true;
                    break;
                }
            }
            if (hasMessageMappingOnMethod) break;
        }

        // If there is no @Bean, no need to check further
        if (!hasMessageMappingOnMethod) {
            return;
        }

        // Now check for @Configuration || @Component
        if (Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.stereotype.Controller")) {
            hasControllerOnClass = true;
        }


        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasMessageMappingOnMethod;
        boolean consequent = hasControllerOnClass;

        if (antecedent && !consequent) {
            Violation.print("@MessageMapping on method --> @Controller on class", classLocation);
        }
    }
}