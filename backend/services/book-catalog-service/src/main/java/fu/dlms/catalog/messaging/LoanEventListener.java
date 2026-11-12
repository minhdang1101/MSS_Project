package fu.dlms.catalog.messaging;

import fu.dlms.catalog.config.RabbitMqConfig;
import fu.dlms.catalog.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LoanEventListener {

    public static final String QUEUE = "catalog.loan-returned";
    public static final String DEAD_LETTER_QUEUE = QUEUE + ".dlq";


    @Bean
    public Queue catalogLoanReturnedQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue catalogLoanReturnedDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE)
                .build();
    }


    @Bean
    public Binding catalogLoanReturnedBinding(Queue catalogLoanReturnedQueue, TopicExchange libraryExchange) {
        return BindingBuilder.bind(catalogLoanReturnedQueue).to(libraryExchange).with(RabbitMqConfig.LOAN_RETURNED);
    }

    private final ReservationService reservationService;

    @RabbitListener(queues = QUEUE)
    public void onLoanReturned(LoanEvent event) {
        log.info("Received {} for loan {}", event.eventType(), event.loanId());
        reservationService.onLoanReturned(event);
    }
}
