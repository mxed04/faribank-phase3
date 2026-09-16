package ir.ac.kntu.exception;

/**
 * Thrown when an admin violates hierarchy constraints such as blocking an ancestor.
 */
public class InvalidHierarchyOperationException extends FaribankException {
    public InvalidHierarchyOperationException(String message) {
        super(message);
    }
}