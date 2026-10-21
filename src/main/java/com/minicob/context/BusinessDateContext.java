package com.minicob.context;

import java.time.LocalDate;

/**
 * Thread-bound business dates, in the spirit of Fineract's {@code ThreadLocalContextUtil}.
 * Whoever starts a unit of work (an HTTP request, a COB run) is responsible for setting and
 * clearing it.
 */
public final class BusinessDateContext {

    private static final ThreadLocal<BusinessDates> CURRENT = new InheritableThreadLocal<>();

    private BusinessDateContext() {
    }

    public static void set(BusinessDates dates) {
        CURRENT.set(dates);
    }

    public static BusinessDates getOrNull() {
        return CURRENT.get();
    }

    public static BusinessDates get() {
        BusinessDates dates = CURRENT.get();
        if (dates == null) {
            throw new IllegalStateException("No business date bound to thread " + Thread.currentThread().getName());
        }
        return dates;
    }

    public static LocalDate getBusinessDate() {
        return get().businessDate();
    }

    public static LocalDate getCobDate() {
        return get().cobDate();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Binds {@code dates} (or clears the context when {@code null}). */
    public static void restore(BusinessDates dates) {
        if (dates == null) {
            clear();
        } else {
            set(dates);
        }
    }
}
