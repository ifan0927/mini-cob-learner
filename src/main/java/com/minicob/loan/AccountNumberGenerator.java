package com.minicob.loan;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gap-free loan account numbers backed by a counter row.
 */
@Service
public class AccountNumberGenerator {

    static final String LOAN_ACCOUNT_SEQUENCE = "LOAN_ACCOUNT_NO";

    private final NumberSequenceRepository repository;

    public AccountNumberGenerator(NumberSequenceRepository repository) {
        this.repository = repository;
    }

    // Allocate in a short transaction of its own so the counter row lock is released
    // immediately instead of being held until the whole loan booking commits.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextLoanAccountNumber() {
        NumberSequence sequence = repository.findForUpdate(LOAN_ACCOUNT_SEQUENCE)
                .orElseThrow(() -> new IllegalStateException("Sequence " + LOAN_ACCOUNT_SEQUENCE + " is missing"));
        return "LN%08d".formatted(sequence.allocate());
    }

    @Transactional
    public void initializeIfMissing() {
        if (!repository.existsById(LOAN_ACCOUNT_SEQUENCE)) {
            repository.save(new NumberSequence(LOAN_ACCOUNT_SEQUENCE, 1));
        }
    }
}
