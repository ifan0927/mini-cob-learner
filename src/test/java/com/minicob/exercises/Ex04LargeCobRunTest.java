package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;

import com.minicob.support.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exercise 04 - reported by finance: after a night with a few hundred active loans, some loans were
 * never closed for the day while others were charged twice. A test environment with a handful of
 * loans always looks fine.
 */
class Ex04LargeCobRunTest extends IntegrationTestBase {

    @Test
    void everyLoanIsClosedExactlyOnceInALargeRun() {
        loans.loan().rate("0.073").create(200);   // 100,000 x 7.3% / 365 = 20 per day
        loans.loan().rate("0.1095").create(200);  // 100,000 x 10.95% / 365 = 30 per day

        cobService.runCob(BUSINESS_DATE);

        List<String> wrong = jdbc.query("""
                select l.id, l.annual_interest_rate, l.accrued_interest, l.last_closed_business_date,
                       (select count(*) from loan_transaction t
                         where t.loan_id = l.id and t.type = 'INTEREST_ACCRUAL') as accruals
                from loan l order by l.id
                """, (rs, n) -> {
            BigDecimal expected = rs.getBigDecimal("annual_interest_rate").compareTo(new BigDecimal("0.073")) == 0
                    ? new BigDecimal("20") : new BigDecimal("30");
            LocalDate closed = rs.getObject("last_closed_business_date", LocalDate.class);
            boolean ok = BUSINESS_DATE.equals(closed)
                    && rs.getBigDecimal("accrued_interest").compareTo(expected) == 0
                    && rs.getInt("accruals") == 1;
            return ok ? null : "loan %d: closed=%s interest=%s (expected %s) accrual entries=%d".formatted(
                    rs.getLong("id"), closed, rs.getBigDecimal("accrued_interest").toPlainString(),
                    expected.toPlainString(), rs.getInt("accruals"));
        }).stream().filter(java.util.Objects::nonNull).toList();

        assertThat(wrong)
                .as("loans not closed exactly once for %s (%d of 400)", BUSINESS_DATE, wrong.size())
                .isEmpty();
    }
}
