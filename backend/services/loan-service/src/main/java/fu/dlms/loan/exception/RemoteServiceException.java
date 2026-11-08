package fu.dlms.loan.exception;

/** Another service could not be reached or failed; the borrow can be retried later with the same key. */
public class RemoteServiceException extends RuntimeException {

    public RemoteServiceException(String service) {
        super("Dịch vụ " + service + " đang không phản hồi. Thử lại sau ít phút.");
    }
}
