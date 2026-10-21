package com.minicob.cob;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Locks a partition's loans before the worker step reads them, and afterwards releases every lock
 * the step did not need to keep. Locks with an error stay for operations to investigate.
 */
@Component
public class CobLockingStepListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(CobLockingStepListener.class);

    private final AccountLockService lockService;

    public CobLockingStepListener(AccountLockService lockService) {
        this.lockService = lockService;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        List<Long> loanIds = LoanCobPartitioner.loanIdsOf(stepExecution.getExecutionContext());
        LocalDate cobDate = stepExecution.getJobParameters().getLocalDate(CobService.COB_DATE_PARAMETER);
        lockService.lockForCob(loanIds, cobDate);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            List<Long> loanIds = LoanCobPartitioner.loanIdsOf(stepExecution.getExecutionContext());
            int released = lockService.releaseErrorFree(loanIds);
            log.debug("{} finished with {}; released {} remaining locks", stepExecution.getStepName(),
                    stepExecution.getStatus(), released);
        }
        return null;
    }
}
