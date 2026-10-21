package com.minicob.cob.step;

import com.minicob.loan.Loan;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class DelinquencyStep implements LoanCobBusinessStep {

    @Override
    public void execute(Loan loan, CobStepContext context) {
        loan.refreshDelinquency(context.cobDate());
    }
}
