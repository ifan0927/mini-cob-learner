package com.minicob.loan;

import java.math.BigDecimal;

public class RepaymentExceedsOutstandingException extends RuntimeException {

    public RepaymentExceedsOutstandingException(Long loanId, BigDecimal amount, BigDecimal outstanding) {
        super("Repayment of " + amount.toPlainString() + " exceeds the total outstanding " + outstanding.toPlainString()
                + " of loan " + loanId);
    }
}
