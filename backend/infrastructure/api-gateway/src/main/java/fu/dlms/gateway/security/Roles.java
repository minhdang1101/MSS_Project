package fu.dlms.gateway.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Tên role và tên claim trong JWT do Keycloak cấp (realm "library"):
 *
 * <pre>
 * Thủ thư:  { "preferred_username": "TT-0001", "name": "...", "realm_access": {"roles": ["LIBRARIAN"]}, "exp": ... }
 * Độc giả:  { "preferred_username": "TV-0087", "name": "...", "realm_access": {"roles": ["MEMBER"]}, "mid": 2, "exp": ... }
 * </pre>
 *
 * Token ký RS256 bằng khóa riêng của Keycloak; Gateway chỉ cần khóa công khai (JWKS) để kiểm tra, không giữ bí mật nào.
 * Claim "mid" (id độc giả trong member-service) do member-service ghi vào thuộc tính người dùng khi cấp thẻ.
 */
public final class Roles {

    /** Thủ thư: quản lý toàn bộ hệ thống. */
    public static final String LIBRARIAN = "LIBRARIAN";

    /** Độc giả: chỉ thao tác trên dữ liệu của chính mình. */
    public static final String MEMBER = "MEMBER";

    /** Keycloak để role của realm trong claim realm_access.roles. */
    public static final String REALM_ACCESS_CLAIM = "realm_access";

    /** Claim chứa tên đăng nhập (số thẻ TV-xxxx hoặc mã thủ thư TT-xxxx). */
    public static final String USERNAME_CLAIM = "preferred_username";

    /** Claim chứa id độc giả (chỉ có trong token MEMBER). */
    public static final String MEMBER_ID_CLAIM = "mid";

    private Roles() {
    }

    /** Các role của realm có trong token, chỉ giữ LIBRARIAN và MEMBER (Keycloak còn thêm offline_access, ...). */
    public static List<String> appRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (!(realmAccess instanceof Map<?, ?> map) || !(map.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream().map(String::valueOf).filter(r -> LIBRARIAN.equals(r) || MEMBER.equals(r)).distinct().toList();
    }

    /** Role dùng để báo cho service phía sau: thủ thư nếu có role đó, ngược lại độc giả, không có thì null. */
    public static String primaryRole(Jwt jwt) {
        List<String> roles = appRoles(jwt);
        if (roles.contains(LIBRARIAN)) {
            return LIBRARIAN;
        }
        return roles.contains(MEMBER) ? MEMBER : null;
    }
}
