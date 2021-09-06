package parser.rules.springboot;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class RequestMappingControllerVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public RequestMappingControllerVisitor(String projectName,
                                           String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[JaxRsAnnotationsVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasAnyMapping = false;
        boolean hasAnyController = false;
        String[] springReqMappingAnnotations = new String[] {
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
        };
        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        for (AnnotationExpr classAnn : classAnnotations) {
            if (Helper.anyOfAnnotationsExist(classAnn, springReqMappingAnnotations)) {
                // Found antecedent to be true, good
                hasAnyMapping = true;
                break;
            }
        }

        // If there is no mapping on classes, now check methods
        if (!hasAnyMapping) {
            for (MethodDeclaration method : c.getMethods()) {
                for (AnnotationExpr methodAnn : method.getAnnotations()) {
                    if (Helper.anyOfAnnotationsExist(methodAnn, springReqMappingAnnotations)) {
                        hasAnyMapping = true;
                        break;
                    }
                }
                if (hasAnyMapping) break;
            }
        }

        // If there is no requestmapping, no need to check further
        if (!hasAnyMapping) {
            return;
        }

        // Now check for consequent
        if (Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.web.bind.annotation.RestController")
            || Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.stereotype.Controller")
            || Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.security.oauth2.provider.endpoint.FrameworkEndpoint")) {
            hasAnyController = true;
        }


        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasAnyMapping;
        boolean consequent = hasAnyController;

        if (antecedent && !consequent) {
            Violation.print("@*Mapping --> @Controller || @RestController on class or method", classLocation);
        }
    }
}