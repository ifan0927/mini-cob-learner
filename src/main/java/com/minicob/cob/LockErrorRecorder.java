package com.minicob.cob;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the reason COB could not process a loan onto that loan's lock, so operations can see it.
 */
@Service
public class LockErrorRecorder {

    private static final Logger log = LoggerFactory.getLogger(LockErrorRecorder.class);

    private final AccountLockRepository repository;

    public LockErrorRecorder(AccountLockRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordError(Long loanId, String message, Throwable error) {
        repository.findById(loanId).ifPresentOrElse(
                lock -> lock.recordError(message, stacktraceOf(error)),
                () -> log.warn("No COB lock found for loan {} while recording: {}", loanId, message));
    }

    private static String stacktraceOf(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
