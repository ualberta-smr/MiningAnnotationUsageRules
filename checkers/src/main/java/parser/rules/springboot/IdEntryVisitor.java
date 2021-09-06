package parser.rules.springboot;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class IdEntryVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public IdEntryVisitor(String projectName,
                          String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[IdEntryVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasIdOnField = false;
        boolean hasEntryAnnOnClass = false;
        boolean isFieldOfTypeName = false;
        boolean hasAttributeOnField = false;

        // Check if class has @Entry
        if (Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.ldap.odm.annotations.Entry")) {
            hasEntryAnnOnClass = true;
        }

        // Check if there is @Id on any method
        for (FieldDeclaration field : c.getFields()) {
            Location fieldLocation = new Location(this.projectName, this.filepath, field.getBegin().get().line);

            // Check for annotations @Id and @Attribute
            for (AnnotationExpr fieldAnn : field.getAnnotations()) {
                // Check if there is @Id on field
                if (Helper.annotationExists(fieldAnn, "org.springframework.ldap.odm.annotations.Id")) {
                    hasIdOnField = true;
                }

                // Check if there is @Attribute on the same field
                if (Helper.annotationExists(fieldAnn, "org.springframework.ldap.odm.annotations.Attribute")) {
                    hasAttributeOnField = true;
                }
            }

            // Check field's type
            isFieldOfTypeName = Helper.fieldTypeExists(field, "Name")
                || Helper.fieldTypeExists(field, "javax.naming.Name");

            // Report violations of constraints, if any
            if (hasIdOnField && hasAttributeOnField) {
                // If we find a field with violation, report!
                Violation.print("@Id on field --> @Attribute MUST NOT be on field", fieldLocation);
            }
            if (hasIdOnField && !hasEntryAnnOnClass) {
                Violation.print("@Id on field --> @Entry on class", fieldLocation);
            }
            if (hasIdOnField && !isFieldOfTypeName) {
                Violation.print("@Id on field --> field has type Name", fieldLocation);
            }

            // Reset
            hasIdOnField = false;
            hasAttributeOnField = false;
        }
    }
}