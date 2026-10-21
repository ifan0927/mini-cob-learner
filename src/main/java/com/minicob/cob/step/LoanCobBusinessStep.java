package com.minicob.cob.step;

import com.minicob.loan.Loan;

/**
 * One unit of end-of-day work on a loan. Steps run in {@link org.springframework.core.annotation.Order}
 * order inside the chunk transaction.
 */
public interface LoanCobBusinessStep {

    void execute(Loan loan, CobStepContext context);
}
