package com.minicob.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mini-cob")
public record MiniCobProperties(
        LocalDate initialBusinessDate,
        BigDecimal customerExposureLimit,
        BigDecimal penaltyAnnualRate,
        Cob cob) {

    public MiniCobProperties {
        // Rates are kept at the same precision as the loan rate column.
        penaltyAnnualRate = penaltyAnnualRate.setScale(6, RoundingMode.HALF_UP);
    }

    public record Cob(int workerThreads, int partitionSize, int chunkSize, int retryLimit, int skipLimit) {
    }
}
