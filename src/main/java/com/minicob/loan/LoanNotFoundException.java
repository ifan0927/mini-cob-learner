package com.minicob.loan;

public class LoanNotFoundException extends RuntimeException {

    public LoanNotFoundException(Long loanId) {
        super("Loan " + loanId + " not found");
    }
}
