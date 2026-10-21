package com.minicob.repayment;

public class LoanLockedException extends RuntimeException {

    public LoanLockedException(Long loanId) {
        super("Loan " + loanId + " is locked by close-of-business processing; try again later");
    }
}
