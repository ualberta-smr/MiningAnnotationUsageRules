package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class GraphQLQueriesVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public GraphQLQueriesVisitor(String projectName,
                                 String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[ConstructVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasMutationOrQueryOnMethod = false;
        for (MethodDeclaration method : c.getMethods()) {
            for (AnnotationExpr annotation : method.getAnnotations()) {

                hasMutationOrQueryOnMethod = Helper.annotationExists(annotation, "org.eclipse.microprofile.graphql.Mutation")
                    || Helper.annotationExists(annotation, "org.eclipse.microprofile.graphql.Query");
                if (hasMutationOrQueryOnMethod) {
                    break;
                }
            }

            if (hasMutationOrQueryOnMethod) break;
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


        boolean antecedent = hasMutationOrQueryOnMethod;
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
            Violation.print("@Query|@Mutation --> @GraphQLApi", classLocation);
        }
    }
}