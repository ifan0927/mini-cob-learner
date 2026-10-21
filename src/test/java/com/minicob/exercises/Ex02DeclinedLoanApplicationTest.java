package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minicob.loan.Loan;
import com.minicob.support.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Exercise 02 - reported by credit risk: customers who were told their application exceeded the
 * exposure limit still end up with an extra loan on their books.
 */
class Ex02DeclinedLoanApplicationTest extends IntegrationTestBase {

    @Test
    void applicationDeclinedForExposureLimitDoesNotBookALoan() throws Exception {
        loans.loan().customer("C-EX02").principal("4000000").create();

        postJson("/api/loans", Map.of(
                "customerId", "C-EX02", "currency", "TWD", "principal", 2000000,
                "annualInterestRate", 0.03, "termMonths", 12))
                .andExpect(status().isUnprocessableEntity());

        assertThat(loanRepository.findByCustomerIdOrderById("C-EX02"))
                .as("loans on the books of customer C-EX02 after the declined application")
                .extracting(Loan::getPrincipal)
                .usingElementComparator(java.math.BigDecimal::compareTo)
                .containsExactly(new java.math.BigDecimal("4000000"));
    }
}
