package fu.dlms.catalog.exception;

/** A rule that depends on today's date or on the data, so a bean-validation annotation cannot express it. */
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
