package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class GraphQLMutationVisitor extends VoidVisitorAdapter<Object> {

    private String projectName;
    private String filepath;

    public GraphQLMutationVisitor(String projectName,
                                 String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[GraphQLMutationVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration c, Object arg) {
        super.visit(c, arg);

        if (c.isInterface() || c.isAbstract()) {
            // do not process interfaces
            return;
        }

        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        boolean hasMutationOnMethod = false;
        for (MethodDeclaration method : c.getMethods()) {
            for (AnnotationExpr annotation : method.getAnnotations()) {

                hasMutationOnMethod = Helper.annotationExists(annotation, "org.eclipse.microprofile.graphql.Mutation");
                if (hasMutationOnMethod) {
                    break;
                }
            }

            if (hasMutationOnMethod) break;
        }

        boolean hasGraphQLApiAnnOnClass = false;
        for (AnnotationExpr annotation : classAnnotations) {
            hasGraphQLApiAnnOnClass = Helper.annotationExists(annotation, "org.eclipse.microprofile.graphql.GraphQLApi");

            if (hasGraphQLApiAnnOnClass) {
                break;
            }
        }
        // Create a location corresponding to this node
        Location classLocation = new Location(this.projectName, this.filepath, c.getName().getBegin().get().line);


        boolean antecedent = hasMutationOnMethod;
        boolean consequent = hasGraphQLApiAnnOnClass;

        // Attempt to deep scan before reporting violation
        // 1. Scan class hierarchy
        if (!consequent && Helper.annExistsinClassHierarchy(c, "org.eclipse.microprofile.graphql.GraphQLApi")) {
            consequent = true;
        }

        // 2. If custom annotation, scan the annotation declaration
        if (!consequent && Helper.annExistsInAnnDeclarationOnClass(c, "org.eclipse.microprofile.graphql.GraphQLApi")) {
            consequent = true;
        }

        if (antecedent && !consequent) {
            Violation.print("@Mutation --> @GraphQLApi", classLocation);
        }
    }
}
