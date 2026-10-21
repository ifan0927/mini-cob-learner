package com.minicob.web;

import com.minicob.loan.ExposureLimitExceededException;
import com.minicob.loan.LoanNotFoundException;
import com.minicob.loan.RepaymentExceedsOutstandingException;
import com.minicob.loan.UnsupportedCurrencyException;
import com.minicob.repayment.IdempotencyKeyReusedException;
import com.minicob.repayment.LoanLockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(LoanNotFoundException.class)
    ProblemDetail notFound(LoanNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({UnsupportedCurrencyException.class, IllegalArgumentException.class})
    ProblemDetail badRequest(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({ExposureLimitExceededException.class, RepaymentExceedsOutstandingException.class,
            IdempotencyKeyReusedException.class})
    ProblemDetail unprocessable(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler({LoanLockedException.class, OptimisticLockingFailureException.class})
    ProblemDetail conflict(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        log.error("Unhandled error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
    }
}
