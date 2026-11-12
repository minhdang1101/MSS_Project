package fu.dlms.gateway.security;

/**
 * Tên role và tên claim trong JWT, thống nhất với member-service (fu.dlms.member.security.JwtService):
 *
 * <pre>
 * Thủ thư:  { "sub": "TT-0001", "name": "...", "role": "LIBRARIAN", "iat": ..., "exp": ... }
 * Độc giả:  { "sub": "TV-0087", "name": "...", "role": "MEMBER", "mid": 2, "iat": ..., "exp": ... }
 * </pre>
 *
 * Token ký HS256 bằng secret dùng chung (app.security.jwt.secret ở cả gateway và member-service).
 */
public final class Roles {

    /** Thủ thư: quản lý toàn bộ hệ thống (tương đương ADMIN trong đề bài). */
    public static final String LIBRARIAN = "LIBRARIAN";

    /** Độc giả: chỉ thao tác trên dữ liệu của chính mình. */
    public static final String MEMBER = "MEMBER";

    /** Claim chứa role. */
    public static final String ROLE_CLAIM = "role";

    /** Claim chứa id độc giả (chỉ có trong token MEMBER). */
    public static final String MEMBER_ID_CLAIM = "mid";

    private Roles() {
    }
}
