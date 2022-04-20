package parser;

import java.io.*;
import java.util.*;

public class Runner {
    private static Parser builder;

    public static void main(String[] args) throws IOException {
        // Get the path to all input user projects that use MicroProfile
        if (args.length != 1) {
            System.out.println("Wrong number of args passed!!!");
            System.exit(1);
        }

        // Read in all java files
        long startProcessingTime = System.currentTimeMillis();
        readProjectsFiles(args[0]);
        long endProcessingTime = System.currentTimeMillis();

        System.out.println("Parsed in " + (endProcessingTime - startProcessingTime) + " milliseconds");
    }

//    private static String getProjectsDirectory() {
//        String projectsDir = Configuration.projectsDir;
//
//        if (!new File(projectsDir).exists()) {
//            System.out.println("Invalid projects directory!");
//            System.exit(1);
//        }
//
//        return projectsDir;
//    }

    private static void readProjectsFiles(String projectPath) {
        // Parse library sources so we get fully-qualified names
        List<File> libs = new ArrayList<>();
        for (String lib : Configuration.librariesPaths) {
            libs.add(new File(lib));
        }

        // Read projects
        builder = new Parser();
        File path = new File(projectPath);

        for(File project : Objects.requireNonNull(path.listFiles())){
            if (project.isDirectory()) {
                boolean parsed = builder.checkRulesInProject(project, libs);
                if (!parsed) {
                    System.out.println("Could not parse project " + project.getPath());
                    System.exit(1);
                }
            }
        }

    }
}
