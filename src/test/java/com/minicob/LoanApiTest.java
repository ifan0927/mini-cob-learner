package com.minicob;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minicob.support.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LoanApiTest extends IntegrationTestBase {

    @Test
    void createsLoanWithMonthlyScheduleOnTheBusinessDate() throws Exception {
        postJson("/api/loans", Map.of(
                "customerId", "C-100", "currency", "TWD", "principal", 100000,
                "annualInterestRate", 0.025, "termMonths", 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNo").value(org.hamcrest.Matchers.matchesPattern("LN\\d{8}")))
                .andExpect(jsonPath("$.disbursementDate").value("2026-04-16"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.installments.length()").value(3))
                .andExpect(jsonPath("$.installments[0].dueDate").value("2026-05-16"))
                .andExpect(jsonPath("$.installments[0].principalDue").value(33333))
                .andExpect(jsonPath("$.installments[2].principalDue").value(33334));
    }

    @Test
    void consecutiveLoansGetDistinctAccountNumbers() throws Exception {
        String first = postJson("/api/loans", Map.of("customerId", "C-1", "currency", "USD", "principal", 1000,
                "annualInterestRate", 0.05, "termMonths", 2)).andReturn().getResponse().getContentAsString();
        String second = postJson("/api/loans", Map.of("customerId", "C-2", "currency", "USD", "principal", 1000,
                "annualInterestRate", 0.05, "termMonths", 2)).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(json.readTree(first).get("accountNo").asText())
                .isNotEqualTo(json.readTree(second).get("accountNo").asText());
    }

    @Test
    void rejectsUnsupportedCurrency() throws Exception {
        postJson("/api/loans", Map.of("customerId", "C-100", "currency", "EUR", "principal", 1000,
                "annualInterestRate", 0.05, "termMonths", 3))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownLoanIsNotFound() throws Exception {
        getJson("/api/loans/{id}", 999_999).andExpect(status().isNotFound());
    }
}
