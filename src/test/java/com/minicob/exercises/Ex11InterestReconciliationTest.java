package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;

import com.minicob.loan.Loan;
import com.minicob.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * Exercise 11 - reported by finance: daily interest booked by COB on USD loans does not match the
 * reconciliation spreadsheet. The difference is a few cents per loan per day.
 */
class Ex11InterestReconciliationTest extends IntegrationTestBase {

    @Test
    void dailyInterestMatchesTheFinanceSpreadsheet() {
        // Spreadsheet: principal x annual rate / 365, rounded half-up to cents.
        Loan small = loans.loan().currency("USD").principal("250000.00").rate("0.03").create();    // 20.547945... -> 20.55
        Loan large = loans.loan().currency("USD").principal("1000000.00").rate("0.05").create();   // 136.986301... -> 136.99

        cobService.runCob(BUSINESS_DATE);

        assertThat(reload(small.getId()).getAccruedInterest()).as("one day of interest on 250,000.00 USD at 3%")
                .isEqualByComparingTo("20.55");
        assertThat(reload(large.getId()).getAccruedInterest()).as("one day of interest on 1,000,000.00 USD at 5%")
                .isEqualByComparingTo("136.99");
    }
}
