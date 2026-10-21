package com.minicob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minicob.loan.InstallmentStatus;
import com.minicob.loan.Loan;
import com.minicob.loan.LoanStatus;
import com.minicob.loan.LoanTransactionType;
import com.minicob.support.IntegrationTestBase;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CobJobTest extends IntegrationTestBase {

    @Test
    void closingTheBusinessDayAccruesInterestFlagsOverdueAndAdvancesTheDate() throws Exception {
        Loan late = loans.loan().principal("120000").disbursedOn(LocalDate.of(2026, 3, 10)).create();
        Loan current = loans.loan().principal("100000").disbursedOn(LocalDate.of(2026, 4, 1)).create();

        mvc.perform(post("/api/cob/close-business-day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.processed").value(2));
        getJson("/api/business-date").andExpect(jsonPath("$.businessDate").value("2026-04-17"));

        Loan lateAfter = reload(late.getId());
        assertThat(lateAfter.getStatus()).isEqualTo(LoanStatus.OVERDUE);
        assertThat(lateAfter.getInstallments().get(0).getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
        assertThat(lateAfter.getAccruedInterest()).isEqualByComparingTo("24");
        assertThat(lateAfter.getAccruedPenalty()).isEqualByComparingTo("8");
        assertThat(lateAfter.getLastClosedBusinessDate()).isEqualTo(BUSINESS_DATE);

        Loan currentAfter = reload(current.getId());
        assertThat(currentAfter.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(currentAfter.getAccruedInterest()).isEqualByComparingTo("20");
        assertThat(currentAfter.getAccruedPenalty()).isEqualByComparingTo("0");

        assertThat(transactionRepository.findByLoanIdOrderById(late.getId()))
                .extracting(t -> t.getType())
                .containsExactly(LoanTransactionType.INTEREST_ACCRUAL, LoanTransactionType.PENALTY_ACCRUAL);
        assertThat(lockRepository.count()).isZero();
    }

    @Test
    void loansDisbursedAfterTheCobDateAreLeftAlone() {
        Loan future = loans.loan().disbursedOn(BUSINESS_DATE.plusDays(1)).create();

        var result = cobService.runCob(BUSINESS_DATE);

        assertThat(result.completed()).isTrue();
        assertThat(reload(future.getId()).getLastClosedBusinessDate()).isNull();
    }
}
