package parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;

import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import miner.Itemset;
import parser.visitors.ConstructVisitor;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public class AnnotationUsageGraphBuilder {

    public static final Set<String> projectsWithCrossClassUsage = new HashSet<>();
    public static int numOfFieldTypeDecl = 0;
    public static int numOfParamTypeDecl = 0;
    private static final HashMap<String, List<Itemset>> usages = new HashMap<>();
    private static final Set<String> forbiddenProjs = new HashSet<String>() {{
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
    private static int countUnparseableFiles = 0;

    public class ParsingStats {
        public int unparseableBeans = 0;
    }

    public int getCountUnparseableFiles() {
        return countUnparseableFiles;
    }

    public List<Itemset> getAllUsageGraphs() {
        List<Itemset> merged = new ArrayList<>();
        for (List<Itemset> i : usages.values()) {
            List<Itemset> nonEmpty = i.stream().filter(it -> !it.isEmpty()).collect(Collectors.toList());
            merged.addAll(nonEmpty);
        }

        return merged;
    }

    public HashMap<String, List<Itemset>> getUsagesAsMap() {
        return usages;
    }

    public boolean generateUsageGraphsPerProject(
            File project,
            List<File> targetLibDependencies,
            List<File> javaFiles,
            List<File> configFiles,
            List<File> beans)
    {
        String projectName = project.getName().toLowerCase();

        // Skip simple projects
        if (forbiddenProjs.parallelStream().anyMatch(projectName::contains)) {
            return false;
        }

        List<Itemset> usagesInProject = new ArrayList<>();

        // ************ 1. Process configuration files of the project ************
        HashSet<String> configFileKeys = new HashSet<>();
        for (File configFile: configFiles) {
            // Load the config properties
            Properties configs = new Properties();
            try {
                // INFO: We are ignoring default props - does it matter?
                InputStream in = new FileInputStream(configFile);

                configs.load(in);

                in.close();
            } catch (FileNotFoundException e) {
                System.out.println(">>>>>> Could not find the config file" + configFile.getName() + " ! <<<<<<");
                e.printStackTrace();
                System.exit(1);
            } catch (IOException e) {
                System.out.println(">>> Could not close the config file" + configFile.getName() + "! <<<");
                e.printStackTrace();
                System.exit(2);
            }

            configFileKeys.addAll(configs.stringPropertyNames());
        }

        int skippedCus = 0;

        // ************ 2. Process source (java) files of the project ************
        SymbolSolverCollectionStrategy s = new SymbolSolverCollectionStrategy();

        // Add the library (or libraries) we analyze so that we get fully-qualified names
        for (File libDep : targetLibDependencies) {
            s.collect(libDep.toPath());
        }
        s.getParserConfiguration().setStoreTokens(true);

        ProjectRoot projectRoot = s.collect(project.toPath());

        System.out.println("Parsing " + projectName);
        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            Path path = sourceRoot.getRoot();

            // Ignore test files
            if (path.toString().contains("src/test")) {
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

            // Parse all subclasses and interface implementations for cross-class relationships
            List<CompilationUnit> compilationUnits = sourceRoot.getCompilationUnits();
            Map<String, Set<ClassOrInterfaceDeclaration>> subclassesByName = parseAllSubclasses(compilationUnits);
            Map<String, Set<ClassOrInterfaceDeclaration>> interfaceImplsByName = parseAllImplementations(compilationUnits);

            for (CompilationUnit cu : compilationUnits) {
                JavaParserFacade.clearInstances();
                String currFilePath = cu.getStorage().get().getPath().toString();

                if (cu.getTypes().size() > 0) {
                    // Resolve imports
                    List<String> imports = new ArrayList<>();
                    if (cu.getImports() != null && cu.getImports().size() > 0) {
                        imports = cu.getImports()
                            .stream()
                            .map(NodeWithName::getNameAsString)
                            .collect(Collectors.toList());
                    }

                    // Get relevant beans.xml from WEB-INF and META-INF
                    File metaInfBeans = null;
                    File webInfBeans = null;
                    for (File beansXml : beans) {
                        if (!beansXml.getAbsolutePath().contains("src/main")
                            || !currFilePath.contains("src/main")) {
                            continue;
                        }

                        String beansXmlPath = beansXml.getAbsolutePath().substring(0, beansXml.getAbsolutePath().indexOf("src/main"));
                        String currentFilePath = currFilePath.substring(0, currFilePath.indexOf("src/main"));
                        if (!beansXmlPath.equals("") && beansXmlPath.equals(currentFilePath)) {
                            if (beansXml.getAbsolutePath().contains("WEB-INF")) {
                                webInfBeans = beansXml;
                            }
                            else if (beansXml.getAbsolutePath().contains("META-INF")) {
                                metaInfBeans = beansXml;
                            }
                            // TODO: Check if there are multiple matches. In that case, something is def. wrong.
                        }
                    }

                    // Encapsulate parsing stats in a single object
                    ParsingStats parsingStats = new ParsingStats();

                    // Visit each file
                    cu.accept(
                        new ConstructVisitor(
                            projectName,
                            currFilePath,
                            imports,
                            configFileKeys,
                            metaInfBeans,
                            webInfBeans,
                            subclassesByName,
                            interfaceImplsByName,
                            parsingStats,
                            usagesInProject
                        ),
                        null
                    );

                    // Aggregate stats
                    countUnparseableFiles += parsingStats.unparseableBeans;
                }
            }
        }

        if (usagesInProject.size() > 0) {
            usages.put(projectName, usagesInProject);
        }

        return true;
    }

    // Given a class name, give me all subclasses
    private Map<String, Set<ClassOrInterfaceDeclaration>> parseAllSubclasses(List<CompilationUnit> cus) {
        Map<String, Set<ClassOrInterfaceDeclaration>> subclassesOf = new HashMap<>();

        // Process subclasses
        cus.forEach(cu -> {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(ci -> {
                List<ResolvedReferenceType> extendedTypes;

                try {
                    extendedTypes = ci.resolve().getAncestors(true);
                } catch (Exception e) {
                    return;
                }

                extendedTypes.forEach(extendedType -> {
                    // Get extension name (e.g., A --inherits--> B, so we get B)
                    String extendedClassName = extendedType.getQualifiedName();
                    if (extendedClassName.startsWith("java.")) {
                        return;
                    }

                    // Add subclass to a list of subclasses for the class (so we have B --> [A]).
                    Set<ClassOrInterfaceDeclaration> subclasses = subclassesOf.computeIfAbsent(extendedClassName, t -> new HashSet<>());
                    subclasses.add(ci);
                });
            });
        });

        return subclassesOf;
    }

    // Given an interface name, give me all interface implementations
    private Map<String, Set<ClassOrInterfaceDeclaration>> parseAllImplementations(List<CompilationUnit> cus) {
        Map<String, Set<ClassOrInterfaceDeclaration>> implementsOf = new HashMap<>();

        // Process interface implementations
        cus.forEach(cu -> {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(ci -> {
                List<ResolvedReferenceType> impls;

                try {
                    impls = ci.resolve().getAncestors(true);
                } catch (Exception e) {
                    return;
                }

                impls.forEach(anInterface -> {
                    // Get extension name (e.g., A --implements--> B, so we get B)
                    String extendedInterfaceName = anInterface.getQualifiedName();
                    if (extendedInterfaceName.startsWith("java.")) {
                        return;
                    }

                    // Add subclass to a list of subclasses for the class (so we have B --> [A]).
                    Set<ClassOrInterfaceDeclaration> interfaceImpls = implementsOf.computeIfAbsent(extendedInterfaceName, t -> new HashSet<>());
                    interfaceImpls.add(ci);
                });
            });
        });

        return implementsOf;
    }
}
