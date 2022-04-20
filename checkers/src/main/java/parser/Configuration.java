package parser;

import java.util.ArrayList;
import java.util.List;

public class Configuration {
    /*------------------- GENERAL CONFIGS ----------------------- */
    //public final static String libraryChoice = "springboot";
    public final static String libraryChoice = "microprofile";

    /** Target library (or libraries) directory */
    public final static String[] librariesPaths = {
        // Spring
//        "../libsources/spring-boot/spring-aop",
//        "../libsources/spring-boot/spring-asm",
//        "../libsources/spring-boot/spring-beans",
//        "../libsources/spring-boot/spring-boot",
//        "../libsources/spring-boot/spring-cloud",
//        "../libsources/spring-boot/spring-context",
//        "../libsources/spring-boot/spring-core",
//        "../libsources/spring-boot/spring-expression",
//        "../libsources/spring-boot/spring-jdbc",
//        "../libsources/spring-boot/spring-jms",
//        "../libsources/spring-boot/spring-ldap",
//        "../libsources/spring-boot/spring-messaging",
//        "../libsources/spring-boot/spring-retry",
//        "../libsources/spring-boot/spring-security",
//        "../libsources/spring-boot/spring-shell",
//        "../libsources/spring-boot/spring-tx",
//        "../libsources/spring-boot/spring-web",
//        "../libsources/spring-boot/spring-ws",

        // Microprofile
        "../libsources/microprofile/microprofile-config",
        "../libsources/microprofile/microprofile-jwt-auth",
        "../libsources/microprofile/microprofile-fault-tolerance",
        "../libsources/microprofile/microprofile-graphql",
        "../libsources/microprofile/microprofile-health",
        "../libsources/microprofile/microprofile-metrics",
        "../libsources/microprofile/microprofile-open-api",
        "../libsources/microprofile/microprofile-opentracing",
        "../libsources/microprofile/microprofile-reactive-streams-operators",
        "../libsources/microprofile/microprofile-reactive-messaging/",
        "../libsources/microprofile/microprofile-rest-client",
        "../libsources/javax-jars/mp-required/common-annotations-api-1.3.5/",
        "../libsources/javax-jars/mp-required/jaxrs-api-2.1.6/",
        "../libsources/javax-jars/mp-required/jsonb-api-1.0-1.0.2-RELEASE/",
        "../libsources/javax-jars/mp-required/jsonp-1.1.5-RELEASE/",
        "../libsources/javax-jars/mp-required/cdi/",
        "../libsources/javax-jars/all"
    };

    /** Should we write violations to file? */
    public final static boolean saveViolations = true;
}
