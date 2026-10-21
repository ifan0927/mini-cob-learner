package com.minicob.cob.step;

import com.minicob.cob.LoanDataCorruptedException;
import com.minicob.loan.Loan;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class ScheduleIntegrityCheckStep implements LoanCobBusinessStep {

    @Override
    public void execute(Loan loan, CobStepContext context) {
        if (!loan.scheduleMatchesPrincipal()) {
            throw new LoanDataCorruptedException("Repayment schedule of loan " + loan.getId()
                    + " does not add up to its principal " + loan.getPrincipal().toPlainString());
        }
    }
}
