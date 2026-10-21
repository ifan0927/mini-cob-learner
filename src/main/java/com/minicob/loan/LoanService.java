package com.minicob.loan;

import com.minicob.config.MiniCobProperties;
import com.minicob.context.BusinessDateContext;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final MiniCobProperties properties;

    public LoanService(LoanRepository loanRepository, AccountNumberGenerator accountNumberGenerator,
                       MiniCobProperties properties) {
        this.loanRepository = loanRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.properties = properties;
    }

    public record CreateLoanCommand(String customerId, String currency, BigDecimal principal,
                                    BigDecimal annualInterestRate, int termMonths) {
    }

    @Transactional
    public Loan createLoan(CreateLoanCommand command) throws ExposureLimitExceededException {
        Loan loan = Loan.disburse(command.customerId(), command.currency(), command.principal(),
                command.annualInterestRate(), command.termMonths(), BusinessDateContext.getBusinessDate());
        loanRepository.saveAndFlush(loan);

        BigDecimal exposure = loanRepository.sumOutstandingPrincipal(command.customerId(), command.currency());
        if (exposure.compareTo(properties.customerExposureLimit()) > 0) {
            throw new ExposureLimitExceededException(command.customerId(), command.currency(), exposure,
                    properties.customerExposureLimit());
        }

        loan.assignAccountNumber(accountNumberGenerator.nextLoanAccountNumber());
        return loan;
    }

    @Transactional(readOnly = true)
    public Loan getLoan(Long loanId) {
        return loanRepository.findWithInstallmentsById(loanId).orElseThrow(() -> new LoanNotFoundException(loanId));
    }
}
