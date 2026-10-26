package fu.dlms.catalog.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "library.events";

    public static final String LOAN_CREATED = "loan.created";
    public static final String LOAN_RETURNED = "loan.returned";
    public static final String LOAN_DUE_SOON = "loan.due-soon";
    public static final String LOAN_OVERDUE = "loan.overdue";

    @Bean
    public TopicExchange libraryExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
