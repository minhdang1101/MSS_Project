package fu.dlms.loan.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.dlms.loan.entity.Loan;
import fu.dlms.loan.entity.OutboxEvent;
import fu.dlms.loan.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Writes the event for a loan change into the outbox. MANDATORY: it must join the transaction that changes the
 * loan, so the change and its event are committed together or not at all. {@link OutboxRelay} publishes it later.
 */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent append(Loan loan, LoanEventType type) {
        String eventId = UUID.randomUUID().toString();
        LoanEvent event = new LoanEvent(eventId, type.eventName(), loan.getId(), loan.getMemberId(), loan.getBookId(),
                loan.getCopyId(), loan.getLoanType().name(), loan.getBorrowedAt(), loan.getDueAt(),
                loan.getReturnedAt(), clock.instant());

        OutboxEvent outbox = new OutboxEvent();
        outbox.setId(eventId);
        outbox.setAggregateId(loan.getId());
        outbox.setEventType(type.eventName());
        outbox.setPayload(toJson(event));
        return outboxEventRepository.save(outbox);
    }

    private String toJson(LoanEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + event.eventType() + " for loan " + event.loanId(), e);
        }
    }
}
