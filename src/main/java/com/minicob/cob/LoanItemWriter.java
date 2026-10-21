package com.minicob.cob;

import com.minicob.loan.Loan;
import com.minicob.loan.LoanRepository;
import com.minicob.loan.LoanTransactionRepository;
import java.util.List;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Persists processed loans and releases their locks in the same chunk transaction.
 */
@Component
public class LoanItemWriter implements ItemWriter<ProcessedLoan> {

    private final LoanRepository loanRepository;
    private final LoanTransactionRepository transactionRepository;
    private final AccountLockService lockService;

    public LoanItemWriter(LoanRepository loanRepository, LoanTransactionRepository transactionRepository,
                          AccountLockService lockService) {
        this.loanRepository = loanRepository;
        this.transactionRepository = transactionRepository;
        this.lockService = lockService;
    }

    @Override
    public void write(Chunk<? extends ProcessedLoan> chunk) {
        List<Loan> loans = chunk.getItems().stream().map(ProcessedLoan::loan).toList();
        loanRepository.saveAll(loans);
        transactionRepository.saveAll(chunk.getItems().stream().flatMap(p -> p.transactions().stream()).toList());
        // Flush inside the write phase so database errors go through the step's retry/skip policy.
        loanRepository.flush();
        lockService.release(loans.stream().map(Loan::getId).toList());
    }
}
