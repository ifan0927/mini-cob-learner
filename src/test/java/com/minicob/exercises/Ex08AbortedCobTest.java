package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;

import com.minicob.cob.CobService.CobRunResult;
import com.minicob.loan.Loan;
import com.minicob.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;

/**
 * Exercise 08 - reported by the call center: the morning after COB aborted on one bad loan,
 * customers with perfectly healthy loans could not make repayments ("locked by close-of-business
 * processing") until someone cleaned the database by hand.
 */
class Ex08AbortedCobTest extends IntegrationTestBase {

    @Test
    void healthyLoansAcceptRepaymentsAfterAnAbortedCob() throws Exception {
        Loan broken = loans.loan().create();
        jdbc.update("update installment set principal_due = principal_due + 1 where loan_id = ? and installment_number = 1",
                broken.getId());
        List<Loan> healthy = loans.loan().create(7);

        CobRunResult result = cobService.runCob(BUSINESS_DATE);
        assertThat(result.status()).as("COB stops on corrupted loan data").isEqualTo(BatchStatus.FAILED);

        List<String> rejected = new ArrayList<>();
        for (Loan loan : healthy) {
            int status = repay(loan.getId(), "ex08-" + loan.getId(), "1000").andReturn().getResponse().getStatus();
            if (status != 200) {
                rejected.add("loan " + loan.getId() + " -> HTTP " + status);
            }
        }
        assertThat(rejected).as("repayments on healthy loans the morning after the aborted COB").isEmpty();
    }
}
