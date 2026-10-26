package fu.dlms.member.exception;

/** Deliberately vague: never reveals whether the staff code or the password was wrong. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Sai mã thủ thư hoặc mật khẩu");
    }
}
