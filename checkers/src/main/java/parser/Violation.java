package parser;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

public class Violation {

    private static boolean startedWriting = false;
    private static File dump;

    public static void print(String rule, Location loc) {
        System.out.println("\n<<< [Violation Detected] >>>");
        System.out.println("\tFor rule : " + rule);
        System.out.println("\tProject  : " + loc.getProjectName());
        System.out.println("\tFile     : " + loc.getFilePath());
        System.out.println("\tLine num : " + loc.getLine());
        System.out.println();

        if (Configuration.saveViolations) {
            // 1. Check if we already started writing the file
            if (!startedWriting) {
                // Delete file contents
                dump = new File(Configuration.libraryChoice + "_violations.csv");
                if (dump.exists()) {
                    try (PrintWriter writer = new PrintWriter(dump)) {
                        writer.print("");
                    } catch (FileNotFoundException e) {
                        System.out.println("Did not find file " + dump.getName());
                    }
                }

                startedWriting = true;
            }

            // Remove redundant prefix from the path
            // FIXME: Might be flaky depending on the path ...
            String filePath = loc.getFilePath()
                .substring(80);

            StringBuilder sb = new StringBuilder();
            sb.append(loc.getProjectName());
            sb.append(",");
            sb.append(filePath);
            sb.append(",");
            sb.append(loc.getLine());
            sb.append(",");
            sb.append(rule);
            sb.append('\n');

            // 2. Save violations to a file
            try {
                FileUtils.writeStringToFile(dump, sb.toString(), Charset.defaultCharset(), true);
            } catch (IOException e) {
                System.err.println("Could not write to output file: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}
