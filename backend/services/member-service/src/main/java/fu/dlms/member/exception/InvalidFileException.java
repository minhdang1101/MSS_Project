package fu.dlms.member.exception;

/** Thrown when an uploaded file cannot be used at all (wrong format, unreadable, too large). Maps to HTTP 400. */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
