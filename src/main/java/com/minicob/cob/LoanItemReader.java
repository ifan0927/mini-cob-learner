package com.minicob.cob;

import com.minicob.loan.Loan;
import java.util.ArrayDeque;
import java.util.Deque;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;

/**
 * Reads the loans of one partition, one by one, inside the chunk transaction.
 */
public class LoanItemReader implements ItemStreamReader<Loan> {

    private final CobLoanRepository repository;
    private Deque<Long> remaining = new ArrayDeque<>();

    public LoanItemReader(CobLoanRepository repository) {
        this.repository = repository;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        remaining = new ArrayDeque<>(LoanCobPartitioner.loanIdsOf(executionContext));
    }

    @Override
    public Loan read() {
        Long loanId;
        while ((loanId = remaining.poll()) != null) {
            Loan loan = repository.findForCob(loanId).orElse(null);
            if (loan != null) {
                return loan;
            }
        }
        return null;
    }
}
