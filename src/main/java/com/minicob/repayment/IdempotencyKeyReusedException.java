package com.minicob.repayment;

public class IdempotencyKeyReusedException extends RuntimeException {

    public IdempotencyKeyReusedException(String key) {
        super("Idempotency key '" + key + "' was already used for a different repayment request");
    }
}
