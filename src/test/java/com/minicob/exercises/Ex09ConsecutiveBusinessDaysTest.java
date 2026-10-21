package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;

import com.minicob.loan.InstallmentStatus;
import com.minicob.loan.Loan;
import com.minicob.loan.LoanStatus;
import com.minicob.support.IntegrationTestBase;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Exercise 09 - reported by collections: when COB runs on two consecutive nights without an
 * application restart in between, the second night behaves as if it were still the first night.
 * An installment missed on its due date is not flagged overdue, and the loan's "last closed
 * business date" does not move. Production runs COB with a single worker thread.
 */
@TestPropertySource(properties = "mini-cob.cob.worker-threads=1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Ex09ConsecutiveBusinessDaysTest extends IntegrationTestBase {

    private static final LocalDate DAY_ONE = LocalDate.of(2026, 4, 15);
    private static final LocalDate DAY_TWO = LocalDate.of(2026, 4, 16);

    @Test
    void secondNightClosesTheSecondDay() {
        jdbc.update("update business_date set value_date = ? where date_type = 'BUSINESS_DATE'", DAY_ONE);
        // First installment (40,000) is due on DAY_ONE.
        Loan loan = loans.loan().principal("120000").disbursedOn(LocalDate.of(2026, 3, 15)).create();

        assertThat(cobService.closeBusinessDay().completed()).isTrue();
        Loan afterDayOne = reload(loan.getId());
        assertThat(afterDayOne.getLastClosedBusinessDate()).as("last closed date after night one").isEqualTo(DAY_ONE);
        assertThat(afterDayOne.getStatus()).as("status after night one (installment due today)").isEqualTo(LoanStatus.ACTIVE);
        assertThat(afterDayOne.getAccruedInterest()).isEqualByComparingTo("24");

        assertThat(cobService.closeBusinessDay().completed()).isTrue();
        Loan afterDayTwo = reload(loan.getId());
        assertThat(afterDayTwo.getLastClosedBusinessDate()).as("last closed date after night two").isEqualTo(DAY_TWO);
        assertThat(afterDayTwo.getInstallments().get(0).getStatus()).as("installment missed on DAY_ONE")
                .isEqualTo(InstallmentStatus.OVERDUE);
        assertThat(afterDayTwo.getStatus()).isEqualTo(LoanStatus.OVERDUE);
        assertThat(afterDayTwo.getAccruedInterest()).isEqualByComparingTo("48");
        assertThat(afterDayTwo.getAccruedPenalty()).as("penalty on 40,000 overdue for one day").isEqualByComparingTo("8");
    }
}
