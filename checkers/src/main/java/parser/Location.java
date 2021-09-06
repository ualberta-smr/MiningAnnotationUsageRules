package parser;

import java.io.Serializable;
import java.util.Objects;

public final class Location implements Serializable {
    private final String projectName;
    private final String filePath;
    private final int line;

    public Location(String projectName, String filePath, int line) {
        this.projectName = projectName;
        this.filePath = filePath;
        this.line = line;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLine() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(projectName, location.projectName) &&
                Objects.equals(filePath, location.filePath) &&
                Objects.equals(line, location.line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectName, filePath, line);
    }
}
