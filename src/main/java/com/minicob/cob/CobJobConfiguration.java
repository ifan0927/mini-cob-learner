package com.minicob.cob;

import com.minicob.config.MiniCobProperties;
import com.minicob.context.CobTaskDecorator;
import com.minicob.loan.Loan;
import java.time.LocalDate;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Loan COB: a manager step partitions the loans to close and a pool of local worker threads runs
 * the chunk-oriented worker step for each partition (compare Fineract's LoanCOBManagerConfiguration
 * and LoanCOBWorkerConfiguration, which do the same with remote partitioning).
 */
@Configuration
public class CobJobConfiguration {

    public static final String JOB_NAME = "loanCobJob";
    public static final String WORKER_STEP_NAME = "loanCobWorkerStep";

    private final MiniCobProperties properties;

    public CobJobConfiguration(MiniCobProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ThreadPoolTaskExecutor cobTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.cob().workerThreads());
        executor.setMaxPoolSize(properties.cob().workerThreads());
        executor.setThreadNamePrefix("cob-worker-");
        executor.setTaskDecorator(new CobTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Bean
    public Job loanCobJob(JobRepository jobRepository, Step loanCobManagerStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(loanCobManagerStep)
                .build();
    }

    @Bean
    public Step loanCobManagerStep(JobRepository jobRepository, Step loanCobWorkerStep, LoanCobPartitioner partitioner,
                                   ThreadPoolTaskExecutor cobTaskExecutor) {
        return new StepBuilder("loanCobManagerStep", jobRepository)
                .partitioner(WORKER_STEP_NAME, partitioner)
                .step(loanCobWorkerStep)
                .gridSize(properties.cob().workerThreads())
                .taskExecutor(cobTaskExecutor)
                .build();
    }

    @Bean
    public Step loanCobWorkerStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                  LoanItemReader cobLoanReader, LoanCobItemProcessor processor, LoanItemWriter writer,
                                  CobItemErrorListener errorListener, CobLockingStepListener lockingListener) {
        MiniCobProperties.Cob cob = properties.cob();
        return new StepBuilder(WORKER_STEP_NAME, jobRepository)
                .<Loan, ProcessedLoan>chunk(cob.chunkSize(), transactionManager)
                .reader(cobLoanReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .retry(OptimisticLockingFailureException.class)
                .retryLimit(cob.retryLimit())
                .skip(Exception.class)
                .noSkip(LoanDataCorruptedException.class)
                .skipLimit(cob.skipLimit())
                .listener((ItemProcessListener<Loan, ProcessedLoan>) errorListener)
                .listener((ItemWriteListener<ProcessedLoan>) errorListener)
                .listener(lockingListener)
                .build();
    }

    @Bean
    public LoanItemReader cobLoanReader(CobLoanRepository repository) {
        return new LoanItemReader(repository);
    }

    @Bean
    @StepScope
    public LoanCobPartitioner loanCobPartitioner(CobLoanRepository repository,
                                                 @Value("#{jobParameters['" + CobService.COB_DATE_PARAMETER + "']}") LocalDate cobDate) {
        return new LoanCobPartitioner(repository, cobDate, properties.cob().partitionSize());
    }
}
