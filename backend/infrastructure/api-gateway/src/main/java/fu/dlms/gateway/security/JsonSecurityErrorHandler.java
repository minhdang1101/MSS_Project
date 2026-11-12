package fu.dlms.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trả lỗi bảo mật dạng JSON giống ApiError của các service (Task G3), để frontend hiển thị được {@code message}:
 *
 * <pre>
 * { "timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Phiên đăng nhập đã hết hạn...", "path": "/api/loans" }
 * </pre>
 *
 * <ul>
 *   <li><b>401 Unauthorized</b> (entry point): chưa đăng nhập, token sai chữ ký, token hết hạn. Frontend nhận 401 sẽ
 *       tự đăng xuất (axiosClient).</li>
 *   <li><b>403 Forbidden</b> (access denied): token hợp lệ nhưng role không đủ quyền, vd MEMBER gọi API của thủ thư.</li>
 * </ul>
 */
@Component
public class JsonSecurityErrorHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 401: được gọi khi request cần đăng nhập mà không có token hợp lệ. */
    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        String message;
        String bearerError;
        if (ex instanceof InvalidBearerTokenException) {
            // Nimbus báo "Jwt expired at ..." khi exp đã qua; các lỗi khác: sai chữ ký, sai định dạng, sai thuật toán
            boolean expired = ex.getMessage() != null && ex.getMessage().contains("expired");
            message = expired ? "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại" : "Token không hợp lệ";
            bearerError = "Bearer error=\"invalid_token\"";
        } else {
            message = "Bạn chưa đăng nhập (thiếu header Authorization: Bearer <token>)";
            bearerError = "Bearer";
        }
        // Chuẩn OAuth2: báo cho client biết cần gửi Bearer token
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, bearerError);
        return write(exchange, HttpStatus.UNAUTHORIZED, message);
    }

    /** 403: được gọi khi đã đăng nhập nhưng role không được phép. */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return write(exchange, HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", exchange.getRequest().getPath().value());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":" + status.value() + "}").getBytes(StandardCharsets.UTF_8);
        }
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
