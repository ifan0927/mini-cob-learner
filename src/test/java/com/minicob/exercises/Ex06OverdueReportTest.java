package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minicob.loan.Loan;
import com.minicob.support.IntegrationTestBase;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Exercise 06 - reported by the DBA: the overdue report endpoint gets slower every month, and one
 * call floods the database with statements.
 */
class Ex06OverdueReportTest extends IntegrationTestBase {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void overdueReportStatementCountDoesNotGrowWithTheNumberOfLoans() throws Exception {
        for (Loan loan : loans.loan().disbursedOn(LocalDate.of(2026, 1, 10)).create(20)) {
            loans.markOverdue(loan.getId(), 1);
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        getJson("/api/reports/overdue-loans")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20));

        assertThat(statistics.getPrepareStatementCount())
                .as("SQL statements executed by one overdue report call with 20 overdue loans")
                .isLessThanOrEqualTo(5);
    }
}
