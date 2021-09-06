package parser.visitors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import graph.Location;
import graph.edges.*;
import graph.nodes.*;
import miner.Configuration;
import miner.Itemset;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import parser.AnnotationUsageGraphBuilder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConstructVisitor extends VoidVisitorAdapter<Object> {
    final private String projectName;
    final private String filepath;
    final private List<String> importDecls;
    final private HashSet<String> configs;
    final private List<Itemset> usagesInProject;
    final private Map<String, Set<ClassOrInterfaceDeclaration>> subclassesByName;
    final private Map<String, Set<ClassOrInterfaceDeclaration>> interfaceImplsByName;
    final private AnnotationUsageGraphBuilder.ParsingStats stats;

    private Document metaInfBeansDoc;
    private Document webInfBeansDoc;

    public ConstructVisitor(String projectName,
                            String filePath,
                            List<String> importDecls,
                            HashSet<String> configs,
                            File metaInfBeansXml,
                            File webInfBeansXml,
                            Map<String, Set<ClassOrInterfaceDeclaration>> subclassesByName,
                            Map<String, Set<ClassOrInterfaceDeclaration>> interfaceImplsByName,
                            AnnotationUsageGraphBuilder.ParsingStats stats,
                            List<Itemset> usagesInProject) {
        if (projectName == null || filePath == null || importDecls == null) {
            throw new RuntimeException("[ConstructVisitor] projectName, filePath, and importDecls cannot be null!");
        }
        this.projectName = projectName;
        this.filepath = filePath;
        this.importDecls = importDecls;
        this.configs = configs;
        this.usagesInProject = usagesInProject;
        this.stats = stats;
        this.subclassesByName = subclassesByName;
        this.interfaceImplsByName = interfaceImplsByName;

        // Parse beans.xml files
        readBeansFiles(metaInfBeansXml, webInfBeansXml);
    }

    private void readBeansFiles(File metaInfBeansXml, File webInfBeansXml) {
        DocumentBuilderFactory docBuilderFactory;
        DocumentBuilder docBuilder;
        Document metaInfBeansDoc = null;
        Document webInfBeansDoc = null;

        if (metaInfBeansXml != null && metaInfBeansXml.length() > 0) {
            // Gotta check fully-qualified name, i.e., if it exists
            // Based on https://mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
            docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilder = null;

            try {
                docBuilder = docBuilderFactory.newDocumentBuilder();
                metaInfBeansDoc = docBuilder.parse(metaInfBeansXml);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                // If xml file cannot be parsed, we should not try to fix it ... yet?
                if (e.getMessage().contains("The processing instruction target matching \"[xX][mM][lL]\"")) {
                    this.stats.unparseableBeans += 1;
                }
                if (e.getMessage().contains("Premature end of file.")) {
                    this.stats.unparseableBeans += 1;
                }
            }
            if (docBuilder == null || metaInfBeansDoc == null) {
                System.err.println("META-INF: Could not initialize DocBuilder or Document");
                // System.exit(1);
                return;
            }

            metaInfBeansDoc.getDocumentElement().normalize();
        }
        if (webInfBeansXml != null && webInfBeansXml.length() > 0) {
            // Gotta check fully-qualified name, i.e., if it exists
            // Based on https://mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
            docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilder = null;

            try {
                docBuilder = docBuilderFactory.newDocumentBuilder();
                webInfBeansDoc = docBuilder.parse(webInfBeansXml);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                // If xml file cannot be parsed, we should not try to fix it ... yet?
                if (e.getMessage().contains("The processing instruction target matching \"[xX][mM][lL]\"")) {
                    this.stats.unparseableBeans += 1;
                }
                if (e.getMessage().contains("Premature end of file.")) {
                    this.stats.unparseableBeans += 1;
                }
            }
            if (docBuilder == null || webInfBeansDoc == null) {
                System.err.println("WEB-INF: Could not initialize DocBuilder or Document");
                // System.exit(1);
                return;
            }

            webInfBeansDoc.getDocumentElement().normalize();
        }

        this.webInfBeansDoc = webInfBeansDoc;
        this.metaInfBeansDoc = metaInfBeansDoc;
    }

    private String ignoreGenericTypeParameters(String type) {
        if (type.contains("<") && type.contains(">")) {
            return type.substring(0, type.indexOf("<")); // Foo<X<A>> --> Foo
        } else {
            return type;
        }
    }

    private void addAnnotation(AnnotationExpr annotation, Node target, Itemset itemset) {
        if (!(target instanceof ConstructNode || target instanceof ParamNode)) {
            return;
        }
        // Treat a class and an annotation as nodes
        AnnotationNode annotationNode;
        try {
            // See if we can resolve the fully qualified name of some annotation
            annotationNode = new AnnotationNode(annotation.resolve().getQualifiedName());
        } catch (Exception e) {
            // If the symbol resolver does not work, check in our list of imports+
            String qualifiedName = annotation.getNameAsString(); // default value is just the name
            // Type resolution works well now, but it's a good idea to push a bit forward with this heuristic
            for (String importName : importDecls) {
                if (importName.contains(annotation.getNameAsString())) {
                    qualifiedName = importName;
                    break;
                }
            }
            annotationNode = new AnnotationNode(qualifiedName);
        }

        // Process annotation parameters
        List<ParamNode> annParams = new ArrayList<>();

        // Process different types of annotations (with and without arguments)
        if (annotation instanceof MarkerAnnotationExpr) {
            // Nothing happens here
        } else if (annotation instanceof SingleMemberAnnotationExpr) {
            // If single annotation passed (e.g., "100" or "Foo.class"), then take them as they are
            Expression expr = ((SingleMemberAnnotationExpr) annotation).getMemberValue();

            if (expr.isClassExpr()) {
                annParams.add(new ParamNode("value:java.lang.Class"));
            }
            if (expr.isBooleanLiteralExpr()) {
                annParams.add(new ParamNode("value:java.lang.Boolean"));
            }
            if (expr.isCharLiteralExpr()) {
                annParams.add(new ParamNode("value:java.lang.Char"));
            }
            if (expr.isIntegerLiteralExpr()) {
                annParams.add(new ParamNode("value:java.lang.Integer"));
            }
            if (expr.isStringLiteralExpr()) {
                annParams.add(new ParamNode("value:java.lang.String"));
            }
            if (expr.isLongLiteralExpr()) {
                annParams.add(new ParamNode("value:java.lang.Long"));
            }
            if (expr.isDoubleLiteralExpr()) {
                annParams.add(new ParamNode("value:java.lang.Double"));
            }
        } else if (annotation instanceof NormalAnnotationExpr) {
            // If key-value pair is there, just take the key name.
            // We only care about the value when dealing with config files (basically,
            // ... checking if the value is defined there).
            NodeList<MemberValuePair> args = ((NormalAnnotationExpr) annotation).getPairs();

            // Iterate over all key-value pairs (annotation args)
            for (MemberValuePair pair : args) {
                String name = pair.getName().asString();
                String value = pair.getValue().toString();
                String valueType;

                try {
                    valueType = pair.getValue().calculateResolvedType().describe();
                } catch (Exception e) {
                    valueType = "";
                }

                ParamNode param = new ParamNode(name + ":" + valueType);
                annParams.add(param);

                // If value contains double qoutes, remove them
                if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                    value = value.substring(1, value.length() - 1);
                }

                // TODO: Get the config files contents and see if value is there.
                // TODO: ... if it's there, then add new HasCorrConfig edge (from <Param> to <Config>)
                if (!value.equals("") && configs.contains(value)) {
                    // If VALUE of Annotation Param pair is present in configs as KEY, ...
                    // ... then add DefinedInEdge edge
                    // FIXME: May need to adjust name (from config: HashSet to config: HashMap<String, HashSet<String>>)
                    ConfigFileNode configNode = new ConfigFileNode("microprofile-config.properties");
                    itemset.add(new DefinedInConfigEdge(param, configNode));
                }
            }
        }

        // Formulate the "annotatedWith" relationship between the nodes
        AnnotatedWithEdge annEdge = new AnnotatedWithEdge(target, annotationNode);


        // Add all annotation parameters
        for (ParamNode param : annParams) {
            itemset.add(new HasParamEdge(annotationNode, param));
        }

        // Add the edge between target (method or class) and annotation
        itemset.add(annEdge);
    }

    private void addAnnsFromExtensions(String baseClassName, Set<String> alreadyAdded, List<AnnotatedWithEdge> typeDeclAnns) {
        Set<ClassOrInterfaceDeclaration> subclasses = subclassesByName.get(baseClassName);
        Set<ClassOrInterfaceDeclaration> interfaceImpls = interfaceImplsByName.get(baseClassName);

        for (ClassOrInterfaceDeclaration subclass : subclasses) {
            NodeList<AnnotationExpr> annotationsOnSubclasses = subclass.getAnnotations();

            for (AnnotationExpr annOnDecl : annotationsOnSubclasses) {
                try {
                    // Add relationship only once
                    if (alreadyAdded.contains(annOnDecl.resolve().getQualifiedName())) {
                        continue;
                    }

                    typeDeclAnns.add(
                        new AnnotatedWithEdge(
                            // TODO: for now, it is straight fieldTypeDecl. But has to be subclass
                            new FieldTypeDeclNode("SubClass_" + baseClassName),
                            new AnnotationNode(annOnDecl.resolve().getQualifiedName())
                        )
                    );

                    alreadyAdded.add(annOnDecl.resolve().getQualifiedName());
                } catch (Exception e) {
                    // Ignore the ones that are not resolvable
                }
            }
        }

        // Add interface implementations' annotations
        for (ClassOrInterfaceDeclaration interfaceImpl : interfaceImpls) {
            NodeList<AnnotationExpr> annotationsOnInterfaceImpl = interfaceImpl.getAnnotations();

            for (AnnotationExpr annOnDecl : annotationsOnInterfaceImpl) {
                try {
                    // Add relationship only once
                    if (alreadyAdded.contains(annOnDecl.resolve().getQualifiedName())) {
                        continue;
                    }

                    typeDeclAnns.add(
                        new AnnotatedWithEdge(
                            // TODO: for now, it is straight fieldTypeDecl. But has to be interfaceImpl
                            new FieldTypeDeclNode("InterfaceImpl_" + baseClassName),
                            new AnnotationNode(annOnDecl.resolve().getQualifiedName())
                        )
                    );

                    alreadyAdded.add(annOnDecl.resolve().getQualifiedName());
                } catch (Exception e) {
                    // Ignore the ones that are not resolvable
                }
            }
        }
    }

    private boolean addFields(List<FieldDeclaration> fields, Itemset classItemset) {
        boolean hasAnyAnnotation = false;
        for (FieldDeclaration field : fields) {
            NodeList<AnnotationExpr> fieldAnnotations = field.getAnnotations();
            if (fieldAnnotations.size() <= 0) {
                continue;
            }

            // Found field annotations
            hasAnyAnnotation = true;

            // Try to get fully-qualified type of the field
            String fieldTypeStr = "";
            try {
                // First, see if the symbol resolver can find the name
                fieldTypeStr = field.getVariables().get(0).getType().resolve().describe();
            } catch (Exception e) {
                // Alternatively, check if the method is in imports
                fieldTypeStr = field.getVariables().get(0).getTypeAsString();
                // Type resolution works well now, but it's a good idea to push a bit forward with this heuristic
                for (String importName : importDecls) {
                    if (importName.contains(fieldTypeStr)) {
                        fieldTypeStr = importName;
                        break;
                    }
                }
            }
            if (fieldTypeStr.equals("")) {
                System.err.println(">>>> Field type (String) cannot be empty!");
                System.exit(1);
            }

            // Now try to get annotations on the type declaration
            List<AnnotatedWithEdge> typeDeclAnns = new ArrayList<>();
            try {
                Type fieldType = field.getCommonType();
                if (fieldType.isClassOrInterfaceType()) {

                    ResolvedReferenceTypeDeclaration r = fieldType.toClassOrInterfaceType().get()
                            .resolve()
                            .getTypeDeclaration().get();

                    // Assumption: take a look only at local types, not primitive types (java.*) and APIs (e.g., javax)
                    if (Arrays.stream(Configuration.apiLibPrefixes).anyMatch(api -> r.getQualifiedName().contains(api))) {
                        // Ignore API library types, we are not interested in them
                        throw new Exception("Looking at the target library API type, but need the local one from proj");
                    }
                    if (r.getQualifiedName().startsWith("java.")) {
                        throw new Exception("Looking at a primitive type, but need the local one from proj");
                    }

                    if (r instanceof JavaParserClassDeclaration) {
                        // Look at the class declaration itself
                        JavaParserClassDeclaration javaParserClassDeclaration = (JavaParserClassDeclaration) r;

                        ClassOrInterfaceDeclaration classDeclarationWrappedNode = javaParserClassDeclaration.getWrappedNode();

                        NodeList<AnnotationExpr> annotations = classDeclarationWrappedNode.getAnnotations();
                        Set<String> alreadyAdded = new HashSet<>();

                        // First try adding annotations from the class declaration itself
                        for (AnnotationExpr annOnDecl : annotations) {
                            try {
                                if (alreadyAdded.contains(annOnDecl.resolve().getQualifiedName())) {
                                    continue;
                                }

                                typeDeclAnns.add(
                                        new AnnotatedWithEdge(
                                                new FieldTypeDeclNode(r.getQualifiedName()),
                                                new AnnotationNode(annOnDecl.resolve().getQualifiedName())
                                        )
                                );

                                alreadyAdded.add(annOnDecl.resolve().getQualifiedName());
                            } catch (Exception e) {
                                // Ignore the ones that are not resolvable
                            }
                        }

                        // Now look at the subclasses and interface implementations as well
                        addAnnsFromExtensions(r.getQualifiedName(), alreadyAdded, typeDeclAnns);
                    }
                }
            } catch (Exception e) {
                // TODO
            }

            // A separate itemset for each field
            Location fieldLocation = new Location(this.projectName, this.filepath, fieldTypeStr);
            Itemset itemset = new Itemset(fieldLocation);

            // Initialize field as a node
            fieldTypeStr = ignoreGenericTypeParameters(fieldTypeStr);
            FieldNode fieldNode = new FieldNode(fieldTypeStr);

            // Add field type as returns edge
            itemset.add(new ReturnsEdge(fieldNode, new ReturnNode(fieldTypeStr)));

            // Add field annotations if any
            for (AnnotationExpr annotation : fieldAnnotations) {
                addAnnotation(annotation, fieldNode, itemset);
            }

            // Add field type declaration annotations if any
            for (AnnotatedWithEdge e : typeDeclAnns) {
                itemset.add(e);
            }

            // Add `belongsToClass` edge and all class annotations
            if (!classItemset.isEmpty()) {
                // itemset.add(new BelongsToClassEdge(fieldNode, classNode));

                for (String item : classItemset.getItems()) {
                    itemset.add(item);
                }
            }

            usagesInProject.add(itemset);
            // itemset.add(new HasFieldEdge(classNode, fieldNode));
        }
        return hasAnyAnnotation;
    }

    private void addCrossClassRelFromParam(Parameter p, Itemset methodItemset) {
        // Now try to get annotations on the type declaration
        List<AnnotatedWithEdge> typeDeclAnns = new ArrayList<>();
        try {
            Type paramType = p.getType();
            if (paramType.isClassOrInterfaceType()) {
                ResolvedReferenceTypeDeclaration r = paramType.toClassOrInterfaceType().get()
                        .resolve()
                        .getTypeDeclaration().get();

                // Assumption: take a look only at local types, not primitive types (java.*) and APIs (e.g., javax)
                if (Arrays.stream(Configuration.apiLibPrefixes).anyMatch(api -> r.getQualifiedName().contains(api))) {
                    // Ignore API library types, we are not interested in them
                    throw new Exception("Looking at the target library API type, but need the local one from proj");
                }
                if (r.getQualifiedName().startsWith("java.")) {
                    throw new Exception("Looking at a primitive type, but need the local one from proj");
                }

                if (r instanceof JavaParserClassDeclaration) {
                    JavaParserClassDeclaration javaParserClassDeclaration = (JavaParserClassDeclaration) r;

                    ClassOrInterfaceDeclaration classDeclarationWrappedNode = javaParserClassDeclaration.getWrappedNode();

                    NodeList<AnnotationExpr> annotations = classDeclarationWrappedNode.getAnnotations();

                    Set<String> alreadyAdded = new HashSet<>();

                    for (AnnotationExpr annOnDecl : annotations) {
                        try {
                            if (alreadyAdded.contains(annOnDecl.resolve().getQualifiedName())) {
                                continue;
                            }

                            typeDeclAnns.add(
                                    new AnnotatedWithEdge(
                                            new ParamTypeDeclNode(r.getQualifiedName()),
                                            new AnnotationNode(annOnDecl.resolve().getQualifiedName())
                                    )
                            );

                            alreadyAdded.add(annOnDecl.resolve().getQualifiedName());
                        } catch (Exception e) {
                            // Ignore the ones that are not resolvable
                        }
                    }

                    // Now look at the subclasses and interface implementations as well
                    addAnnsFromExtensions(r.getQualifiedName(), alreadyAdded, typeDeclAnns);
                }
            }
        } catch (Exception e) {
            // TODO
        }

        // Add cross-class rels to the method itemset from parameter type to the annotations on its declaration
        for (AnnotatedWithEdge e : typeDeclAnns) {
            methodItemset.add(e);
        }
    }

    private boolean addMethods(List<MethodDeclaration> methods, Itemset classItemset) {
        boolean hasAnyAnnotation = false;
        for (MethodDeclaration method : methods) {
            // Ignore methods without annotations (as well as ignore Overrides)
            NodeList<AnnotationExpr> methodAnnotations = method.getAnnotations();
            long acceptableAnnotations = methodAnnotations.parallelStream()
                    .filter(t -> !t.toString().contains("Override"))
                    .count();
            if (methodAnnotations.size() <= 0 || acceptableAnnotations <= 0) {
                continue;
            }
            // Found method annotations
            hasAnyAnnotation = true;

            // Create a location corresponding to this node
            String methodName = method.getNameAsString();
            Location methodLocation = new Location(this.projectName, this.filepath, methodName);
            Itemset itemset = new Itemset(methodLocation);

            MethodNode methodNode;

            // Get return typeNode of the method
            String methodReturnType;
            try {
                // First, see if the symbol resolver can find the name
                methodReturnType = method.getType().resolve().describe();
            } catch (Exception e) {
                // Alternatively, check if the method is in imports
                methodReturnType = method.getTypeAsString();
                // Type resolution works well now, but it's a good idea to push a bit forward with this heuristic
                for (String importName : importDecls) {
                    if (importName.contains(methodReturnType)) {
                        methodReturnType = importName;
                        break;
                    }
                }
            }

            // Get parameters
            StringBuilder methodParamTypes = new StringBuilder("");
            List<ParamNode> paramNodes = new ArrayList<>();
            for (Parameter p : method.getParameters()) {
                String parameterType;
                try {
                    // First, see if resolver can find the ParamNode
                    parameterType = p.getType().resolve().describe();
                } catch (Exception e) {
                    // Alternatively, check if ParamNode is in imports
                    parameterType = p.getTypeAsString();
                    // Type resolution works well now, but it's a good idea to push a bit forward with this heuristic
                    for (String importName : importDecls) {
                        if (importName.contains(parameterType)) {
                            parameterType = importName;
                            break;
                        }
                    }
                }

                parameterType = ignoreGenericTypeParameters(parameterType);
                methodParamTypes.append(parameterType).append(",");

                ParamNode paramNode = new ParamNode(parameterType);
                NodeList<AnnotationExpr> paramAnnotations = p.getAnnotations();
                long possibleAnnotations = paramAnnotations.parallelStream()
                        .filter(t -> !t.toString().contains("Override"))
                        .count();

                // Add parameters that are annotated with annotations
                if (paramAnnotations.size() > 0 && possibleAnnotations > 0) {
                    for (AnnotationExpr annotation : paramAnnotations) {
                        addAnnotation(annotation, paramNode, itemset);
                    }

                    // and then add parameter type declaration
                    // Assumption: only look at anns on the type declaration if there are anns on the parameter itself
                    addCrossClassRelFromParam(p, itemset);
                }

                // Creating separate ParamNode for each parameter in a method
                paramNodes.add(paramNode);
            }
            // Check if a method has any parameters
            if (methodParamTypes.length() > 0) {
                methodParamTypes.deleteCharAt(methodParamTypes.length() - 1);
            }

            // Add method node to the graph
            methodReturnType = ignoreGenericTypeParameters(methodReturnType);
            methodNode = new MethodNode(methodName, methodReturnType, methodParamTypes.toString());

            // Add method's parameters to the graph
            for (ParamNode paramNode : paramNodes) {
                itemset.add(new HasParamEdge(methodNode, paramNode));
            }

            // Add returns edge with node type
            ReturnNode returnType = new ReturnNode(methodReturnType);
            itemset.add(new ReturnsEdge(methodNode, returnType));

            // Add method annotations if any
            for (AnnotationExpr annotation : methodAnnotations) {
                addAnnotation(annotation, methodNode, itemset);
            }

            // Add `belongsToClass` edge and all class annotations
            if (!classItemset.isEmpty()) {
                // itemset.add(new BelongsToClassEdge(methodNode, classNode));

                for (String item : classItemset.getItems()) {
                    itemset.add(item);
                }
            }

            usagesInProject.add(itemset);

            // After all annotation parsing, add the "hasMethod" relationship between the class and each method
            // FIXME: Edge "class --hasMethod--> method" is ignored for now
            // HasMethodEdge hasMethodEdge = new HasMethodEdge(classNode, methodNode);
            // itemset.add(new HasMethodEdge(classNode, methodNode));
        }
        return hasAnyAnnotation;
    }

    private boolean addConstructor(List<ConstructorDeclaration> constructors, Itemset classItemset) {
        boolean hasAnyAnnotation = false;

        for (ConstructorDeclaration constructor : constructors) {
            // Ignore constructors without annotations (as well as ignore Overrides)
            NodeList<AnnotationExpr> constructorAnnotations = constructor.getAnnotations();
            long acceptableAnnotations = constructorAnnotations.parallelStream()
                    .filter(t -> !t.toString().contains("Override"))
                    .count();
            if (constructorAnnotations.size() <= 0 || acceptableAnnotations <= 0) {
                continue;
            }

            // Found constructor annotations
            hasAnyAnnotation = true;

            // Create a location corresponding to this node
            String constructorName = constructor.getNameAsString();
            Location constructorLocation = new Location(this.projectName, this.filepath, constructorName);
            Itemset itemset = new Itemset(constructorLocation);

            ConstructorNode constructorNode;

            // Get parameters
            StringBuilder constructorParamTypes = new StringBuilder("");
            List<ParamNode> paramNodes = new ArrayList<>();
            for (Parameter p : constructor.getParameters()) {
                String parameterType;
                try {
                    // First, see if resolver can find the ParamNode
                    parameterType = p.getType().resolve().describe();
                } catch (Exception e) {
                    // Alternatively, check if ParamNode is in imports
                    parameterType = p.getTypeAsString();
                    // Type resolution works well now, but it's a good idea to push a bit forward with this heuristic
                    for (String importName : importDecls) {
                        if (importName.contains(parameterType)) {
                            parameterType = importName;
                            break;
                        }
                    }
                }

                parameterType = ignoreGenericTypeParameters(parameterType);
                constructorParamTypes.append(parameterType).append(",");

                ParamNode paramNode = new ParamNode(parameterType);
                NodeList<AnnotationExpr> paramAnnotations = p.getAnnotations();
                long possibleAnnotations = paramAnnotations.parallelStream()
                        .filter(t -> !t.toString().contains("Override"))
                        .count();

                // Add parameters that are annotated with annotations
                if (paramAnnotations.size() > 0 && possibleAnnotations > 0) {
                    for (AnnotationExpr annotation : paramAnnotations) {
                        addAnnotation(annotation, paramNode, itemset);
                    }
                }

                // Creating separate ParamNode for each parameter in a constructor
                paramNodes.add(paramNode);
            }
            // Check if a constructor has any parameters
            if (constructorParamTypes.length() > 0) {
                constructorParamTypes.deleteCharAt(constructorParamTypes.length() - 1);
            }

            // Add constructor node to the graph
            constructorNode = new ConstructorNode(constructorName, constructorParamTypes.toString());

            // Add method's parameters to the graph
            for (ParamNode paramNode : paramNodes) {
                itemset.add(new HasParamEdge(constructorNode, paramNode));
            }

            // Add constructor annotations if any
            for (AnnotationExpr annotation : constructorAnnotations) {
                addAnnotation(annotation, constructorNode, itemset);
            }

            // Add `belongsToClass` edge and all class annotations
            if (!classItemset.isEmpty()) {
                //itemset.add(new BelongsToClassEdge(methodNode, classNode));

                for (String item : classItemset.getItems()) {
                    itemset.add(item);
                }
            }

            usagesInProject.add(itemset);

            // After all annotation parsing, add the "hasMethod" relationship between the class and each method
            // FIXME: Edge "class --hasMethod--> method" is ignored for now
            // HasMethodEdge hasMethodEdge = new HasMethodEdge(classNode, methodNode);
            // itemset.add(new HasMethodEdge(classNode, methodNode));
        }
        return hasAnyAnnotation;
    }

    private void addExtendedClassOrInterface(List<ResolvedReferenceType> extendedTypes, ClassOrInterfaceNode root, Itemset classItemset) {
        String targetLib = "";
        ClassOrInterfaceNode.Type type = null;

        for (ResolvedReferenceType c : extendedTypes) {
            // Try to get fully qualified name
            boolean foundFullName = false;
            ResolvedReferenceType t;

            try {
                t = c;
            } catch (UnsolvedSymbolException | StackOverflowError | IllegalArgumentException e) {
                t = null;
            } catch (UnsupportedOperationException e) {
                System.out.println("*** Could not parse " + filepath + " due to unsupported operation exception: ");
                e.printStackTrace();
                t = null;
            }
            String extendedClassName = t == null ? "" : t.getQualifiedName();

            // Get full name
            if (extendedClassName.contains(".")) {
                foundFullName = true;
            } else {
                extendedClassName = c.describe();
            }

            // Get extension type
            if (c.getTypeDeclaration().get().isClass()) {
                type = ClassOrInterfaceNode.Type.CLASS;
            } else {
                type = ClassOrInterfaceNode.Type.INTERFACE;
            }

            boolean foundLibrary = false;
            // FIXME: Only check if extends class from target library
            // Check if it belongs to our type or not
            for (String lib : Configuration.apiLibPrefixes) {
                if (extendedClassName.contains(lib)) {
                    targetLib = extendedClassName;
                    foundLibrary = true;
                    break;
                }
            }

            // FIXME: only checks one class now. Do we take whatever we see?
            // FIXME: plus, does not go up the hierarchy.
            if (foundLibrary) {
                break;
            }
        }

        // If we don't find any class from the library we analyze, then default to general "class"
        // Otherwise, we take the class name from the library.
        if (targetLib.equals("")) {
            // Don't add anything if no class is found ...
            targetLib = "Class";
            return;
        }

        ExtendsEdge extendsEdge = new ExtendsEdge(root, new ClassOrInterfaceNode(targetLib, type));
        classItemset.add(extendsEdge);
    }

    private void addDeclaredAs(String className, ClassOrInterfaceNode classOrInterfaceNode, Document beansXmlDoc, Itemset classItemset) {
        if (beansXmlDoc == null) {
            // Nothing to do
            return;
        }

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = null;
        try {
            // Compile what we are looking for: any element with attribute `class=a.b.c.Foo`
            StringBuilder sb = new StringBuilder();
            sb.append("//*[@class=\"");
            sb.append(className);
            sb.append("\"]");
            expr = xpath.compile(sb.toString());

            // Evaluate
            org.w3c.dom.NodeList nl = (org.w3c.dom.NodeList) expr.evaluate(beansXmlDoc, XPathConstants.NODESET);

            if (nl.getLength() > 1) {
                System.out.println("[WARNING]: Node element with class name " + className + " appears multiple times.");
            }

            // Add relationship(s) if any.
            for (int i = 0; i < nl.getLength(); ++i) {
                org.w3c.dom.Node element = nl.item(i);

                BeansFileNode parent = new BeansFileNode(element.getNodeName() + ".class");
                classItemset.add(new DeclaredInBeansEdge(classOrInterfaceNode, parent));
            }
        } catch (XPathExpressionException ignored) {

        }

    }

    private String getParentNameInBeansXml(String className, Document beansXmlDoc) {
        if (beansXmlDoc == null) {
            return "";
        }

        // Traverse all elements with class node and get their parents if className matches
        org.w3c.dom.NodeList elements = beansXmlDoc.getDocumentElement().getElementsByTagName("class");
        String targetParentName = "";
        for (int i = 0; i < elements.getLength(); ++i) {
            org.w3c.dom.Node element = elements.item(i);

            if (element.getTextContent() != null && element.getTextContent().contains(className)) {
                targetParentName = element.getParentNode().getNodeName();
                break; // found the class, so no need to search more
            }
        }

        // TODO look for more things


        return targetParentName;
    }

    private void addDeclaredInBeans(String className, ClassOrInterfaceNode classOrInterfaceNode, Itemset classItemset) {
        // Check if exists in META-INF
        String parentInMetaInf = getParentNameInBeansXml(className, this.metaInfBeansDoc);
        if (!parentInMetaInf.equals("")) {
            // add ConfigNode?
            // META-INF/{Parent}
            BeansFileNode parent = new BeansFileNode("childOf<" + parentInMetaInf + ">");
            classItemset.add(new DeclaredInBeansEdge(classOrInterfaceNode, parent));
        }

        // Check if exists in WEB-INF
        String parentInWebInf = getParentNameInBeansXml(className, this.webInfBeansDoc);
        if (!parentInWebInf.equals("")) {
            // add ConfigNode?
            // WEB-INF/{Parent}
            BeansFileNode parent = new BeansFileNode("childOf<" + parentInWebInf + ">");
            classItemset.add(new DeclaredInBeansEdge(classOrInterfaceNode, parent));
        }
    }

    private List<ResolvedReferenceType> allAncestors(ResolvedReferenceTypeDeclaration c) {

        try {
            return c.getAncestors(true);
        } catch (IllegalArgumentException e) {
            System.out.println("[allAncestors] Illegal argument exception for " + c.getName());
            return new ArrayList<>();
        }

//            List<ResolvedReferenceType> resolvableAncestors = new ArrayList<>();
//            Queue<ResolvedReferenceType> q;

//            try {
//                q = new ArrayDeque<>(c.getAncestors(true));
//            } catch (IllegalArgumentException e) {
//                // If there's a problem with type parameters, just ignore the extensions
//                return new ArrayList<>();
//            }
//
//            Set<ResolvedReferenceType> cacheForSeenTypes = new HashSet<>();
//
//            System.out.println("---- new call to allAncestors ----");
//            while (!q.isEmpty()) {
//                System.out.println("Queue size = " + q.size());
//                ResolvedReferenceType head = q.poll();
//
//                // This will hopefully let us skip class A definition once it's processed
//                if (cacheForSeenTypes.contains(head)) {
//                    continue;
//                } else {
//                    cacheForSeenTypes.add(head);
//                }
//
//                try {
//                    // Get all parents in the current level of hierarchy
//                    List<ResolvedReferenceType> parents = head.getDirectAncestors();
//                    String packageName = c.getPackageName();
//
//                    for (ResolvedReferenceType parent : parents) {
//                        // If contains any library target prefix, then put it in the bag (we will return that altogether)
//                        boolean isAnyTargetLibType = Arrays.stream(Configuration.apiLibPrefixes)
//                            .anyMatch(lib -> parent.getQualifiedName().contains(lib));
//                        boolean isInternalType = parent.getQualifiedName().contains(packageName);
//
//                        if (isAnyTargetLibType) {
//                            resolvableAncestors.add(parent);
//                            // NOTE: this could be sped up if we are only interested in target lib extensions.
//                            // In that case, just return `resolvableAncestors` here.
//                        }
//
//                        if (isInternalType) {
//                            q.add(parent);
//                        }
//                    }
//                } catch (UnsolvedSymbolException e) {
//                    System.out.println("[allAncestors] Could not get parents for class " + head.getQualifiedName());
//                }
//            }
//
//            return resolvableAncestors;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Object arg) {
        // NOTE: Important to iterate over the subtree first to get more info about methods
        // and only then process it
        super.visit(n, arg);

        NodeList<AnnotationExpr> classAnnotations = n.getAnnotations();

        /*
         * I. Add "annotatedWith" relationships between a class and annotations, if any.
         */
        // Create a location corresponding to this node
        String className;
        if (n.getFullyQualifiedName().isPresent()) {
            className = n.getFullyQualifiedName().get();
        } else {
            className = n.getNameAsString();
        }
        Location classLocation = new Location(this.projectName, this.filepath, className);
        Itemset classItemset = new Itemset(classLocation);

        // Add class node to the graph
        ClassOrInterfaceNode classOrInterfaceNode = new ClassOrInterfaceNode("", ClassOrInterfaceNode.Type.CLASS); // ignore class name for here

        // Add class annotations if any
        for (AnnotationExpr annotation : classAnnotations) {
            addAnnotation(annotation, classOrInterfaceNode, classItemset);
        }

        // Check if exists in beans.xml
        addDeclaredInBeans(className, classOrInterfaceNode, classItemset);

        // Process configuration files (beans.xml)
        addDeclaredAs(className, classOrInterfaceNode, this.metaInfBeansDoc, classItemset);
        addDeclaredAs(className, classOrInterfaceNode, this.webInfBeansDoc, classItemset);

        /*
         * II. Add "hasMethod" and "AnnotatedWithEdge" relationships.
         *     For every method, parse all annotations and add corresp relationships.
         *     If there are no methods with annotations, then ignore this class.
         */
        List<MethodDeclaration> methods = n.getMethods();
        List<FieldDeclaration> fields = n.getFields();
        List<ConstructorDeclaration> constructors = n.getConstructors();

        // Ignore the current class if it does not have any methods or fields
        if (methods.size() <= 0 && fields.size() <= 0) {
            return;
        }

        // Add superclass extensions and/or interface implementations.
        List<ResolvedReferenceType> parentClassesOrInterfaces = allAncestors(n.resolve());
        addExtendedClassOrInterface(parentClassesOrInterfaces, classOrInterfaceNode, classItemset);

        // We should have at least one (field or method) annotation.
        // For all fields, we parse their annotations
        boolean hasAnyAnnotation = addFields(fields, classItemset);

        // For all methods, we parse their annotations
        if (addMethods(methods, classItemset)) {
            hasAnyAnnotation = true;
        }

        // Process constructors (and their params, annotations, param annotations, etc.)
        if (addConstructor(constructors, classItemset)) {
            hasAnyAnnotation = true;
        }

        // TODO: itemsets for methods, fields, and classes are all separate, for now.
//            if (hasAnyAnnotation && !classItemset.isEmpty()) {
//                usagesInProject.add(classItemset);
//            }
    }
}