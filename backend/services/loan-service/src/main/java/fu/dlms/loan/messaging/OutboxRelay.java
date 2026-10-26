package fu.dlms.loan.messaging;

import fu.dlms.loan.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls unpublished outbox rows and publishes them to RabbitMQ (at-least-once delivery). */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:5000}")
    public void publishPendingEvents() {
        // TODO: for each findTop100ByPublishedFalseOrderByCreatedAtAsc():
        //       rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, routingKey(eventType), payload)
        //       then mark published = true, publishedAt = now
    }
}
