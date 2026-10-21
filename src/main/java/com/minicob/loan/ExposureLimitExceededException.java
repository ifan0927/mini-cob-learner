package com.minicob.loan;

import java.math.BigDecimal;

/** Business rule violation: booking the loan would put the customer over the exposure limit. */
public class ExposureLimitExceededException extends Exception {

    public ExposureLimitExceededException(String customerId, String currency, BigDecimal exposure, BigDecimal limit) {
        super("Customer " + customerId + " exposure " + exposure.toPlainString() + " " + currency
                + " would exceed the limit of " + limit.toPlainString());
    }
}
