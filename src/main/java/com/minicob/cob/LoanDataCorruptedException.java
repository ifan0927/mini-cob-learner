package com.minicob.cob;

/**
 * The loan's data is inconsistent. COB must stop instead of booking accruals on top of a broken
 * ledger, so this exception is never skipped.
 */
public class LoanDataCorruptedException extends RuntimeException {

    public LoanDataCorruptedException(String message) {
        super(message);
    }
}
