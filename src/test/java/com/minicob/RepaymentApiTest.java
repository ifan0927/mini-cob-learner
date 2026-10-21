package com.minicob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minicob.cob.AccountLock;
import com.minicob.cob.LockOwner;
import com.minicob.loan.InstallmentStatus;
import com.minicob.loan.Loan;
import com.minicob.loan.LoanStatus;
import com.minicob.support.IntegrationTestBase;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RepaymentApiTest extends IntegrationTestBase {

    @Test
    void allocatesToPenaltyThenInterestThenOldestPrincipal() throws Exception {
        Loan loan = loans.loan().principal("120000").create();
        loans.setAccruedInterest(loan.getId(), new BigDecimal("24"), new BigDecimal("8"));

        repay(loan.getId(), "k-1", "1032")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.penaltyPortion").value(8))
                .andExpect(jsonPath("$.interestPortion").value(24))
                .andExpect(jsonPath("$.principalPortion").value(1000))
                .andExpect(jsonPath("$.replayed").value(false));

        Loan after = reload(loan.getId());
        assertThat(after.getOutstandingPrincipal()).isEqualByComparingTo("119000");
        assertThat(after.getAccruedInterest()).isEqualByComparingTo("0");
        assertThat(after.getAccruedPenalty()).isEqualByComparingTo("0");
        assertThat(after.getInstallments().get(0).getPrincipalPaid()).isEqualByComparingTo("1000");
    }

    @Test
    void retriedRequestWithSameKeyIsAppliedOnce() throws Exception {
        Loan loan = loans.loan().create();

        String first = repay(loan.getId(), "k-retry", "5000").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = repay(loan.getId(), "k-retry", "5000").andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(second).get("repaymentId")).isEqualTo(json.readTree(first).get("repaymentId"));
        assertThat(reload(loan.getId()).getOutstandingPrincipal()).isEqualByComparingTo("95000");
        assertThat(repaymentRepository.findByLoanIdOrderById(loan.getId())).hasSize(1);
    }

    @Test
    void sameKeyWithDifferentAmountIsRejected() throws Exception {
        Loan loan = loans.loan().create();
        repay(loan.getId(), "k-x", "5000").andExpect(status().isOk());
        repay(loan.getId(), "k-x", "6000").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void payingEverythingClosesTheLoan() throws Exception {
        Loan loan = loans.loan().principal("90000").create();
        repay(loan.getId(), "k-all", "90000").andExpect(status().isOk());

        Loan after = reload(loan.getId());
        assertThat(after.getStatus()).isEqualTo(LoanStatus.CLOSED);
        assertThat(after.getInstallments()).allMatch(i -> i.getStatus() == InstallmentStatus.PAID);
    }

    @Test
    void loanLockedByCobRejectsRepayment() throws Exception {
        Loan loan = loans.loan().create();
        lockRepository.save(new AccountLock(loan.getId(), LockOwner.LOAN_COB, BUSINESS_DATE));

        repay(loan.getId(), "k-locked", "1000").andExpect(status().isConflict());
        assertThat(reload(loan.getId()).getOutstandingPrincipal()).isEqualByComparingTo("100000");
    }
}
