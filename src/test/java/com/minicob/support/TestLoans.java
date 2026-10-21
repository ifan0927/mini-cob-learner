package com.minicob.support;

import com.minicob.loan.Loan;
import com.minicob.loan.LoanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;

/** Books loans directly through the domain model, bypassing the API. */
public class TestLoans {

    private static final AtomicLong ACCOUNT_NUMBERS = new AtomicLong(90_000_000L);

    private final LoanRepository loanRepository;
    private final JdbcTemplate jdbc;

    public TestLoans(LoanRepository loanRepository, JdbcTemplate jdbc) {
        this.loanRepository = loanRepository;
        this.jdbc = jdbc;
    }

    public Builder loan() {
        return new Builder();
    }

    public final class Builder {
        private String customerId = "CUST-" + ACCOUNT_NUMBERS.get();
        private String currency = "TWD";
        private BigDecimal principal = new BigDecimal("100000");
        private BigDecimal rate = new BigDecimal("0.073");
        private int termMonths = 3;
        private LocalDate disbursedOn = LocalDate.of(2026, 4, 1);

        public Builder customer(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder principal(String principal) {
            this.principal = new BigDecimal(principal);
            return this;
        }

        public Builder rate(String rate) {
            this.rate = new BigDecimal(rate);
            return this;
        }

        public Builder term(int months) {
            this.termMonths = months;
            return this;
        }

        public Builder disbursedOn(LocalDate date) {
            this.disbursedOn = date;
            return this;
        }

        private Loan build() {
            Loan loan = Loan.disburse(customerId, currency, principal, rate, termMonths, disbursedOn);
            loan.assignAccountNumber("TS%08d".formatted(ACCOUNT_NUMBERS.incrementAndGet()));
            return loan;
        }

        public Loan create() {
            return loanRepository.save(build());
        }

        public List<Loan> create(int count) {
            List<Loan> loans = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                loans.add(build());
            }
            return loanRepository.saveAll(loans);
        }
    }

    /** Marks the given installment and its loan as overdue, as an earlier COB would have. */
    public void markOverdue(Long loanId, int installmentNumber) {
        jdbc.update("update installment set status = 'OVERDUE' where loan_id = ? and installment_number = ?",
                loanId, installmentNumber);
        jdbc.update("update loan set status = 'OVERDUE', version = version + 1 where id = ?", loanId);
    }

    public void setAccruedInterest(Long loanId, BigDecimal interest, BigDecimal penalty) {
        jdbc.update("update loan set accrued_interest = ?, accrued_penalty = ?, version = version + 1 where id = ?",
                interest, penalty, loanId);
    }
}
