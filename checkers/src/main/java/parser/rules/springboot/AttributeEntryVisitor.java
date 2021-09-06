package parser.rules.springboot;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import parser.Location;
import parser.Violation;
import parser.util.Helper;

public class AttributeEntryVisitor extends VoidVisitorAdapter<Object> {
    private String projectName;
    private String filepath;

    public AttributeEntryVisitor(String projectName,
                                 String filePath) {
        if (projectName == null || filePath == null) {
            throw new RuntimeException("[AttributeEntryVisitor] projectName, filePath, and importDecls cannot be null!");
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

        boolean hasAttributeAnnOnField = false;
        boolean hasEntryAnnOnClass = false;

        NodeList<AnnotationExpr> classAnnotations = c.getAnnotations();

        // Check if there is @Attribute on any method
        for (FieldDeclaration field : c.getFields()) {
            for (AnnotationExpr fieldAnn : field.getAnnotations()) {
                if (Helper.annotationExists(fieldAnn, "org.springframework.ldap.odm.annotations.Attribute")) {
                    hasAttributeAnnOnField = true;
                    break;
                }
            }
            if (hasAttributeAnnOnField) break;
        }

        // If there is no @Attribute, no need to check further
        if (!hasAttributeAnnOnField) {
            return;
        }

        // Now check for @Entry
        if (Helper.annExistsInAnnDeclarationOnClass(c, "org.springframework.ldap.odm.annotations.Entry")) {
            hasEntryAnnOnClass = true;
        }


        Location classLocation = new Location(this.projectName, this.filepath, c.getBegin().get().line);
        boolean antecedent = hasAttributeAnnOnField;
        boolean consequent = hasEntryAnnOnClass;

        if (antecedent && !consequent) {
            Violation.print("@Attribute on field --> @Entry on class", classLocation);
        }
    }
}