package parser.rules.springboot;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class EnablePrometheusEndpoint extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public EnablePrometheusEndpoint(String projectName,
                                    String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[EnablePrometheusEndpoint] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasEnablePrometheusEndpointAnn = false;
        boolean hasSpringBootAppAnn = false;

        // Check if @EnablePrometheusEndpoint exists (INFO: Notice that we don't have fully qualified name coz 3rd party)
        hasEnablePrometheusEndpointAnn = c.getAnnotations()
            .stream()
            .anyMatch(ae -> Helper.annotationExists(ae,
                "EnablePrometheusEndpoint"
            ) || Helper.annotationExists(ae, "io.prometheus.client.spring.boot.EnablePrometheusEndpoint"));

        if (!hasEnablePrometheusEndpointAnn) {
            return;
        }

        // Now check for @SpringBootApplication
        hasSpringBootAppAnn = Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.boot.autoconfigure.SpringBootApplication");

        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasEnablePrometheusEndpointAnn;
        boolean consequent = hasSpringBootAppAnn;

        if (antecedent && !consequent) {
            Violation.print("@EnablePrometheusEndpoint on class --> @SpringBootApplication on class", classLocation);
        }
    }
}