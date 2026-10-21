package com.minicob.config;

import com.minicob.businessdate.BusinessDateService;
import com.minicob.loan.AccountNumberGenerator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the rows the application cannot run without: the current business date and the
 * account number sequence.
 */
@Component
public class ReferenceDataInitializer implements ApplicationRunner {

    private final BusinessDateService businessDateService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final MiniCobProperties properties;

    public ReferenceDataInitializer(BusinessDateService businessDateService,
                                    AccountNumberGenerator accountNumberGenerator,
                                    MiniCobProperties properties) {
        this.businessDateService = businessDateService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        businessDateService.initializeIfMissing(properties.initialBusinessDate());
        accountNumberGenerator.initializeIfMissing();
    }
}
