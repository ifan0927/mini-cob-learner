package com.minicob.loan;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan")
public class Loan {

    static final int RATE_SCALE = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "account_no", unique = true, length = 20)
    private String accountNo;

    @Column(name = "customer_id", nullable = false, length = 40)
    private String customerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;

    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(name = "annual_interest_rate", nullable = false, precision = 9, scale = RATE_SCALE)
    private BigDecimal annualInterestRate;

    @Column(name = "accrued_interest", nullable = false, precision = 19, scale = 2)
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    @Column(name = "accrued_penalty", nullable = false, precision = 19, scale = 2)
    private BigDecimal accruedPenalty = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(name = "disbursement_date", nullable = false)
    private LocalDate disbursementDate;

    @Column(name = "last_closed_business_date")
    private LocalDate lastClosedBusinessDate;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    private List<Installment> installments = new ArrayList<>();

    protected Loan() {
    }

    /**
     * Books a new loan with an equal-principal monthly schedule. The last installment absorbs the
     * rounding remainder.
     */
    public static Loan disburse(String customerId, String currency, BigDecimal principal, BigDecimal annualInterestRate,
                                int termMonths, LocalDate disbursementDate) {
        int scale = Currencies.scaleOf(currency);
        if (principal.signum() <= 0 || principal.stripTrailingZeros().scale() > scale) {
            throw new IllegalArgumentException("Principal must be positive with at most " + scale + " decimals for " + currency);
        }
        if (annualInterestRate.signum() < 0) {
            throw new IllegalArgumentException("Interest rate must not be negative");
        }
        if (termMonths < 1) {
            throw new IllegalArgumentException("Term must be at least one month");
        }
        Loan loan = new Loan();
        loan.customerId = customerId;
        loan.currency = currency;
        loan.principal = principal.setScale(2, RoundingMode.UNNECESSARY);
        loan.outstandingPrincipal = loan.principal;
        loan.annualInterestRate = annualInterestRate.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        loan.disbursementDate = disbursementDate;

        BigDecimal regular = principal.divide(BigDecimal.valueOf(termMonths), scale, RoundingMode.DOWN);
        BigDecimal scheduled = BigDecimal.ZERO;
        for (int n = 1; n <= termMonths; n++) {
            BigDecimal due = n < termMonths ? regular : principal.subtract(scheduled);
            scheduled = scheduled.add(due);
            loan.installments.add(new Installment(loan, n, disbursementDate.plusMonths(n), due.setScale(2, RoundingMode.UNNECESSARY)));
        }
        return loan;
    }

    public void assignAccountNumber(String accountNo) {
        if (this.accountNo != null) {
            throw new IllegalStateException("Loan " + id + " already has account number " + this.accountNo);
        }
        this.accountNo = accountNo;
    }

    /** The schedule must add up to the booked principal. */
    public boolean scheduleMatchesPrincipal() {
        BigDecimal scheduled = installments.stream().map(Installment::getPrincipalDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return scheduled.compareTo(principal) == 0;
    }

    public BigDecimal overduePrincipal(LocalDate cobDate) {
        return installments.stream()
                .filter(i -> i.isOverdueOn(cobDate))
                .map(Installment::remainingPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Flags unpaid installments due before {@code cobDate} and derives the loan status from them. */
    public void refreshDelinquency(LocalDate cobDate) {
        if (status == LoanStatus.CLOSED) {
            return;
        }
        boolean anyOverdue = false;
        for (Installment installment : installments) {
            if (installment.isOverdueOn(cobDate)) {
                installment.markOverdue();
                anyOverdue = true;
            }
        }
        status = anyOverdue ? LoanStatus.OVERDUE : LoanStatus.ACTIVE;
    }

    public void accrueInterest(BigDecimal amount) {
        accruedInterest = accruedInterest.add(amount);
    }

    public void accruePenalty(BigDecimal amount) {
        accruedPenalty = accruedPenalty.add(amount);
    }

    public void markBusinessDayClosed(LocalDate cobDate) {
        lastClosedBusinessDate = cobDate;
    }

    public BigDecimal totalOutstanding() {
        return accruedPenalty.add(accruedInterest).add(outstandingPrincipal);
    }

    /**
     * Allocates a repayment to penalty, then interest, then principal of the oldest installments.
     *
     * @throws RepaymentExceedsOutstandingException when the amount is larger than everything owed
     */
    public RepaymentAllocation applyRepayment(BigDecimal amount) {
        if (amount.compareTo(totalOutstanding()) > 0) {
            throw new RepaymentExceedsOutstandingException(id, amount, totalOutstanding());
        }
        BigDecimal remaining = amount;

        BigDecimal penaltyPortion = remaining.min(accruedPenalty);
        accruedPenalty = accruedPenalty.subtract(penaltyPortion);
        remaining = remaining.subtract(penaltyPortion);

        BigDecimal interestPortion = remaining.min(accruedInterest);
        accruedInterest = accruedInterest.subtract(interestPortion);
        remaining = remaining.subtract(interestPortion);

        BigDecimal principalPortion = BigDecimal.ZERO;
        for (Installment installment : installments) {
            if (remaining.signum() == 0) {
                break;
            }
            if (!installment.isPaid()) {
                BigDecimal used = installment.pay(remaining);
                principalPortion = principalPortion.add(used);
                remaining = remaining.subtract(used);
            }
        }
        outstandingPrincipal = outstandingPrincipal.subtract(principalPortion);

        if (outstandingPrincipal.signum() == 0 && accruedInterest.signum() == 0 && accruedPenalty.signum() == 0) {
            status = LoanStatus.CLOSED;
        } else if (status == LoanStatus.OVERDUE && installments.stream().noneMatch(i -> i.getStatus() == InstallmentStatus.OVERDUE)) {
            status = LoanStatus.ACTIVE;
        }
        return new RepaymentAllocation(principalPortion, interestPortion, penaltyPortion);
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public BigDecimal getOutstandingPrincipal() {
        return outstandingPrincipal;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public BigDecimal getAccruedInterest() {
        return accruedInterest;
    }

    public BigDecimal getAccruedPenalty() {
        return accruedPenalty;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    public LocalDate getLastClosedBusinessDate() {
        return lastClosedBusinessDate;
    }

    public List<Installment> getInstallments() {
        return installments;
    }
}
