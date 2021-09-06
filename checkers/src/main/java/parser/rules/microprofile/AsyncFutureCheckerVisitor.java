package parser.rules.microprofile;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class AsyncFutureCheckerVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public AsyncFutureCheckerVisitor(String projectName,
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

        boolean hasAsynchronous = false;
        for (AnnotationExpr annotation : methodAnnotations) {
            hasAsynchronous = Helper.annotationExists(annotation, "org.eclipse.microprofile.faulttolerance.Asynchronous");
            if (hasAsynchronous) {
                break;
            }
        }
        // Create a location corresponding to this node
        // String methodName = method.getNameAsString();
        Location methodLocation = new Location(this.projectName, this.filepath, method.getName().getBegin().get().line);

        boolean antecedent = hasAsynchronous;
        boolean consequent = Helper.methodReturnTypeExists(method, "java.util.concurrent.Future")
            || Helper.methodReturnTypeExists(method, "java.util.concurrent.CompletionStage");


        if (antecedent && !consequent) {
            // Check if imported locally
            boolean importedLocally = false;
            for (ImportDeclaration imp : method.findCompilationUnit().get().getImports()) {
                if (imp.getNameAsString().contains("java.util.concurrent.Future")
                    || imp.getNameAsString().contains("java.util.concurrent.CompletionStage")) {
                    System.out.println(imp.getNameAsString());
                    importedLocally = true;
                    break;
                }
                if (imp.getNameAsString().contains("java.util.concurrent")) {
                    //System.out.println(imp.getNameAsString());
                    importedLocally = true;
                    break;
                }
            }

            if (!importedLocally) {
                Violation.print("@Async --> Future | CompletionStage", methodLocation);
            }
        }
    }
}