package com.minicob.loan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "installment")
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Column(name = "installment_number", nullable = false)
    private int number;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_due", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalDue;

    @Column(name = "principal_paid", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstallmentStatus status = InstallmentStatus.PENDING;

    protected Installment() {
    }

    Installment(Loan loan, int number, LocalDate dueDate, BigDecimal principalDue) {
        this.loan = loan;
        this.number = number;
        this.dueDate = dueDate;
        this.principalDue = principalDue;
    }

    public BigDecimal remainingPrincipal() {
        return principalDue.subtract(principalPaid);
    }

    public boolean isPaid() {
        return status == InstallmentStatus.PAID;
    }

    /** An unpaid installment whose due date is before the day being closed. */
    public boolean isOverdueOn(LocalDate cobDate) {
        return !isPaid() && dueDate.isBefore(cobDate);
    }

    void markOverdue() {
        if (!isPaid()) {
            status = InstallmentStatus.OVERDUE;
        }
    }

    /** Applies up to {@code amount} to this installment and returns the part actually used. */
    BigDecimal pay(BigDecimal amount) {
        BigDecimal used = amount.min(remainingPrincipal());
        principalPaid = principalPaid.add(used);
        if (remainingPrincipal().signum() == 0) {
            status = InstallmentStatus.PAID;
        }
        return used;
    }

    public Long getId() {
        return id;
    }

    public Loan getLoan() {
        return loan;
    }

    public int getNumber() {
        return number;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getPrincipalDue() {
        return principalDue;
    }

    public BigDecimal getPrincipalPaid() {
        return principalPaid;
    }

    public InstallmentStatus getStatus() {
        return status;
    }
}
