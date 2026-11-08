package fu.dlms.loan.scheduler;

import fu.dlms.loan.entity.Loan;
import fu.dlms.loan.entity.LoanStatus;
import fu.dlms.loan.messaging.LoanEventType;
import fu.dlms.loan.messaging.OutboxWriter;
import fu.dlms.loan.repository.LoanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Daily scan (08:00 by default) that emits LoanDueSoon / LoanOverdue through the outbox.
 * <ul>
 *   <li>BORROWED loans due within app.loan.due-soon-days: LoanDueSoon, once per loan.</li>
 *   <li>BORROWED loans past their due time: status OVERDUE + LoanOverdue, once (the status changes).</li>
 * </ul>
 * Each loan is handled in its own transaction: a loan returned at the same moment (version conflict) is skipped
 * without undoing the others.
 */
@Slf4j
@Component
public class DueDateScheduler {

    private final LoanRepository loanRepository;
    private final OutboxWriter outboxWriter;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final int dueSoonDays;

    public DueDateScheduler(LoanRepository loanRepository, OutboxWriter outboxWriter,
                            TransactionTemplate transactionTemplate, Clock clock,
                            @Value("${app.loan.due-soon-days:2}") int dueSoonDays) {
        this.loanRepository = loanRepository;
        this.outboxWriter = outboxWriter;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.dueSoonDays = dueSoonDays;
    }

    @Scheduled(cron = "${app.loan.due-scan-cron:0 0 8 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void scanDueDates() {
        Instant now = clock.instant();
        int overdue = markOverdue(now);
        int dueSoon = remindDueSoon(now);
        log.info("Due date scan: {} loan(s) became overdue, {} due-soon reminder(s)", overdue, dueSoon);
    }

    int markOverdue(Instant now) {
        List<Long> ids = loanRepository.findIdsByStatusAndDueAtBefore(LoanStatus.BORROWED, now);
        return (int) ids.stream().filter(id -> update(id, loan -> {
            loan.setStatus(LoanStatus.OVERDUE);
            return LoanEventType.LOAN_OVERDUE;
        })).count();
    }

    int remindDueSoon(Instant now) {
        Instant until = now.plus(Duration.ofDays(dueSoonDays));
        List<Long> ids = loanRepository.findIdsDueSoonNotYetNotified(LoanStatus.BORROWED, now, until);
        return (int) ids.stream().filter(id -> update(id, loan -> {
            loan.setDueSoonNotifiedAt(now);
            return LoanEventType.LOAN_DUE_SOON;
        })).count();
    }

    /** Re-reads the loan inside the transaction and skips it if it was returned since the query. */
    private boolean update(Long loanId, Function<Loan, LoanEventType> change) {
        try {
            Boolean done = transactionTemplate.execute(tx -> {
                Loan loan = loanRepository.findById(loanId).orElse(null);
                if (loan == null || loan.getStatus() != LoanStatus.BORROWED) {
                    return false;
                }
                LoanEventType type = change.apply(loan);
                loanRepository.saveAndFlush(loan);
                outboxWriter.append(loan, type);
                return true;
            });
            return Boolean.TRUE.equals(done);
        } catch (OptimisticLockingFailureException e) {
            log.info("Loan {} changed during the scan (probably returned); skipped", loanId);
            return false;
        } catch (RuntimeException e) {
            log.error("Due date scan failed for loan {}: {}", loanId, e.getMessage(), e);
            return false;
        }
    }
}
