package com.minicob.support;

import com.minicob.cob.step.CobStepContext;
import com.minicob.cob.step.LoanCobBusinessStep;
import com.minicob.loan.Loan;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Test-only COB step. When armed for a loan, the first time COB reaches that loan it lets a branch
 * teller post a principal payment from another connection, which commits immediately. This
 * reproduces "a customer transaction landed while COB was working on the loan" deterministically.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TellerActivitySimulator implements LoanCobBusinessStep {

    private record Armed(Long loanId, BigDecimal amount) {
    }

    private final DataSource dataSource;
    private final AtomicReference<Armed> armed = new AtomicReference<>();

    public TellerActivitySimulator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void armFor(Long loanId, BigDecimal principalPayment) {
        armed.set(new Armed(loanId, principalPayment));
    }

    public void disarm() {
        armed.set(null);
    }

    @Override
    public void execute(Loan loan, CobStepContext context) {
        Armed current = armed.get();
        if (current == null || !current.loanId().equals(loan.getId()) || !armed.compareAndSet(current, null)) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(
                     "update loan set outstanding_principal = outstanding_principal - ?, version = version + 1 where id = ?")) {
            connection.setAutoCommit(true);
            update.setBigDecimal(1, current.amount());
            update.setLong(2, current.loanId());
            update.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Teller simulation failed", e);
        }
    }
}
