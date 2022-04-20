package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class RegistryTypeInjectWithMetricRegistryVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public RegistryTypeInjectWithMetricRegistryVisitor(String projectName,
                                   String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[RegistryTypeInjectWithMetricRegistry] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
    }


    @Override
    public void visit(FieldDeclaration field, Object arg) {
        super.visit(field, arg);


        NodeList<AnnotationExpr> fieldAnnotations = field.getAnnotations();

        boolean hasRegistryType = false;
        boolean hasParamType = false;
        for (AnnotationExpr annotation : fieldAnnotations) {
            hasRegistryType = Helper.annotationExists(annotation, "org.eclipse.microprofile.metrics.annotation.RegistryType");
            if (hasRegistryType) {
                String paramValue = annotation.asSingleMemberAnnotationExpr().getMemberValue().toString();
                if(paramValue.equals("MetricRegistry.Type.APPLICATION") || paramValue.equals("MetricRegistry.Type.BASE")
                        || paramValue.equals("MetricRegistry.Type.VENDOR")){
                    hasParamType = true;
                }
                break;
            }
        }

        boolean hasInject = false;
        if(hasRegistryType){
            for (AnnotationExpr annotation : fieldAnnotations) {
                hasInject = Helper.annotationExists(annotation, "javax.inject.Inject");
                if (hasInject) {
                    break;
                }
            }
        }

        boolean hasMetricRegistry = false;
        if(field.getCommonType().asString().equals("MetricRegistry")){
            hasMetricRegistry = true;
        }


        // Create a location corresponding to this node
        // String methodName = method.getNameAsString();
        Location methodLocation = new Location(this.projectName, this.filepath, field.getBegin().get().line);
        boolean antecedent = hasRegistryType && hasParamType && hasInject;
        boolean consequent = hasMetricRegistry;

        if (antecedent && !consequent) {
            Violation.print("@RegistryType && @Inject on field --> MetricRegistry field type", methodLocation);
        }
    }
}
