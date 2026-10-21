package com.minicob.loan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "loan_transaction")
public class LoanTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanTransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LoanTransaction() {
    }

    private LoanTransaction(Long loanId, LoanTransactionType type, BigDecimal amount, LocalDate transactionDate) {
        this.loanId = loanId;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.createdAt = Instant.now();
    }

    public static LoanTransaction interestAccrual(Long loanId, BigDecimal amount, LocalDate date) {
        return new LoanTransaction(loanId, LoanTransactionType.INTEREST_ACCRUAL, amount, date);
    }

    public static LoanTransaction penaltyAccrual(Long loanId, BigDecimal amount, LocalDate date) {
        return new LoanTransaction(loanId, LoanTransactionType.PENALTY_ACCRUAL, amount, date);
    }

    public static LoanTransaction repayment(Long loanId, BigDecimal amount, LocalDate date) {
        return new LoanTransaction(loanId, LoanTransactionType.REPAYMENT, amount, date);
    }

    public Long getId() {
        return id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public LoanTransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }
}
