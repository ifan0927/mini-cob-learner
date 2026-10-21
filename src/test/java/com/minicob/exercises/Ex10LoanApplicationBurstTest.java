package com.minicob.exercises;

import static org.assertj.core.api.Assertions.assertThat;

import com.minicob.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;

/**
 * Exercise 10 - reported by the online channel: during a marketing campaign, a burst of loan
 * applications made the service hang for a few seconds and then fail several of them. Production
 * runs with a small database connection pool (4 connections).
 */
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=4",
        "spring.datasource.hikari.connection-timeout=2000"
})
class Ex10LoanApplicationBurstTest extends IntegrationTestBase {

    private static final int APPLICATIONS = 8;

    @Test
    void burstOfApplicationsIsServedOnASmallConnectionPool() throws Exception {
        ExecutorService clients = Executors.newFixedThreadPool(APPLICATIONS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MockHttpServletResponse>> responses = new ArrayList<>();
        for (int i = 0; i < APPLICATIONS; i++) {
            String customer = "C-EX10-" + i;
            responses.add(clients.submit(() -> {
                start.await();
                return postJson("/api/loans", Map.of("customerId", customer, "currency", "TWD", "principal", 100000,
                        "annualInterestRate", 0.03, "termMonths", 3)).andReturn().getResponse();
            }));
        }
        start.countDown();

        List<String> failed = new ArrayList<>();
        for (Future<MockHttpServletResponse> future : responses) {
            MockHttpServletResponse response = future.get(60, TimeUnit.SECONDS);
            if (response.getStatus() != 201) {
                failed.add("HTTP " + response.getStatus() + ": " + response.getContentAsString());
            }
        }
        clients.shutdownNow();

        assertThat(failed).as("loan applications that failed out of %d", APPLICATIONS).isEmpty();
        assertThat(loanRepository.count()).isEqualTo(APPLICATIONS);
    }
}
