package parser;

/**
 * Represents a file that is not somewhere in the 'src' folder.
 */
public class IncorrectSourcePathException extends Exception {
    public IncorrectSourcePathException(String errorMsg) {
        super(errorMsg);
    }
}
