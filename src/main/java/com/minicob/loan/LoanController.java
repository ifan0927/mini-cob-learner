package com.minicob.loan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    public record CreateLoanRequest(
            @NotBlank String customerId,
            @NotBlank String currency,
            @NotNull @Positive BigDecimal principal,
            @NotNull @DecimalMin("0") BigDecimal annualInterestRate,
            @Min(1) @Max(360) int termMonths) {
    }

    public record InstallmentResponse(int number, LocalDate dueDate, BigDecimal principalDue, BigDecimal principalPaid,
                                      InstallmentStatus status) {
    }

    public record LoanResponse(Long id, String accountNo, String customerId, String currency, BigDecimal principal,
                               BigDecimal outstandingPrincipal, BigDecimal annualInterestRate, BigDecimal accruedInterest,
                               BigDecimal accruedPenalty, LoanStatus status, LocalDate disbursementDate,
                               LocalDate lastClosedBusinessDate, List<InstallmentResponse> installments) {

        static LoanResponse from(Loan loan) {
            return new LoanResponse(loan.getId(), loan.getAccountNo(), loan.getCustomerId(), loan.getCurrency(),
                    loan.getPrincipal(), loan.getOutstandingPrincipal(), loan.getAnnualInterestRate(),
                    loan.getAccruedInterest(), loan.getAccruedPenalty(), loan.getStatus(), loan.getDisbursementDate(),
                    loan.getLastClosedBusinessDate(),
                    loan.getInstallments().stream()
                            .map(i -> new InstallmentResponse(i.getNumber(), i.getDueDate(), i.getPrincipalDue(),
                                    i.getPrincipalPaid(), i.getStatus()))
                            .toList());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse create(@Valid @RequestBody CreateLoanRequest request) throws ExposureLimitExceededException {
        Loan loan = loanService.createLoan(new LoanService.CreateLoanCommand(request.customerId(), request.currency(),
                request.principal(), request.annualInterestRate(), request.termMonths()));
        return LoanResponse.from(loan);
    }

    @GetMapping("/{loanId}")
    public LoanResponse get(@PathVariable Long loanId) {
        return LoanResponse.from(loanService.getLoan(loanId));
    }
}
