package fu.dlms.loan.service.impl;

import fu.dlms.loan.client.BookCatalogClient;
import fu.dlms.loan.client.MemberClient;
import fu.dlms.loan.dto.BorrowRequest;
import fu.dlms.loan.dto.LoanResponse;
import fu.dlms.loan.exception.ResourceNotFoundException;
import fu.dlms.loan.repository.LoanRepository;
import fu.dlms.loan.repository.OutboxEventRepository;
import fu.dlms.loan.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final MemberClient memberClient;
    private final BookCatalogClient bookCatalogClient;

    @Override
    public List<LoanResponse> findAll() {
        return loanRepository.findAll().stream().map(LoanResponse::from).toList();
    }

    @Override
    public LoanResponse findById(Long id) {
        return loanRepository.findById(id)
                .map(LoanResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }

    @Override
    @Transactional
    public LoanResponse borrow(BorrowRequest request) {
        // TODO (Borrow Book flow):
        //  1. Return existing loan if idempotencyKey was already used
        //  2. memberClient.getMember -> must be ACTIVE
        //  3. bookCatalogClient -> check & reserve availability
        //  4. Save Loan (BORROWED, dueAt = borrowedAt + loan period)
        //  5. Save OutboxEvent(LOAN_CREATED) in the same transaction
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    @Transactional
    public LoanResponse returnBook(Long loanId) {
        // TODO (Return Book flow):
        //  1. Load loan, reject if already RETURNED (@Version guards concurrent returns)
        //  2. Set returnedAt, status = RETURNED
        //  3. Save OutboxEvent(LOAN_RETURNED) -> catalog frees the copy, fine-service calculates fine
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
