package fu.dlms.catalog.messaging;

import fu.dlms.catalog.config.RabbitMqConfig;
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

    public static final String QUEUE = "catalog.loan-returned";

    @Bean
    public Queue catalogLoanReturnedQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding catalogLoanReturnedBinding(Queue catalogLoanReturnedQueue, TopicExchange libraryExchange) {
        return BindingBuilder.bind(catalogLoanReturnedQueue).to(libraryExchange).with(RabbitMqConfig.LOAN_RETURNED);
    }

    @RabbitListener(queues = QUEUE)
    public void onLoanReturned(LoanEvent event) {
        log.info("Received {} for loan {}", event.eventType(), event.loanId());
        // TODO: skip if processed_event exists, else mark copy AVAILABLE, increase available_copies,
        //       then save processed_event - all in one transaction
    }
}
