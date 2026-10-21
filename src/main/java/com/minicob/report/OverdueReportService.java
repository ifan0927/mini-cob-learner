package com.minicob.report;

import com.minicob.loan.Installment;
import com.minicob.loan.InstallmentStatus;
import com.minicob.loan.Loan;
import com.minicob.loan.LoanRepository;
import com.minicob.loan.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OverdueReportService {

    private final LoanRepository loanRepository;

    public OverdueReportService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public record OverdueLoanLine(Long loanId, String accountNo, String customerId, String currency,
                                  int overdueInstallments, BigDecimal overduePrincipal, LocalDate oldestDueDate,
                                  BigDecimal accruedInterest, BigDecimal accruedPenalty) {
    }

    @Transactional(readOnly = true)
    public List<OverdueLoanLine> overdueLoans() {
        return loanRepository.findByStatusOrderById(LoanStatus.OVERDUE).stream()
                .map(OverdueReportService::toLine)
                .toList();
    }

    private static OverdueLoanLine toLine(Loan loan) {
        List<Installment> overdue = loan.getInstallments().stream()
                .filter(i -> i.getStatus() == InstallmentStatus.OVERDUE)
                .toList();
        BigDecimal overduePrincipal = overdue.stream().map(Installment::remainingPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate oldest = overdue.stream().map(Installment::getDueDate).min(Comparator.naturalOrder()).orElse(null);
        return new OverdueLoanLine(loan.getId(), loan.getAccountNo(), loan.getCustomerId(), loan.getCurrency(),
                overdue.size(), overduePrincipal, oldest, loan.getAccruedInterest(), loan.getAccruedPenalty());
    }
}
