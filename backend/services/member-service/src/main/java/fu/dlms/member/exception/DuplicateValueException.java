package fu.dlms.member.exception;

import lombok.Getter;

/** A value that must be unique (email, phone) is already used by another member. Maps to HTTP 409 on {@code field}. */
@Getter
public class DuplicateValueException extends RuntimeException {

    private final String field;

    public DuplicateValueException(String field, String message) {
        super(message);
        this.field = field;
    }

    public static DuplicateValueException email() {
        return new DuplicateValueException("email", "Email này đã được dùng cho thành viên khác");
    }

    public static DuplicateValueException phone() {
        return new DuplicateValueException("phone", "Số điện thoại này đã được dùng cho thành viên khác");
    }
}
