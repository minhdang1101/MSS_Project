package fu.dlms.loan.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id);
    }

    /** Keeps the message another service sent with its 404. */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
