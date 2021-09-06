package parser.util;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;

import java.util.HashSet;
import java.util.Set;

public class FalsePositiveFilters {

    final static Set<String> conditionalAnns = new HashSet<>();
    static {
        conditionalAnns.add("AutoConfigure");
        conditionalAnns.add("ConditionalOn");
    };

    public static boolean classContainsConditionals(ClassOrInterfaceDeclaration c) {
        // CHECK IF FALSE POSITIVE: check if class type is imported through annotation (e.g., @Import)
        try {
            String typeName = c.resolve().getQualifiedName();

            // check if class is used as a return type
            boolean hasAlternativeWay = conditionalAnns.stream().anyMatch(
                ca -> ca.startsWith("org.springframework") && typeName.contains(ca)
            );
            if (hasAlternativeWay) {
                return true;
            }
        } catch (UnsolvedSymbolException e) {

        }
        return false;
    }

    public static boolean classImportedThroughAnn(ClassOrInterfaceDeclaration c, Set<String> allImportClasses) {
        // CHECK IF FALSE POSITIVE: check if class type is imported through annotation (e.g., @Import)
        try {
            String typeName = c.resolve().getQualifiedName();

            // check if class is used as a return type
            if (allImportClasses.contains(typeName)) {
                return true;
            }
        } catch (UnsolvedSymbolException e) {

        }
        return false;
    }

    public static boolean classUsedAsReturnType(ClassOrInterfaceDeclaration c, Set<String> methodReturnTypes) {
        // CHECK IF FALSE POSITIVE: check if class type is used as the return type
        try {
            String typeName = c.resolve().getQualifiedName();

            // check if class is used as a return type
            if (methodReturnTypes.contains(typeName)) {
                return true;
            }
            if (methodReturnTypes.parallelStream().anyMatch(rt -> rt.contains(c.getNameAsString()))) {
                return true;
            }
        } catch (UnsolvedSymbolException e) {

        }
        return false;
    }

    public static boolean classExtendsSpringTypes(ClassOrInterfaceDeclaration c, Set<String> methodReturnTypes) {
        // CHECK IF FALSE POSITIVE: If class extends or implements Spring interface, then probably not a false positive
        NodeList<ClassOrInterfaceType> extensions = c.getExtendedTypes();
        extensions.addAll(c.getImplementedTypes());

        // INFO: For now, if class extends anything, then skip it (not 100%, but likely FP)
        if (extensions.size() >= 1) {
            return true;
        }


//        for (ClassOrInterfaceType ext : extensions) {
//            try {
//                String fullName = ext.resolve().getQualifiedName();
//
//                // If extends some of Spring's types, then just return
//                if (fullName.contains("org.springframework")) {
//                    return true;
//                }
//            } catch (UnsolvedSymbolException e) {
//
//            }
//        }
        return false;
    }

}
