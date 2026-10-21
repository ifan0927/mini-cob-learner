package com.minicob.cob;

import com.minicob.businessdate.BusinessDateService;
import com.minicob.context.BusinessDateContext;
import com.minicob.context.BusinessDates;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
public class CobService {

    public static final String COB_DATE_PARAMETER = "cobDate";
    public static final String RUN_ID_PARAMETER = "runId";
    public static final String MDC_RUN_ID = "cobRunId";

    private static final Logger log = LoggerFactory.getLogger(CobService.class);

    private final JobLauncher jobLauncher;
    private final Job loanCobJob;
    private final BusinessDateService businessDateService;

    public CobService(JobLauncher jobLauncher, Job loanCobJob, BusinessDateService businessDateService) {
        this.jobLauncher = jobLauncher;
        this.loanCobJob = loanCobJob;
        this.businessDateService = businessDateService;
    }

    public record CobRunResult(Long jobExecutionId, LocalDate cobDate, BatchStatus status, long processed, long skipped,
                               List<String> failures) {

        public boolean completed() {
            return status == BatchStatus.COMPLETED;
        }
    }

    /**
     * Closes the current business date and, if every loan was handled, opens the next one.
     */
    public CobRunResult closeBusinessDay() {
        LocalDate businessDate = businessDateService.getBusinessDate();
        CobRunResult result = runCob(businessDate);
        if (result.completed()) {
            businessDateService.advanceTo(businessDate.plusDays(1));
        }
        return result;
    }

    /**
     * Runs loan COB for {@code cobDate}. Running it again for the same date only picks up the loans
     * that are not closed for that date yet.
     */
    public CobRunResult runCob(LocalDate cobDate) {
        String runId = UUID.randomUUID().toString();
        BusinessDates previous = BusinessDateContext.getOrNull();
        BusinessDateContext.set(BusinessDates.closing(cobDate));
        MDC.put(MDC_RUN_ID, runId);
        try {
            JobParameters parameters = new JobParametersBuilder()
                    .addLocalDate(COB_DATE_PARAMETER, cobDate)
                    .addString(RUN_ID_PARAMETER, runId)
                    .toJobParameters();
            JobExecution execution = jobLauncher.run(loanCobJob, parameters);
            CobRunResult result = summarize(cobDate, execution);
            log.info("Loan COB for {} finished: {}", cobDate, result);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Could not launch loan COB for " + cobDate, e);
        } finally {
            MDC.remove(MDC_RUN_ID);
            BusinessDateContext.restore(previous);
        }
    }

    private static CobRunResult summarize(LocalDate cobDate, JobExecution execution) {
        List<StepExecution> workers = execution.getStepExecutions().stream()
                .filter(s -> s.getStepName().startsWith(CobJobConfiguration.WORKER_STEP_NAME))
                .toList();
        long processed = workers.stream().mapToLong(StepExecution::getWriteCount).sum();
        long skipped = workers.stream().mapToLong(StepExecution::getSkipCount).sum();
        List<String> failures = execution.getAllFailureExceptions().stream().map(Throwable::toString).toList();
        return new CobRunResult(execution.getId(), cobDate, execution.getStatus(), processed, skipped, failures);
    }
}
