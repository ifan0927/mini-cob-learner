package com.minicob.cob;

import com.minicob.loan.Loan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

/**
 * Records processing and writing failures on the affected loans' locks.
 */
@Component
public class CobItemErrorListener implements ItemProcessListener<Loan, ProcessedLoan>, ItemWriteListener<ProcessedLoan> {

    private static final Logger log = LoggerFactory.getLogger(CobItemErrorListener.class);

    private final LockErrorRecorder recorder;

    public CobItemErrorListener(LockErrorRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void onProcessError(Loan item, Exception e) {
        log.warn("COB processing failed for loan {}: {}", item.getId(), e.toString());
        recorder.recordError(item.getId(), "Loan " + item.getId() + " processing failed: " + e.getMessage(), e);
    }

    @Override
    public void onWriteError(Exception e, Chunk<? extends ProcessedLoan> items) {
        for (ProcessedLoan processed : items) {
            Long loanId = processed.loan().getId();
            log.warn("COB write failed for loan {}: {}", loanId, e.toString());
            recorder.recordError(loanId, "Loan " + loanId + " writing failed: " + e.getMessage(), e);
        }
    }
}
