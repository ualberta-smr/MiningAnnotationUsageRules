package sideprocessing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class AnnotationDeclarationParser {
    // These are paths to directories that contain sources of annotations (java files)
    private static final String[] librarySources = {
            "../../libsources/microprofile/microprofile-config",
            "../../libsources/microprofile/microprofile-jwt-auth",
            "../../libsources/microprofile/microprofile-fault-tolerance",
            "../../libsources/microprofile/microprofile-graphql",
            "../../libsources/microprofile/microprofile-health",
            "../../libsources/microprofile/microprofile-metrics",
            "../../libsources/microprofile/microprofile-open-api",
            "../../libsources/microprofile/microprofile-opentracing",
            "../../libsources/microprofile/microprofile-reactive-streams-operators",
            "../../libsources/microprofile/microprofile-rest-client",
            "../../libsources/javax-jars/mp-required/common-annotations-api-1.3.5/",
            "../../libsources/javax-jars/mp-required/jaxrs-api-2.1.6/",
            "../../libsources/javax-jars/mp-required/jsonb-api-1.0-1.0.2-RELEASE/",
            "../../libsources/javax-jars/mp-required/jsonp-1.1.5-RELEASE/",
            "../../libsources/javax-jars/mp-required/cdi/",
    };

    public static void main(String[] args) throws IOException {
        List<Annotation> targetLibAnnotations = new ArrayList<>();

        // First, parse library annotations from given sources and extract relevant information.
        for (String libPath : librarySources) {
            // Read files for each library
            targetLibAnnotations.addAll(parseLibraryAnnotations(libPath));
        }

        // Second, get all processed annotations (one instance contains information about one annotation)
        // and convert that to json.
        try {
            // create Gson instance with pretty-print
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            // create a writer
            Writer writer = new FileWriter("target_library_annotations.json");

            // convert user object to JSON file
            gson.toJson(targetLibAnnotations, writer);

            // close writer
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static List<Annotation> parseLibraryAnnotations(String path) {
        SymbolSolverCollectionStrategy s = new SymbolSolverCollectionStrategy();
        ProjectRoot projectRoot = s.collect((new File(path)).toPath());
        List<Annotation> targetLibAnnotations = new ArrayList<>();

        // First, parse all project sources, and for each source, parse its compilation unit (CU)
        System.out.println("***** Parsing project " + path + " ******");
        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            Path sourcePath = sourceRoot.getRoot();

            // Ignore test files
            if (sourcePath.toString().contains("src/test")) {
                continue;
            }

            try {
                sourceRoot.tryToParse();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            if (sourceRoot.getCompilationUnits().size() == 0) {
                System.out.println("No compilation units");
            }
            for (CompilationUnit cu : sourceRoot.getCompilationUnits()) {
                // String currFilePath = cu.getStorage().get().getPath().toString();

                if (cu.getTypes().size() > 0) {
                    targetLibAnnotations.addAll(processAnnDeclsInCU(cu));
                }
            }
        }

        return targetLibAnnotations;
    }

    private static List<Annotation> processAnnDeclsInCU(CompilationUnit cu) {
        // Find all annotation declarations and put it in a list
        List<AnnotationDeclaration> annDecls = cu.findAll(AnnotationDeclaration.class);
        List<Annotation> processedAnnotations = new ArrayList<>();

        // ¯\_(ツ)_/¯
        // Parsing annotations on annotations ... https://imgur.com/gallery/qxQ1v
        for (AnnotationDeclaration annotationDeclaration : annDecls) {
            System.out.println("\t" + annotationDeclaration.getNameAsString());
            Annotation annotationInfo = new Annotation();

            // Set name (should be fully-qualified name)
            annotationInfo.setName(annotationDeclaration.resolve().getQualifiedName());

            // Process annotations on the annotation declaration (Target, Qualifier, Retention, etc.)
            processAnnotationsOnAnnotationDeclaration(annotationDeclaration, annotationInfo);

            // Getting required parameters
            processRequiredAnnotationMembers(annotationDeclaration, annotationInfo);

            processedAnnotations.add(annotationInfo);
        }

        return processedAnnotations;
    }

    private static void processAnnotationsOnAnnotationDeclaration(
        AnnotationDeclaration targetAnnDecl,
        Annotation targetAnnInfo
    ) {
        // For each annotation, get values of Target, and check if annotation is a Qualifier one.
        NodeList<AnnotationExpr> annotationsOnLibraryAnnotations = targetAnnDecl.getAnnotations();
        for (AnnotationExpr annExpr : annotationsOnLibraryAnnotations) {
            String annExprName = annExpr.getNameAsString();

            if (annExprName.equals("Qualifier")) {
                targetAnnInfo.setQualifier(true);
            }
            else if (annExprName.equals("Target")) {
                List<String> targets = new ArrayList<>();

                if (annExpr.isMarkerAnnotationExpr()) {
                    // Target is empty ... cannot be here
                    // No value within a "marker annotation" (e.g. `@Deprecated`)
                    // MarkerAnnotationExpr markerAnnotationExpr = annExpr.asMarkerAnnotationExpr();
                    throw new IllegalStateException("@Target annotation is parameter-less. Where did they go?!");
                }
                else if (annExpr.isSingleMemberAnnotationExpr()) {
                    // A single member annotation expression has just ONE value and no key (e.g. `@Deprecated("reason")`)
                    SingleMemberAnnotationExpr singleMemberAnnotationExpr = annExpr.asSingleMemberAnnotationExpr();
                    Expression memberValue = singleMemberAnnotationExpr.getMemberValue();

                    if (memberValue.isArrayInitializerExpr()) {
                        ArrayInitializerExpr arrayExpr = memberValue.asArrayInitializerExpr();

                        for (Expression expr : arrayExpr.getValues()) {
                            String value = expr.toString();
                            // If there is full name (with package), then take only type/enum value.
                            if (value.contains(".")) {
                                targets.add(value.substring(value.lastIndexOf(".") + 1));
                            }
                            else {
                                targets.add(value);
                            }
                        }
                    }
                    else if (memberValue.isFieldAccessExpr()) {
                        FieldAccessExpr fieldAccessExpr = memberValue.asFieldAccessExpr();
                        targets.add(fieldAccessExpr.getNameAsString());
                    }
                    else if (memberValue.isNameExpr()) {
                        NameExpr nameExpr = memberValue.asNameExpr();
                        targets.add(nameExpr.getNameAsString());
                    }
                    else {
                        throw new IllegalStateException("@Target annotation's parameter type is weird. "
                            + "Not something I expected. I got: "
                            + memberValue.getClass());
                    }
                } else if (annExpr.isNormalAnnotationExpr()) {
                    throw new IllegalStateException("@Target has multiple parameter pairs. Hmm, unexpected. Why?!");
                } else {
                    throw new IllegalStateException("Unknown type of annotation");
                }

                targetAnnInfo.setTarget(targets);
            }
        }
    }

    private static void processRequiredAnnotationMembers(
        AnnotationDeclaration targetAnnDecl,
        Annotation targetAnnInfo
    ) {
        // Get annotation members and check if they have default values.
        // If they do have default values, it means that values are optional.
        // If they don't, then they are required.
        NodeList<BodyDeclaration<?>> members = targetAnnDecl.getMembers();
        for (BodyDeclaration<?> m : members) {
            // Processing only annotation members, not interested in fields
            if (m.isAnnotationMemberDeclaration()) {
                AnnotationMemberDeclaration member = (AnnotationMemberDeclaration) m;

                Optional<Expression> defaultVal = member.getDefaultValue();
                // Case 1: If default value is missing, then the value is required
                if (!defaultVal.isPresent()) {
                    Annotation.Parameter param = new Annotation.Parameter(
                        member.getName().toString(),
                        member.getType().resolve().describe()
                    );
                    System.out.println("\t\t" + member.getName().toString() + " is required");
                    targetAnnInfo.addParameter(param);
                }
            }
        }
    }
}
