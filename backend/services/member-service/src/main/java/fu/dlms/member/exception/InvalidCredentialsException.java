package fu.dlms.member.exception;

/** Deliberately vague: never reveals whether the account or the password was wrong. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Sai tài khoản hoặc mật khẩu");
    }
}
