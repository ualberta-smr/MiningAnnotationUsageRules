package sideprocessing;

import java.util.ArrayList;
import java.util.List;

public class Annotation {
    private String name; // fully-qualified name
    private boolean isQualifier;
    private List<String> target;
    private List<Parameter> requiredParams;

    public static class Parameter {
        private final String name;
        private final String type; // can be primitive sometimes

        public Parameter(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String name() {
            return name;
        }

        public String type() {
            return type;
        }
    };

    public Annotation() {
        this.target = new ArrayList<>();
        this.requiredParams = new ArrayList<>();
        this.isQualifier = false; // default value
    }

    public void setName(String name) {
        // dumb assertion for fully-qualified names, but works eh
        assert name.contains(".");
        this.name = name;
    }

    public void addParameter(Parameter param) {
        this.requiredParams.add(param);
    }

    public void setQualifier(boolean qualifier) {
        isQualifier = qualifier;
    }

    public void setTarget(List<String> target) {
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public boolean isQualifier() {
        return isQualifier;
    }

    public List<String> getTarget() {
        return target;
    }

    public List<Parameter> getRequiredParams() {
        return requiredParams;
    }
}
