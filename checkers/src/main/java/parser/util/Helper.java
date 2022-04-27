package parser.util;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;

import java.util.*;

public class Helper {

    private static String[] primitiveTypes = new String[] {
        "boolean",
        "byte",
        "char",
        "short",
        "int",
        "long",
        "float",
        "double",
        "void"
    };

    public static boolean annotationMatches(AnnotationExpr ae, String partialName) {
        assert partialName.contains(".");

        String qualifiedName;
        try {
            qualifiedName = ae.resolve().getQualifiedName();
        } catch (Exception e) {
            // If the symbol resolver does not work, check in our list of imports+
            qualifiedName = ae.getNameAsString(); // default value is just the name
        }

        return qualifiedName.contains(partialName);
    }

    public static boolean anyOfAnnotationsExist(AnnotationExpr ae, String[] fullNames) {
        assert Arrays.stream(fullNames).allMatch(s -> s.contains("."));

        String qualifiedName;
        try {
            qualifiedName = ae.resolve().getQualifiedName();
        } catch (Exception e) {
            // If the symbol resolver does not work, check in our list of imports+
            qualifiedName = ae.getNameAsString(); // default value is just the name
        }

        return Arrays.asList(fullNames).contains(qualifiedName);
    }

    public static boolean annotationExists(AnnotationExpr ae, String fullName) {
        assert fullName.contains(".");

        String qualifiedName;
        try {
            qualifiedName = ae.resolve().getQualifiedName();
//            if (!qualifiedName.equals(fullName)) {
//                // TODO
//            }
        } catch (Exception e) {
            // If the symbol resolver does not work, check in our list of imports+
            qualifiedName = ae.getNameAsString(); // default value is just the name
        }
        //System.out.println(qualifiedName);

        String expected = fullName.substring(fullName.lastIndexOf(".")+1);
        String actual = qualifiedName.contains(".") ? qualifiedName.substring(qualifiedName.lastIndexOf(".")+1) : qualifiedName;
        return expected.equals(actual);
        //below code gives false positive
        //return qualifiedName.equals(fullName);
    }

    public static boolean fieldTypeExists(FieldDeclaration field, String fullName) {
        assert fullName.contains(".");

        String fieldType;
        try {
            // First, see if the symbol resolver can find the name
            fieldType = field.getCommonType().resolve().asReferenceType().getQualifiedName();
        } catch (Exception e) {
            // Alternatively, check if the method is in imports
            fieldType = field.getCommonType().asString();
        }

        return fieldType.equals(fullName);
    }

    public static boolean methodReturnTypeExists(MethodDeclaration method, String fullName) {
        assert fullName.contains(".");

        String methodReturnType;
        try {
            // First, see if the symbol resolver can find the name
            // methodReturnType = method.getType().resolve().describe();
            methodReturnType = method.resolve().getReturnType().describe();
        } catch (Exception e) {
            // Alternatively, check if the method is in imports
            methodReturnType = method.getTypeAsString();
        }
        // Handle generic types
        if (methodReturnType.contains("<")) {
            methodReturnType = methodReturnType.substring(0, methodReturnType.indexOf("<"));
        }

        // If not found, check supertypes (given that it is not a primitive type)
        if (!methodReturnType.equals(fullName) && fullName.contains(".")) {
            if (Arrays.asList(primitiveTypes).contains(method.getType().toString())) {
                // we are not dealing with primitive types
                return false;
            }

            Optional<ResolvedReferenceTypeDeclaration> typeDecl;

            try {
                typeDecl = method.resolve().getReturnType().asReferenceType().getTypeDeclaration();
            }
            catch (UnsolvedSymbolException | UnsupportedOperationException e) {
                typeDecl = Optional.empty();
            }

            if (typeDecl.isPresent()) {
                List<ResolvedReferenceType> supertypes = typeDecl.get().getAncestors(true);

                for (ResolvedReferenceType supert : supertypes) {
                    if (supert.getQualifiedName().equals(fullName)) {
                        return true;
                    }
                }
            }
        }

        return methodReturnType.equals(fullName);
    }

    public static boolean annExistsinClassHierarchy(ClassOrInterfaceDeclaration c, String annFullName) {

        try {
            if (c.resolve().hasAnnotation(annFullName)) {
                return true;
            }
        } catch (UnsolvedSymbolException e) {

        }

        return false;
    }

    public static boolean annExistsInAnnDeclarationOnClass(ClassOrInterfaceDeclaration c, String annFullName) {
        for (AnnotationExpr annotation : c.getAnnotations()) {
            if (Helper.annotationExists(annotation, annFullName)) {
                return true;
            }

            try {
                ResolvedAnnotationDeclaration annDecl = annotation.resolve();

                if (annDecl.hasAnnotation(annFullName)) {
                    return true;
                }

            } catch (UnsolvedSymbolException e) {
                // well, could not resolve, but it is probably unfamiliar annotation from 3rd party lib
                // i.e., not client (local) or target (e.g. microprofile) library.
            } catch (ClassCastException e) {
                System.out.println("This annotation could not be resolved --> " + annotation.getNameAsString());
            }
        }

        Queue<ResolvedAnnotationDeclaration> annotations = new LinkedList<>();
        // If we did not find any annotations on the class, then try BFS-ing going through annotations
        for (AnnotationExpr annotation : c.getAnnotations()) {
            try {
                ResolvedAnnotationDeclaration annDecl = annotation.resolve();

                // Make sure it's resolvable
                if (annDecl.getQualifiedName() != null) annotations.add(annDecl);
            } catch (UnsolvedSymbolException e) {
                // well, could not resolve, but it is probably unfamiliar annotation from 3rd party lib
                // i.e., not client (local) or target (e.g. microprofile) library.
            }
        }

        // Maximum depth we go to
        int maxLevels = 4;
        while (!annotations.isEmpty()) {
            if (maxLevels == 0) break;

            // Process level by level
            int n = annotations.size();

            for (int i = 0; i < n; ++i) {
                ResolvedAnnotationDeclaration resolvedAnnDecl = annotations.poll();

                if (resolvedAnnDecl.toAst().isPresent()) {
                    AnnotationDeclaration annDecl = resolvedAnnDecl.toAst().get();

                    for (AnnotationExpr ae : annDecl.getAnnotations()) {
                        try {
                            ResolvedAnnotationDeclaration composedAnnDecl = ae.resolve();

                            if (composedAnnDecl.hasAnnotation(annFullName)) {
                                // If we found target annotation, break
                                return true;
                            }
                            else if (!composedAnnDecl.getQualifiedName().startsWith("java.lang")){
                                // If we did not find, add this annotation for further inspection
                                annotations.add(composedAnnDecl);
                            }
                        } catch (UnsolvedSymbolException e) {

                        }
                    }
                }
            }

            --maxLevels;
        }

        return false;
    }

    public static boolean fieldExistsAsConstructorParam(ClassOrInterfaceDeclaration c, String fullFieldTypeName) {
        List<ConstructorDeclaration> constructors = c.getConstructors();

        for (ConstructorDeclaration constructor : constructors) {
            for (Parameter param : constructor.getParameters()) {
                String paramName = param.getTypeAsString();

                if (Arrays.asList(primitiveTypes).contains(paramName)) {
                    // we are not dealing with primitive types
                    return false;
                }

                try {
                    paramName = param.getType().resolve().asReferenceType().getQualifiedName();
                    if (paramName.equals(fullFieldTypeName)) {
                        return true;
                    }
                } catch (UnsolvedSymbolException e) {
                    // INFO: This is flaky, but works given that type is the only capitalised portion
                    if (fullFieldTypeName.contains(paramName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean extensionExists(ClassOrInterfaceType type, String fullName) {
        assert fullName.contains(".");

        String extensionType;
        try {
            // First, see if the symbol resolver can find the name
            extensionType = type.resolve().describe();
        } catch (Exception e) {
            // Alternatively, check if the method is in imports
            extensionType = type.getNameAsString();
        }
        // Handle generic types
        if (extensionType.contains("<")) {
            extensionType = extensionType.substring(0, extensionType.indexOf("<"));
        }

        String expected = fullName.substring(fullName.lastIndexOf(".")+1);
        String actual = extensionType.contains(".") ? extensionType.substring(extensionType.lastIndexOf(".")+1) : extensionType;
        return expected.equals(actual);
        //return extensionType.equals(fullName);
    }
}
