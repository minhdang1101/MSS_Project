package fu.dlms.catalog.exception;

/** Thrown when a request is valid but violates a business rule (e.g. member suspended). */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
