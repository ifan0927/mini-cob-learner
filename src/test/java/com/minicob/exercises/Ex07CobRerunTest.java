package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;

import com.minicob.loan.Loan;
import com.minicob.loan.LoanTransactionType;
import com.minicob.support.IntegrationTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exercise 07 - reported by operations: after re-running COB for a date that had already been
 * closed (standard procedure after a partial failure), customers were charged a second day of
 * interest.
 */
class Ex07CobRerunTest extends IntegrationTestBase {

    @Test
    void rerunningCobForAClosedDateDoesNotChargeAnotherDay() {
        List<Loan> booked = loans.loan().create(3);

        assertThat(cobService.runCob(BUSINESS_DATE).completed()).isTrue();
        assertThat(cobService.runCob(BUSINESS_DATE).completed()).isTrue();

        for (Loan loan : booked) {
            assertThat(reload(loan.getId()).getAccruedInterest())
                    .as("interest accrued for one day on loan %d", loan.getId())
                    .isEqualByComparingTo("20");
            assertThat(transactionRepository.findByLoanIdOrderById(loan.getId()))
                    .as("interest accrual entries of loan %d", loan.getId())
                    .filteredOn(t -> t.getType() == LoanTransactionType.INTEREST_ACCRUAL)
                    .hasSize(1);
        }
    }
}
