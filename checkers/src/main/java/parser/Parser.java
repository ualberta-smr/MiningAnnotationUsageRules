package parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;

import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import parser.rules.microprofile.*;
import parser.rules.springboot.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class Parser {

    private final Set<String> forbiddenProjs = new HashSet<String>() {{
        add("demo");
        add("workshop");
        add("guide");
        add("guides");
        add("example");
        add("playground");
        add("getting-started");
        add("sample");
        add("samples");
        add("starter");
        add("quickstart");
        add("quick-start");
        add("examples");
        add("tutorial");
    }};

    public boolean checkRulesInProject(
            File project,
            List<File> targetLibDependencies)
    {
        String projectName = project.getName().toLowerCase();

//        if (forbiddenProjs.parallelStream().anyMatch(projectName::contains)) {
//            return false;
//        }

        SymbolSolverCollectionStrategy s = new SymbolSolverCollectionStrategy();

        // Add the library (or libraries) we analyze so that we get fully-qualified names
        for (File libDep : targetLibDependencies) {
            s.collect(libDep.toPath());
        }
        s.getParserConfiguration().setStoreTokens(true);

        ProjectRoot projectRoot = s.collect(project.toPath());

        // Get all usages of method return types to reduce # of false positives
        Set<String> allReturnTypes = getAllMethodReturnTypes(projectRoot);
        Set<String> allImportClasses = getAllImportClasses(projectRoot);

        //System.out.println("Parsing " + projectName);
        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            Path path = sourceRoot.getRoot();

            // Ignore test files
            if (path.toString().contains("src/test") || path.toString().contains("test")) {
                continue;
            }

            try {
                sourceRoot.tryToParse();
            } catch (IOException ioException) {
                ioException.printStackTrace();
                continue;
            }

            if (sourceRoot.getCompilationUnits().size() == 0) {
                System.out.println("No compilation units");
            }

//            System.out.println(path);
            // Parse all subclasses and interface implementations for cross-class relationships
            List<CompilationUnit> compilationUnits = sourceRoot.getCompilationUnits();

            for (CompilationUnit cu : compilationUnits) {
                JavaParserFacade.clearInstances();
                String currFilePath = cu.getStorage().get().getPath().toString();

                if (cu.getTypes().size() > 0) {
                    if (Configuration.libraryChoice.equals("microprofile")) {
                        try {
                            runMicroprofileCheckers(cu, projectName, currFilePath);
                        } catch(Exception ignore){}

                    }
                    else if (Configuration.libraryChoice.equals("springboot")) {
                        runSpringCheckers(cu, projectName, currFilePath, allReturnTypes, allImportClasses);
                    }
                }
            }
        }

        return true;
    }


    /**
     * This technique looks whether some class is used as input value in @Import annotation.
     * E.g., @Import(X.class) or @Import({X.class, Y.class})
     *
     * @param project - project from which we analyze all @Import annotations
     */
    private Set<String> getAllImportClasses(ProjectRoot project) {
        Set<String> allClassesInImport = new HashSet<>();
        List<CompilationUnit> projectWideCUs = new ArrayList<>();

        for (SourceRoot sourceRoot : project.getSourceRoots()) {
            Path path = sourceRoot.getRoot();

            // Ignore test files
            if (path.toString().contains("src/test") || path.toString().contains("test")) {
                continue;
            }

            try {
                sourceRoot.tryToParse();
            } catch (IOException ioException) {
                ioException.printStackTrace();
                continue;
            }

            if (sourceRoot.getCompilationUnits().size() == 0) {
                System.out.println("No compilation units");
            }

            // Parse all subclasses and interface implementations for cross-class relationships
            List<CompilationUnit> compilationUnits = sourceRoot.getCompilationUnits();
            projectWideCUs.addAll(compilationUnits);
        }

        // Now process all compilation units and annotation @Import
        for (CompilationUnit cu : projectWideCUs) {
            JavaParserFacade.clearInstances();

            // Find all annotation expressions
            cu.findAll(AnnotationExpr.class, ae -> ae.getNameAsString().contains("Import"))
                .forEach(annotationExpr -> {
                    try {
                        ResolvedAnnotationDeclaration annDecl = annotationExpr.resolve();
                        if (annDecl.getQualifiedName().equals("org.springframework.context.annotation.Import")) {
                            if (annotationExpr.isSingleMemberAnnotationExpr()) {
                                SingleMemberAnnotationExpr ann = annotationExpr.asSingleMemberAnnotationExpr();

                                if (ann.getMemberValue().isClassExpr()) {
                                    ClassExpr ce = ann.getMemberValue().asClassExpr();
                                    String name = ce.getType().asClassOrInterfaceType().resolve().getQualifiedName();
                                    System.out.println(name);
                                    allClassesInImport.add(name);
                                }
                                else if (ann.getMemberValue().isArrayInitializerExpr()) {
                                    ArrayInitializerExpr aiexpr = ann.getMemberValue().asArrayInitializerExpr();
                                    for (Expression val : aiexpr.getValues()) {
                                        if (val.isClassExpr()) {
                                            ClassExpr ce = val.asClassExpr();
                                            String name = ce.getType().asClassOrInterfaceType().resolve().getQualifiedName();
                                            allClassesInImport.add(name);
                                        }
                                    }
                                }
                            }
                            else if (annotationExpr.isNormalAnnotationExpr()) {
                                NormalAnnotationExpr ann = annotationExpr.asNormalAnnotationExpr();
                                for (MemberValuePair mvp : ann.getPairs()) {
                                    if (mvp.getValue().isClassExpr()) {
                                        ClassExpr ce = mvp.getValue().asClassExpr();
                                        String name = ce.getType().asClassOrInterfaceType().resolve().getQualifiedName();
                                        allClassesInImport.add(name);
                                    }
                                }
                            }
                        }
                    } catch (UnsolvedSymbolException | UnsupportedOperationException e) {

                    }
                });
        }

        return allClassesInImport;
    }

    /**
     * This technique looks whether some class is used as a method return type.
     * However, this does not look at whether the method itself contains target annotation
     * or the class containing the method contains the annotation.
     * Thus, it's a probabilistic heuristic.
     *
     * @param project - project from which we analyze all methods (ideally, methods that return local ref types)
     */
    private Set<String> getAllMethodReturnTypes(ProjectRoot project) {
        Set<String> methodReturnTypes = new HashSet<>();
        List<CompilationUnit> projectWideCUs = new ArrayList<>();

        for (SourceRoot sourceRoot : project.getSourceRoots()) {
            Path path = sourceRoot.getRoot();

            // Ignore test files
            if (path.toString().contains("src/test") || path.toString().contains("test")) {
                continue;
            }

            try {
                sourceRoot.tryToParse();
            } catch (IOException ioException) {
                ioException.printStackTrace();
                continue;
            }

            if (sourceRoot.getCompilationUnits().size() == 0) {
                System.out.println("No compilation units");
            }

            // Parse all subclasses and interface implementations for cross-class relationships
            List<CompilationUnit> compilationUnits = sourceRoot.getCompilationUnits();
            projectWideCUs.addAll(compilationUnits);
        }

        // Now process all compilation units
        for (CompilationUnit cu : projectWideCUs) {
            JavaParserFacade.clearInstances();

            if (cu.getTypes().size() > 0) {
                // Find all methods and process their return types only if they are reference (class or interface) types
                cu.findAll(MethodDeclaration.class)
                    .forEach(md -> {
                        try {
                            ResolvedType resolvedReturnType = md.getType().resolve();

                            if (resolvedReturnType.isReference()) {
                                String fullReturnTypeName = resolvedReturnType.describe();
                                if (!fullReturnTypeName.startsWith("java.")) {
                                    methodReturnTypes.add(resolvedReturnType.describe());
                                }
                            }
                        } catch (Exception e) {
                            // Ignore if not resolvable or an external type is used
                        }
                    });
            }
        }

        return methodReturnTypes;
    }


    private void runSpringCheckers(CompilationUnit cu, String projectName, String currFilePath, Set<String> returnTypes, Set<String> allImportClasses) {
        // Verified
        cu.accept(
            new RequestMappingControllerVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new BeanConfigurationVisitor(
                projectName,
                currFilePath,
                allImportClasses
            ),
            null
        );

        // Verified
        cu.accept(
            new AutowiredComponentVisitor(
                projectName,
                currFilePath,
                returnTypes,
                allImportClasses
            ),
            null
        );

        // Verified
        cu.accept(
            new EnableWebSecurityClassVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new MessageMappingControllerVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new OrderComponentVisitor(
                projectName,
                currFilePath,
                returnTypes
            ),
            null
        );

        // Verified
        cu.accept(
            new RecoverRetryableServiceVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new EnableTransactMgmtClassVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new EnableJmsConfigVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new EnableRetryConfigVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new EnablePrometheusEndpoint(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new AttributeEntryVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new IdEntryVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new ConditionalOnMissingBeanVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new DependsOnBeanVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new PrimaryBeanVisitor(
                projectName,
                currFilePath
            ),
            null
        );
    }


    private void runMicroprofileCheckers(CompilationUnit cu, String projectName, String currFilePath) {
        // Verified
        /*
        cu.accept(
            new AsyncFutureCheckerVisitor(
                projectName,
                currFilePath
            ),
            null
        );
         */

        // Verified
        /*
        cu.accept(
            new JsonWebTokenFieldVisitor(
                projectName,
                currFilePath
            ),
            null
        );
         */

        // Verified
        cu.accept(
            new GraphQLQueriesVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        cu.accept(
                new GraphQLMutationVisitor(
                        projectName,
                        currFilePath
                ),
                null
        );

        // Verified
        /*
        cu.accept(
            new PathParamVisitor(
                projectName,
                currFilePath
            ),
            null
        );
         */

        // Verified
        cu.accept(
            new OutgoingAndScopeVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        cu.accept(
                new IncomingWithScopeVisitor(
                        projectName,
                        currFilePath
                ),
                null
        );

        // Verified
        cu.accept(
            new ReadinessWithHealthCheckVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        cu.accept(
            new ClaimInjectFieldVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        cu.accept(
                new LivenessWithHealthCheckVisitor(
                        projectName,
                        currFilePath
                ),
                null
        );

        cu.accept(
                new HealthWithHealthCheckVisitor(
                        projectName,
                        currFilePath
                ),
                null
        );

        cu.accept(
                new ConfigPropertyInjectFieldVisitor(
                        projectName,
                        currFilePath
                ),
                null
        );

        cu.accept(
                new RegisterRestClientPathVisitor(
                        projectName,
                        currFilePath
                ),
                null
        );

        cu.accept(
                new RegistryTypeInjectWithMetricRegistryVisitor(
                        projectName,
                        currFilePath
                ),
                null
        );

        // Verified
        cu.accept(
            new RestClientInjectFieldVisitor(
                projectName,
                currFilePath
            ),
            null
        );

        // Verified
        /*
        cu.accept(
            new JaxRsAnnotationsVisitor(
                projectName,
                currFilePath
            ),
            null
        );
         */

        // Verified TODO: Is this correct?
//        cu.accept(
//            new ConnectorFactoryVisitor(
//                projectName,
//                currFilePath
//            ),
//            null
//        );
    }
}