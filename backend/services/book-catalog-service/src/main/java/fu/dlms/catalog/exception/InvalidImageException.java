package fu.dlms.catalog.exception;

/** The uploaded cover is not a usable picture (wrong type, empty, too big). */
public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message) {
        super(message);
    }
}
