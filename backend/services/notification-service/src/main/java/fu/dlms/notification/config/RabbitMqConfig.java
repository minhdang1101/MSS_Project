package fu.dlms.notification.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.Map;

/**
 * Cấu hình RabbitMQ của notification-service.
 *
 * <p>Đường đi bình thường:
 * <pre>
 *   loan-service ──(loan.due-soon / loan.overdue)──► exchange "library.events" ──► queue "notification.loan-reminders"
 * </pre>
 * Đường đi của message lỗi (Task N6):
 * <pre>
 *   queue "notification.loan-reminders" ──(bị reject)──► exchange "library.events.dlx"
 *                                       ──► queue "notification.loan-reminders.dlq"  (giữ lại để xem và xử lý tay)
 * </pre>
 */
@Configuration
public class RabbitMqConfig {

    /** Exchange chung của hệ thống, loan-service gửi sự kiện vào đây. */
    public static final String EXCHANGE = "library.events";

    public static final String LOAN_CREATED = "loan.created";
    public static final String LOAN_RETURNED = "loan.returned";
    public static final String LOAN_DUE_SOON = "loan.due-soon";
    public static final String LOAN_OVERDUE = "loan.overdue";

    /** Queue riêng của notification-service. */
    public static final String QUEUE = "notification.loan-reminders";

    /** Dead letter exchange (DLX): nơi RabbitMQ chuyển message bị reject tới, thay vì xóa. */
    public static final String DEAD_LETTER_EXCHANGE = "library.events.dlx";

    /** Dead letter queue (DLQ): giữ message lỗi để xem trên RabbitMQ UI, sửa nguyên nhân rồi gửi lại. */
    public static final String DEAD_LETTER_QUEUE = QUEUE + ".dlq";

    @Bean
    public TopicExchange libraryExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * Queue chính. Hai tham số x-dead-letter-* bảo RabbitMQ: message nào bị reject (không requeue) thì chuyển sang
     * DEAD_LETTER_EXCHANGE với routing key DEAD_LETTER_QUEUE.
     *
     * <p>RabbitMQ KHÔNG cho đổi tham số của queue đã tồn tại. Nếu queue cũ (không có DLX) còn trên RabbitMQ, service
     * báo lỗi PRECONDITION_FAILED khi khởi động: xóa queue "notification.loan-reminders" trên UI rồi chạy lại.
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Binding notificationDueSoonBinding(Queue notificationQueue, TopicExchange libraryExchange) {
        return BindingBuilder.bind(notificationQueue).to(libraryExchange).with(LOAN_DUE_SOON);
    }

    @Bean
    public Binding notificationOverdueBinding(Queue notificationQueue, TopicExchange libraryExchange) {
        return BindingBuilder.bind(notificationQueue).to(libraryExchange).with(LOAN_OVERDUE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_QUEUE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Thay "nhà máy" listener mặc định của Spring Boot (cùng tên bean rabbitListenerContainerFactory) để thêm retry.
     *
     * <p>Khi hàm @RabbitListener ném exception:
     * <ol>
     *   <li>Lỗi có thể tự hết (database chập chờn, deadlock...): thử lại ngay trong service, tối đa
     *       {@code maxAttempts} lần, chờ 1s, 2s, 4s... giữa các lần.</li>
     *   <li>Lỗi chắc chắn không tự hết (message sai dữ liệu: AmqpRejectAndDontRequeueException; JSON hỏng:
     *       MessageConversionException): không thử lại.</li>
     *   <li>Hết lượt thử (hoặc lỗi không thử lại): RejectAndDontRequeueRecoverer reject message → RabbitMQ chuyển vào
     *       DLQ. Message không bị mất, cũng không bị gửi lại vô hạn.</li>
     * </ol>
     *
     * <p>configurer.configure(...) áp dụng các cấu hình spring.rabbitmq.listener.* và MessageConverter ở trên, giống
     * như factory mặc định; sau đó mới gắn thêm retry.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
            @Value("${app.notification.listener.max-attempts:3}") int maxAttempts) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(retryAdvice(maxAttempts));
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    static Advice retryAdvice(int maxAttempts) {
        // false = KHÔNG thử lại loại lỗi này. traverseCauses = true: Spring bọc lỗi trong
        // ListenerExecutionFailedException nên phải xét cả nguyên nhân bên trong.
        Map<Class<? extends Throwable>, Boolean> retryable = Map.of(
                AmqpRejectAndDontRequeueException.class, false,
                MessageConversionException.class, false);
        SimpleRetryPolicy policy = new SimpleRetryPolicy(maxAttempts, retryable, true, true);
        return RetryInterceptorBuilder.stateless()
                .retryPolicy(policy)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }
}
