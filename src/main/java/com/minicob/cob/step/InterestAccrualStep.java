package com.minicob.cob.step;

import com.minicob.loan.InterestCalculator;
import com.minicob.loan.Loan;
import com.minicob.loan.LoanTransaction;
import java.math.BigDecimal;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class InterestAccrualStep implements LoanCobBusinessStep {

    private final InterestCalculator calculator;

    public InterestAccrualStep(InterestCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public void execute(Loan loan, CobStepContext context) {
        BigDecimal interest = calculator.dailyInterest(loan.getOutstandingPrincipal(), loan.getAnnualInterestRate(),
                loan.getCurrency());
        if (interest.signum() > 0) {
            loan.accrueInterest(interest);
            context.record(LoanTransaction.interestAccrual(loan.getId(), interest, context.cobDate()));
        }
    }
}
