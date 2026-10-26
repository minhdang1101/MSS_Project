package fu.dlms.member.exception;

import lombok.Getter;

/** Too many wrong passwords for one staff code in a short time. Maps to HTTP 429. */
@Getter
public class TooManyLoginAttemptsException extends RuntimeException {

    private final long retryAfterMinutes;

    public TooManyLoginAttemptsException(long retryAfterMinutes) {
        super("Đăng nhập sai quá nhiều lần. Thử lại sau " + retryAfterMinutes + " phút.");
        this.retryAfterMinutes = retryAfterMinutes;
    }
}
