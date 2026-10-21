package com.minicob.cob;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.domain.Persistable;

/**
 * Marks a loan as owned by COB. While the row exists, customer-facing writes are rejected. A lock
 * that carries an error belongs to a loan COB could not process; it stays until someone looks.
 */
@Entity
@Table(name = "account_lock")
public class AccountLock implements Persistable<Long> {

    static final int MAX_MESSAGE_LENGTH = 1000;
    static final int MAX_STACKTRACE_LENGTH = 4000;

    @Id
    @Column(name = "loan_id")
    private Long loanId;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "lock_owner", nullable = false, length = 30)
    private LockOwner owner;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    @Column(name = "cob_business_date", nullable = false)
    private LocalDate cobBusinessDate;

    @Column(name = "error_message", length = MAX_MESSAGE_LENGTH)
    private String errorMessage;

    @Column(name = "error_stacktrace", length = MAX_STACKTRACE_LENGTH)
    private String errorStacktrace;

    @Transient
    private boolean isNew = true;

    protected AccountLock() {
    }

    public AccountLock(Long loanId, LockOwner owner, LocalDate cobBusinessDate) {
        this.loanId = loanId;
        this.owner = owner;
        this.cobBusinessDate = cobBusinessDate;
        this.lockedAt = Instant.now();
    }

    /** A COB run takes over a lock left behind by an earlier run and gives the loan a fresh attempt. */
    public void takeOver(LocalDate cobBusinessDate) {
        this.cobBusinessDate = cobBusinessDate;
        this.lockedAt = Instant.now();
        this.errorMessage = null;
        this.errorStacktrace = null;
    }

    public void recordError(String message, String stacktrace) {
        this.errorMessage = truncate(message, MAX_MESSAGE_LENGTH);
        this.errorStacktrace = truncate(stacktrace, MAX_STACKTRACE_LENGTH);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public Long getId() {
        return loanId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public Long getLoanId() {
        return loanId;
    }

    public Long getVersion() {
        return version;
    }

    public LockOwner getOwner() {
        return owner;
    }

    public LocalDate getCobBusinessDate() {
        return cobBusinessDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorStacktrace() {
        return errorStacktrace;
    }
}
