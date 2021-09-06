package parser.rules.microprofile;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class ConnectorFactoryVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public ConnectorFactoryVisitor(String projectName,
                                   String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[ConnectorFactoryVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration c, Object arg) {
        super.visit(c, arg);

        /*
        *
        *
        *
        *
        * INCORRECT!!!! SHOULD CHECK IF VALUE OF THE @CONNECTOR PARAMETER IS A CLASS THAT EXTENDS/IMPLEMENTS STUFF
        *
        *
        *
        *
        *
        * */

        if (c.isInterface() || c.isAbstract()) {
            // do not process interfaces
            return;
        }

        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        // Check if a class is annotation with @Connector
        boolean hasConnectorAnn = false;
        for (AnnotationExpr annotation : classAnnotations) {
            hasConnectorAnn = Helper.annotationExists(annotation, "org.eclipse.microprofile.reactive.messaging.spi.Connector");
            if (hasConnectorAnn) {
                break;
            }
        }

        // Check class extensions
        NodeList<ClassOrInterfaceType> impls = c.getImplementedTypes();

        boolean hasFactoryImpl = false;
        for (ClassOrInterfaceType impl : impls) {
            hasFactoryImpl = Helper.extensionExists(impl, "org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory")
                || Helper.extensionExists(impl, "org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory");
            if (hasFactoryImpl) {
                break;
            }
        }

        boolean antecedent = hasConnectorAnn;
        boolean consequent = hasFactoryImpl;

        if (antecedent && !consequent) {
            // Report class location
            Location classLoc = new Location(
                this.projectName, this.filepath, c.getName().getBegin().get().line
            );
            Violation.print("@Connector on class --> Class implements IncomingFactoryConnector|OutgoingFactoryConnector", classLoc);
        }
    }
}