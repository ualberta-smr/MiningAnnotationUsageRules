package sideprocessing;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class AnnotationDeclarationReader {

    private final Map<String, Annotation> annData = new HashMap<>();

    public AnnotationDeclarationReader() {
        String filename = "target_library_annotations.json";
        try {
            JsonReader reader = new JsonReader(new FileReader("target_library_annotations.json"));
            Annotation[] annotations = new Gson().fromJson(reader, Annotation[].class);

            // For easier and faster access
            for (Annotation ann : annotations) {
                annData.put(ann.getName(), ann);
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + filename
                + " not found!!! Make sure it's the same as in AnnotationDeclarationParser.java");
        }
    }

    public Map<String, Annotation> getMap() {
        return this.annData;
    }

    public Annotation get(String fullyQualifiedAnnName) {
        return this.annData.get(fullyQualifiedAnnName);
    }
}
