package fu.dlms.loan.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import fu.dlms.loan.exception.BusinessException;
import fu.dlms.loan.exception.RemoteServiceException;
import fu.dlms.loan.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.io.InputStream;

/**
 * Turns an error answer of member-service or book-catalog-service into this service's own exceptions, keeping
 * the other service's message (they all return ApiError { message }): 404 stays 404, a broken business rule
 * (400/409/422) becomes 409, anything else means the other service is unhealthy (503).
 * Not a @Configuration on purpose: it is only applied to the Feign clients that name it.
 */
@Slf4j
public class RemoteErrorConfig {

    @Bean
    public ErrorDecoder remoteErrorDecoder(ObjectMapper objectMapper) {
        return (methodKey, response) -> {
            String service = response.request().requestTemplate().feignTarget().name();
            String message = readMessage(objectMapper, response);
            return switch (response.status()) {
                case 404 -> new ResourceNotFoundException(message != null ? message : "Không tìm thấy dữ liệu ở " + service);
                case 400, 409, 422 -> new BusinessException(message != null ? message : service + " từ chối yêu cầu");
                default -> {
                    log.warn("{} answered {} to {}: {}", service, response.status(), methodKey, message);
                    yield new RemoteServiceException(service);
                }
            };
        };
    }

    private static String readMessage(ObjectMapper objectMapper, Response response) {
        if (response.body() == null) {
            return null;
        }
        try (InputStream body = response.body().asInputStream()) {
            JsonNode node = objectMapper.readTree(body);
            if (node == null) {
                return null;
            }
            // A validation error says only "Validation failed"; the useful text is in fieldErrors.
            JsonNode fieldErrors = node.get("fieldErrors");
            if (fieldErrors != null && fieldErrors.isObject() && fieldErrors.size() > 0) {
                String first = fieldErrors.elements().next().asText();
                if (!first.isBlank()) {
                    return first;
                }
            }
            JsonNode message = node.get("message");
            return message == null || message.isNull() || message.asText().isBlank() ? null : message.asText();
        } catch (IOException | RuntimeException e) {
            return null; // not JSON (e.g. an HTML error page): fall back to the generic message
        }
    }
}
