package com.minicob.cob;

import com.minicob.cob.step.CobStepContext;
import com.minicob.cob.step.LoanCobBusinessStep;
import com.minicob.context.BusinessDateContext;
import com.minicob.loan.Loan;
import java.util.List;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Runs the configured business steps against one loan.
 */
@Component
public class LoanCobItemProcessor implements ItemProcessor<Loan, ProcessedLoan> {

    private final List<LoanCobBusinessStep> steps;

    public LoanCobItemProcessor(List<LoanCobBusinessStep> steps) {
        this.steps = steps;
    }

    @Override
    public ProcessedLoan process(Loan loan) {
        CobStepContext context = new CobStepContext(BusinessDateContext.getCobDate());
        for (LoanCobBusinessStep step : steps) {
            step.execute(loan, context);
        }
        loan.markBusinessDayClosed(context.cobDate());
        return new ProcessedLoan(loan, context.newTransactions());
    }
}
