package com.minicob.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicob.cob.AccountLock;
import com.minicob.cob.AccountLockRepository;
import com.minicob.cob.CobService;
import com.minicob.loan.Loan;
import com.minicob.loan.LoanRepository;
import com.minicob.loan.LoanTransactionRepository;
import com.minicob.repayment.RepaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSupportConfiguration.class)
public abstract class IntegrationTestBase {

    /** The business date every test starts on. COB for this date closes it. */
    protected static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 4, 16);

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected TestLoans loans;
    @Autowired
    protected CobService cobService;
    @Autowired
    protected LoanRepository loanRepository;
    @Autowired
    protected LoanTransactionRepository transactionRepository;
    @Autowired
    protected RepaymentRepository repaymentRepository;
    @Autowired
    protected AccountLockRepository lockRepository;
    @Autowired
    protected TellerActivitySimulator tellerActivity;
    @Autowired
    protected TransactionTemplate transactionTemplate;

    @BeforeEach
    void resetData() {
        tellerActivity.disarm();
        jdbc.update("delete from loan_transaction");
        jdbc.update("delete from repayment");
        jdbc.update("delete from account_lock");
        jdbc.update("delete from installment");
        jdbc.update("delete from loan");
        jdbc.update("update business_date set value_date = ? where date_type = 'BUSINESS_DATE'", BUSINESS_DATE);
    }

    /** Reloads a loan with its schedule. */
    protected Loan reload(Long loanId) {
        return transactionTemplate.execute(status -> {
            Loan loan = loanRepository.findWithInstallmentsById(loanId).orElseThrow();
            loan.getInstallments().size();
            return loan;
        });
    }

    protected Optional<AccountLock> lockOf(Long loanId) {
        return lockRepository.findById(loanId);
    }

    protected ResultActions postJson(String url, Object body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    protected ResultActions repay(Long loanId, String idempotencyKey, String amount) throws Exception {
        return mvc.perform(post("/api/loans/{id}/repayments", loanId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("amount", new BigDecimal(amount)))));
    }

    protected ResultActions getJson(String url, Object... vars) throws Exception {
        return mvc.perform(get(url, vars));
    }
}
