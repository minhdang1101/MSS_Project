package fu.dlms.fine.messaging;

import fu.dlms.fine.config.RabbitMqConfig;
import fu.dlms.fine.service.FineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * Consumes LoanOverdue (opens an accruing fine) and LoanReturned (settles it). The routing key, not eventType,
 * decides which: it is fixed by the binding. A message that still fails after the retries in
 * spring.rabbitmq.listener.simple.retry.* is parked in the dead-letter queue instead of being lost or looping.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LoanEventListener {

    public static final String QUEUE = "fine.loan-events";
    public static final String DEAD_LETTER_QUEUE = QUEUE + ".dlq";

    private final FineService fineService;

    @Bean
    public Queue fineLoanEventsQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue fineLoanEventsDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding fineLoanReturnedBinding(TopicExchange libraryExchange) {
        return BindingBuilder.bind(fineLoanEventsQueue()).to(libraryExchange).with(RabbitMqConfig.LOAN_RETURNED);
    }

    @Bean
    public Binding fineLoanOverdueBinding(TopicExchange libraryExchange) {
        return BindingBuilder.bind(fineLoanEventsQueue()).to(libraryExchange).with(RabbitMqConfig.LOAN_OVERDUE);
    }

    @RabbitListener(queues = QUEUE)
    public void onLoanEvent(@Payload LoanEvent event, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        log.info("Received {} ({}) for loan {}", routingKey, event.eventId(), event.loanId());
        switch (routingKey) {
            case RabbitMqConfig.LOAN_OVERDUE -> fineService.onLoanOverdue(event);
            case RabbitMqConfig.LOAN_RETURNED -> fineService.onLoanReturned(event);
            default -> log.warn("Ignoring unexpected routing key {} on {}", routingKey, QUEUE);
        }
    }
}
