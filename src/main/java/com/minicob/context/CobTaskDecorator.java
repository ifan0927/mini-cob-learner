package com.minicob.context;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * Carries the submitting thread's logging context over to pooled COB worker threads.
 */
public class CobTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            setMdc(mdc);
            try {
                runnable.run();
            } finally {
                setMdc(previousMdc);
            }
        };
    }

    private static void setMdc(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}
