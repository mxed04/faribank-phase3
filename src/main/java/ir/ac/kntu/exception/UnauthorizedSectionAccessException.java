package ir.ac.kntu.exception;

/**
 * Thrown when an operator attempts to access or mutate a ticket outside assigned sections.
 */
public class UnauthorizedSectionAccessException extends FaribankException {
    public UnauthorizedSectionAccessException(String message) {
        super(message);
    }
}