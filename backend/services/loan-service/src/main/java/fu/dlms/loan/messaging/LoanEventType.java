package fu.dlms.loan.messaging;

import fu.dlms.loan.config.RabbitMqConfig;

import java.util.Arrays;

/** The four events of the agreed contract. The name goes in the payload, the routing key picks the queues. */
public enum LoanEventType {
    LOAN_CREATED("LoanCreated", RabbitMqConfig.LOAN_CREATED),
    LOAN_RETURNED("LoanReturned", RabbitMqConfig.LOAN_RETURNED),
    LOAN_DUE_SOON("LoanDueSoon", RabbitMqConfig.LOAN_DUE_SOON),
    LOAN_OVERDUE("LoanOverdue", RabbitMqConfig.LOAN_OVERDUE);

    private final String eventName;
    private final String routingKey;

    LoanEventType(String eventName, String routingKey) {
        this.eventName = eventName;
        this.routingKey = routingKey;
    }

    public String eventName() {
        return eventName;
    }

    public String routingKey() {
        return routingKey;
    }

    public static LoanEventType fromEventName(String eventName) {
        return Arrays.stream(values())
                .filter(type -> type.eventName.equals(eventName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown loan event type: " + eventName));
    }
}
