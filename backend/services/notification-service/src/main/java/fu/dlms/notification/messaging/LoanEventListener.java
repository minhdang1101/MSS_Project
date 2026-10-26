package fu.dlms.notification.messaging;

import fu.dlms.notification.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LoanEventListener {

    public static final String QUEUE = "notification.loan-reminders";

    @Bean
    public Queue notificationQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding notificationDueSoonBinding(Queue notificationQueue, TopicExchange libraryExchange) {
        return BindingBuilder.bind(notificationQueue).to(libraryExchange).with(RabbitMqConfig.LOAN_DUE_SOON);
    }

    @Bean
    public Binding notificationOverdueBinding(Queue notificationQueue, TopicExchange libraryExchange) {
        return BindingBuilder.bind(notificationQueue).to(libraryExchange).with(RabbitMqConfig.LOAN_OVERDUE);
    }

    @RabbitListener(queues = QUEUE)
    public void onLoanEvent(LoanEvent event) {
        log.info("Received {} for loan {}", event.eventType(), event.loanId());
        // TODO: build idempotency key (type + loanId), skip if exists, otherwise save PENDING notification
    }
}
