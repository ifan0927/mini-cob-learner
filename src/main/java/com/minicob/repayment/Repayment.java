package com.minicob.repayment;

import com.minicob.loan.RepaymentAllocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "repayment")
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "principal_portion", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalPortion = BigDecimal.ZERO;

    @Column(name = "interest_portion", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestPortion = BigDecimal.ZERO;

    @Column(name = "penalty_portion", nullable = false, precision = 19, scale = 2)
    private BigDecimal penaltyPortion = BigDecimal.ZERO;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Repayment() {
    }

    public static Repayment received(Long loanId, String idempotencyKey, BigDecimal amount, LocalDate transactionDate) {
        Repayment repayment = new Repayment();
        repayment.loanId = loanId;
        repayment.idempotencyKey = idempotencyKey;
        repayment.amount = amount;
        repayment.transactionDate = transactionDate;
        repayment.createdAt = Instant.now();
        return repayment;
    }

    public void recordAllocation(RepaymentAllocation allocation) {
        this.principalPortion = allocation.principalPortion();
        this.interestPortion = allocation.interestPortion();
        this.penaltyPortion = allocation.penaltyPortion();
    }

    public boolean sameRequestAs(Long loanId, BigDecimal amount) {
        return this.loanId.equals(loanId) && this.amount.compareTo(amount) == 0;
    }

    public Long getId() {
        return id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPrincipalPortion() {
        return principalPortion;
    }

    public BigDecimal getInterestPortion() {
        return interestPortion;
    }

    public BigDecimal getPenaltyPortion() {
        return penaltyPortion;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }
}
