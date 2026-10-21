package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;

import com.minicob.cob.CobService.CobRunResult;
import com.minicob.loan.Loan;
import com.minicob.support.IntegrationTestBase;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Exercise 05 - reported by operations: on nights when a branch posts a payment on a loan while COB
 * is working on it, that loan is not closed for the day, even though COB is configured to retry.
 */
class Ex05TellerDuringCobTest extends IntegrationTestBase {

    @Test
    void loanPaidAtTheBranchDuringCobIsStillClosedForTheDay() {
        Loan loan = loans.loan().principal("100000").create();
        tellerActivity.armFor(loan.getId(), new BigDecimal("10000"));

        CobRunResult result = cobService.runCob(BUSINESS_DATE);

        Loan after = reload(loan.getId());
        assertThat(after.getLastClosedBusinessDate()).as("last closed business date of the loan").isEqualTo(BUSINESS_DATE);
        assertThat(after.getOutstandingPrincipal()).as("outstanding principal (branch payment kept)").isEqualByComparingTo("90000");
        assertThat(after.getAccruedInterest()).as("interest accrued on 90,000 at 7.3%").isEqualByComparingTo("18");
        assertThat(result.skipped()).as("loans skipped by COB").isZero();
    }
}
