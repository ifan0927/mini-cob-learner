package com.minicob.cob.step;

import com.minicob.config.MiniCobProperties;
import com.minicob.loan.InterestCalculator;
import com.minicob.loan.Loan;
import com.minicob.loan.LoanTransaction;
import java.math.BigDecimal;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Charges penalty interest on overdue principal. */
@Component
@Order(30)
public class PenaltyAccrualStep implements LoanCobBusinessStep {

    private final InterestCalculator calculator;
    private final MiniCobProperties properties;

    public PenaltyAccrualStep(InterestCalculator calculator, MiniCobProperties properties) {
        this.calculator = calculator;
        this.properties = properties;
    }

    @Override
    public void execute(Loan loan, CobStepContext context) {
        BigDecimal overdue = loan.overduePrincipal(context.cobDate());
        if (overdue.signum() == 0) {
            return;
        }
        BigDecimal penalty = calculator.dailyInterest(overdue, properties.penaltyAnnualRate(), loan.getCurrency());
        if (penalty.signum() > 0) {
            loan.accruePenalty(penalty);
            context.record(LoanTransaction.penaltyAccrual(loan.getId(), penalty, context.cobDate()));
        }
    }
}
