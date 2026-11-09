package fu.dlms.loan.messaging;

import fu.dlms.loan.config.RabbitMqConfig;
import fu.dlms.loan.entity.OutboxEvent;
import fu.dlms.loan.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

/**
 * Polls unpublished outbox rows and publishes them to RabbitMQ, oldest first (at-least-once delivery: a crash
 * between publish and the "published" update sends the event again, so consumers de-duplicate on eventId).
 * With publisher confirms on, a row is marked published only after the broker has accepted the message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    @Value("${app.outbox.confirm-timeout-ms:5000}")
    private long confirmTimeoutMs;

    @Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            try {
                publish(event);
            } catch (AmqpException | IllegalArgumentException e) {
                // Stop here so the events of a loan keep their order; the next run starts again from this row.
                log.warn("Could not publish outbox event {} ({}): {}", event.getId(), event.getEventType(), e.getMessage());
                return;
            }
            event.setPublished(true);
            event.setPublishedAt(clock.instant());
            outboxEventRepository.save(event);
        }
    }

    private void publish(OutboxEvent event) {
        String routingKey = LoanEventType.fromEventName(event.getEventType()).routingKey();
        // The payload is already JSON: send the bytes as they are (a message converter would wrap them in quotes).
        Message message = MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(event.getId())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("eventType", event.getEventType())
                .build();

        if (rabbitTemplate.getConnectionFactory().isSimplePublisherConfirms()) {
            rabbitTemplate.invoke(ops -> {
                ops.send(RabbitMqConfig.EXCHANGE, routingKey, message);
                ops.waitForConfirmsOrDie(confirmTimeoutMs);
                return null;
            });
        } else {
            rabbitTemplate.send(RabbitMqConfig.EXCHANGE, routingKey, message);
        }
        log.debug("Published {} {} for loan {}", event.getEventType(), event.getId(), event.getAggregateId());
    }
}
