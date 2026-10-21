package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;

import com.minicob.cob.CobService.CobRunResult;
import com.minicob.loan.Loan;
import com.minicob.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * Exercise 03 - reported by operations: when COB cannot process a loan, the failure reason never
 * reaches the account lock table, so nobody can tell what went wrong.
 */
class Ex03CobFailureDiagnosticsTest extends IntegrationTestBase {

    @Test
    void loanThatCobCouldNotProcessKeepsTheReasonOnItsLock() {
        Loan healthy = loans.loan().create();
        Loan broken = loans.loan().create();
        jdbc.update("update loan set currency = 'XXX' where id = ?", broken.getId());

        CobRunResult result = cobService.runCob(BUSINESS_DATE);

        assertThat(result.completed()).as("COB status (one bad loan should only be skipped)").isTrue();
        assertThat(reload(healthy.getId()).getLastClosedBusinessDate()).isEqualTo(BUSINESS_DATE);
        assertThat(lockOf(broken.getId()))
                .as("account lock of the loan COB could not process")
                .hasValueSatisfying(lock -> {
                    assertThat(lock.getErrorMessage()).contains("Unsupported currency: XXX");
                    assertThat(lock.getErrorStacktrace()).isNotBlank();
                });
    }
}
