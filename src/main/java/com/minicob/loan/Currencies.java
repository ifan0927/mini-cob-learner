package com.minicob.loan;

import java.util.Map;

/** Supported currencies and the number of decimal places amounts are rounded to. */
public final class Currencies {

    private static final Map<String, Integer> SCALES = Map.of(
            "TWD", 0,
            "JPY", 0,
            "USD", 2);

    private Currencies() {
    }

    public static int scaleOf(String currency) {
        Integer scale = currency == null ? null : SCALES.get(currency);
        if (scale == null) {
            throw new UnsupportedCurrencyException(currency);
        }
        return scale;
    }
}
