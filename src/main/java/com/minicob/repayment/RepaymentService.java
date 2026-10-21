package com.minicob.repayment;

import com.minicob.cob.AccountLockService;
import com.minicob.context.BusinessDateContext;
import com.minicob.loan.Loan;
import com.minicob.loan.LoanNotFoundException;
import com.minicob.loan.LoanRepository;
import com.minicob.loan.LoanTransaction;
import com.minicob.loan.LoanTransactionRepository;
import com.minicob.loan.RepaymentAllocation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final AccountLockService accountLockService;

    public RepaymentService(RepaymentRepository repaymentRepository, LoanRepository loanRepository,
                            LoanTransactionRepository loanTransactionRepository, AccountLockService accountLockService) {
        this.repaymentRepository = repaymentRepository;
        this.loanRepository = loanRepository;
        this.loanTransactionRepository = loanTransactionRepository;
        this.accountLockService = accountLockService;
    }

    /**
     * Applies a repayment exactly once per idempotency key. A retried request with the same key and
     * payload gets the original result back.
     */
    public RepaymentResult repay(Long loanId, String idempotencyKey, BigDecimal amount) {
        Optional<Repayment> previous = repaymentRepository.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            if (!previous.get().sameRequestAs(loanId, amount)) {
                throw new IdempotencyKeyReusedException(idempotencyKey);
            }
            return RepaymentResult.of(previous.get(), true);
        }
        if (accountLockService.isLocked(loanId)) {
            throw new LoanLockedException(loanId);
        }
        return applyRepayment(loanId, idempotencyKey, amount);
    }

    @Transactional
    public RepaymentResult applyRepayment(Long loanId, String idempotencyKey, BigDecimal amount) {
        Loan loan = loanRepository.findWithInstallmentsById(loanId).orElseThrow(() -> new LoanNotFoundException(loanId));
        LocalDate businessDate = BusinessDateContext.getBusinessDate();

        // Persist the repayment first so the allocation below always belongs to a stored request.
        Repayment repayment = repaymentRepository.save(Repayment.received(loanId, idempotencyKey, amount, businessDate));
        RepaymentAllocation allocation = loan.applyRepayment(amount);
        repayment.recordAllocation(allocation);
        repayment = repaymentRepository.save(repayment);

        loanRepository.save(loan);
        loanTransactionRepository.save(LoanTransaction.repayment(loanId, amount, businessDate));
        return RepaymentResult.of(repayment, false);
    }

    @Transactional(readOnly = true)
    public List<RepaymentResult> history(Long loanId) {
        return repaymentRepository.findByLoanIdOrderById(loanId).stream()
                .map(r -> RepaymentResult.of(r, false))
                .toList();
    }
}
