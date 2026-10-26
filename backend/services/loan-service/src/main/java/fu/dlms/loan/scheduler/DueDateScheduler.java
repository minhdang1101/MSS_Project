package fu.dlms.loan.scheduler;

import fu.dlms.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily scan that emits LoanDueSoon / LoanOverdue events through the outbox. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DueDateScheduler {

    private final LoanRepository loanRepository;

    @Scheduled(cron = "${app.loan.due-scan-cron:0 0 8 * * *}")
    public void scanDueDates() {
        // TODO: BORROWED loans with dueAt within N days -> OutboxEvent(LOAN_DUE_SOON)
        // TODO: BORROWED loans with dueAt < now -> status OVERDUE + OutboxEvent(LOAN_OVERDUE)
    }
}
