package com.minicob.cob;

import com.minicob.loan.Loan;
import com.minicob.loan.LoanTransaction;
import java.util.List;

/** A loan after its COB business steps, plus the transactions they produced. */
public record ProcessedLoan(Loan loan, List<LoanTransaction> transactions) {
}
