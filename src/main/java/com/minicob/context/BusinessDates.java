package com.minicob.context;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The dates a unit of work runs under.
 *
 * @param businessDate the open business day (used for customer transactions)
 * @param cobDate      the business day being closed by COB
 */
public record BusinessDates(LocalDate businessDate, LocalDate cobDate) {

    public BusinessDates {
        Objects.requireNonNull(businessDate, "businessDate");
        Objects.requireNonNull(cobDate, "cobDate");
    }

    /** Dates for a request handled while {@code businessDate} is open. */
    public static BusinessDates openDay(LocalDate businessDate) {
        return new BusinessDates(businessDate, businessDate.minusDays(1));
    }

    /** Dates for a COB run closing {@code cobDate}. */
    public static BusinessDates closing(LocalDate cobDate) {
        return new BusinessDates(cobDate.plusDays(1), cobDate);
    }
}
