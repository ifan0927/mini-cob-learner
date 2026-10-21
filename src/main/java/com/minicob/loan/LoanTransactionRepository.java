package com.minicob.loan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanTransactionRepository extends JpaRepository<LoanTransaction, Long> {

    List<LoanTransaction> findByLoanIdOrderById(Long loanId);
}
