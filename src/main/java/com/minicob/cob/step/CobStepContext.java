package com.minicob.cob.step;

import com.minicob.loan.LoanTransaction;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Per-loan state shared by the business steps of one COB pass. */
public final class CobStepContext {

    private final LocalDate cobDate;
    private final List<LoanTransaction> newTransactions = new ArrayList<>();

    public CobStepContext(LocalDate cobDate) {
        this.cobDate = cobDate;
    }

    public LocalDate cobDate() {
        return cobDate;
    }

    public void record(LoanTransaction transaction) {
        newTransactions.add(transaction);
    }

    public List<LoanTransaction> newTransactions() {
        return List.copyOf(newTransactions);
    }
}
