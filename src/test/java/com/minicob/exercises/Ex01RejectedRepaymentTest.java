package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minicob.loan.Loan;
import com.minicob.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * Exercise 01 - reported by customer service: a repayment the API rejected still shows up in the
 * loan's repayment history.
 */
class Ex01RejectedRepaymentTest extends IntegrationTestBase {

    @Test
    void rejectedOverpaymentDoesNotAppearInRepaymentHistory() throws Exception {
        Loan loan = loans.loan().principal("100000").create();

        repay(loan.getId(), "ex01-overpay", "100001").andExpect(status().isUnprocessableEntity());

        String history = getJson("/api/loans/{id}/repayments", loan.getId())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(history))
                .as("repayment history after the API answered 422 to the only repayment attempt")
                .isEmpty();
        assertThat(reload(loan.getId()).getOutstandingPrincipal()).isEqualByComparingTo("100000");
    }
}
