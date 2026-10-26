package fu.dlms.fine.messaging;

import fu.dlms.fine.config.RabbitMqConfig;
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

    public static final String QUEUE = "fine.loan-events";

    @Bean
    public Queue fineLoanEventsQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding fineLoanReturnedBinding(Queue fineLoanEventsQueue, TopicExchange libraryExchange) {
        return BindingBuilder.bind(fineLoanEventsQueue).to(libraryExchange).with(RabbitMqConfig.LOAN_RETURNED);
    }

    @Bean
    public Binding fineLoanOverdueBinding(Queue fineLoanEventsQueue, TopicExchange libraryExchange) {
        return BindingBuilder.bind(fineLoanEventsQueue).to(libraryExchange).with(RabbitMqConfig.LOAN_OVERDUE);
    }

    @RabbitListener(queues = QUEUE)
    public void onLoanEvent(LoanEvent event) {
        log.info("Received {} for loan {}", event.eventType(), event.loanId());
        // TODO: load active FinePolicy, use FineCalculator, upsert Fine by loan_id (unique => idempotent)
    }
}
