package miner;

import java.util.*;

public class Configuration {
    /*------------------- GENERAL CONFIGS ----------------------- */
    /**
     * All projects (collective) strategy configs
     */
    public static double minSupp = 10; // absolute support, should be at least >= 5
    public static double minConf = 0.5;

    /**
     * Version of the tool currently being run (can be any value; used just for convenience)
     */
    public final static String version = "v0.0.10_wo_maximal";

    /** Projects directory - specify the absolute path to dir that contains client projects to mine */
    public final static String projectsDir = "/home/rahatly/Desktop/projects/MiningAnnotationUsageRules/clientProjectsMP";

    /** Library prefixes/regexes - uncomment one or the other */

    // Spring
//    public final static String libSubApiRegex = "org\\.springframework\\.\\w+";
//    public final static String libPref = "org.springframework";
    // Microprofile
    public final static String libSubApiRegex = "org\\.eclipse\\.microprofile\\.\\w+";
    public final static String libPref = "org.eclipse.microprofile";

    /** Target library (or libraries) directory */
    // info: may have to adjust the correct paths. If does not work, try absolute paths, e.g., "/a/b/c"
    public final static String[] librariesPaths = {
        // spring
//        "../../libsources/spring-boot/spring-aop",
//        "../../libsources/spring-boot/spring-asm",
//        "../../libsources/spring-boot/spring-beans",
//        "../../libsources/spring-boot/spring-boot",
//        "../../libsources/spring-boot/spring-context",
//        "../../libsources/spring-boot/spring-core",
//        "../../libsources/spring-boot/spring-expression",
//        "../../libsources/spring-boot/spring-jdbc",
//        "../../libsources/spring-boot/spring-jms",
//        "../../libsources/spring-boot/spring-ldap",
//        "../../libsources/spring-boot/spring-messaging",
//        "../../libsources/spring-boot/spring-retry",
//        "../../libsources/spring-boot/spring-security",
//        "../../libsources/spring-boot/spring-shell",
//        "../../libsources/spring-boot/spring-tx",
//        "../../libsources/spring-boot/spring-web",
//        "../../libsources/spring-boot/spring-ws",

        // microprofile
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
    };

    public final static List<String> subApiLibPrefixes = new ArrayList<String>() {{
        // spring boot
//        add("org.springframework.aop");
//        add("org.springframework.asm");
//        add("org.springframework.boot");
//        add("org.springframework.beans");
//        add("org.springframework.core");
//        add("org.springframework.context");
//        add("org.springframework.expression");
//        add("org.springframework.jdbc");
//        add("org.springframework.jms");
//        add("org.springframework.ldap");
//        add("org.springframework.messaging");
//        add("org.springframework.orm");
//        add("org.springframework.retry");
//        add("org.springframework.security");
//        add("org.springframework.shell");
//        add("org.springframework.tx");
//        // spring web
//        add("org.springframework.remoting");
//        add("org.springframework.web");
//        add("org.springframework.http");

        // microprofile
        add("org.eclipse.microprofile.config");
        add("org.eclipse.microprofile.faulttolerance");
        add("org.eclipse.microprofile.graphql");
        add("org.eclipse.microprofile.health");
        add("org.eclipse.microprofile.jwt");
        add("org.eclipse.microprofile.metrics");
        add("org.eclipse.microprofile.openapi");
        add("org.eclipse.microprofile.opentracing");
        add("org.eclipse.microprofile.reactive");
        add("org.eclipse.microprofile.rest");
    }};

    /** Library prefix */
    // INFO: If you want to get usages for Java EE, then there should not be checkers for it. Unless we look for javax...
    public final static String[] apiLibPrefixes = {
        // Spring
//        "org.springframework",

        // Microprofile
        "javax",
        "org.eclipse.microprofile",
    };

    /**
     *
     * Invididual project mining strategy configs
     *
     * If false, then combines all itemsets/usages together and mines.
     * Otherwise, this will individually mine from each project, and then combine the mined frequent itemsets.
     */
    public static boolean mineIndividualProjects = true;
    public static int minItemsetSizePerProject = 1;


    /*------------- GROUP BY SUB-API BEFORE MINING -------------- */
    public static boolean mineBySubApi = true;


    /* -------- Additional dumps from intermediate steps (input & freq itemsets) -------- */
    public static boolean dumpFrequentItemsets = false;
    public static boolean dumpCandidateRules = false;
    public static boolean dumpInputItemsets = false;

}
