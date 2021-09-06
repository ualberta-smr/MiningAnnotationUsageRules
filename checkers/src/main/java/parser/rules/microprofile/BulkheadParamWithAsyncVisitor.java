package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;

public class BulkheadParamWithAsyncVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public BulkheadParamWithAsyncVisitor(String projectName,
                                         String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[ConstructVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
    }

    @Override
    public void visit(MethodDeclaration method, Object arg) {
        super.visit(method, arg);

        NodeList<AnnotationExpr> methodAnnotations = method.getAnnotations();

        boolean hasAsynchronousAnnOnMethod = false;
        boolean hasBulkheadAnnOnMethod = false;
        boolean usesWaitingTaskQueueParam = false;

        for (AnnotationExpr annotation : methodAnnotations) {
            String qualifiedName;
            try {
                qualifiedName = annotation.resolve().getQualifiedName();
            } catch (Exception e) {
                // If the symbol resolver does not work, check in our list of imports+
                qualifiedName = annotation.getNameAsString(); // default value is just the name
            }

            if (qualifiedName.contains("org.eclipse.microprofile.faulttolerance.Asynchronous")) {
                hasAsynchronousAnnOnMethod = true;
            }
            if (qualifiedName.contains("org.eclipse.microprofile.faulttolerance.Bulkhead")) {
                hasBulkheadAnnOnMethod = true;
            }

            if (annotation instanceof NormalAnnotationExpr) {
                NodeList<MemberValuePair> args = ((NormalAnnotationExpr) annotation).getPairs();

                // Iterate over all key-value pairs (annotation args)
                for (MemberValuePair pair : args) {
                    String name = pair.getName().asString();

                    if (name.contains("waitingTaskQueue")) {
                        usesWaitingTaskQueueParam = true;
                    }
                }
            }
        }
        // Create a location corresponding to this node
        // String methodName = method.getNameAsString();
        Location methodLocation = new Location(this.projectName, this.filepath, method.getName().getBegin().get().line);


        boolean antecedent = hasBulkheadAnnOnMethod && usesWaitingTaskQueueParam;
        boolean consequent = hasAsynchronousAnnOnMethod;

        if (antecedent && !consequent) {
            Violation.print("@Bulkhead[p=waitingTaskQueue]-->@Async", methodLocation);
        }
    }
}