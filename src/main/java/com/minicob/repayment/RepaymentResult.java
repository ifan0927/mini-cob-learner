package com.minicob.repayment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepaymentResult(Long repaymentId, Long loanId, BigDecimal amount, BigDecimal principalPortion,
                              BigDecimal interestPortion, BigDecimal penaltyPortion, LocalDate transactionDate,
                              boolean replayed) {

    static RepaymentResult of(Repayment repayment, boolean replayed) {
        return new RepaymentResult(repayment.getId(), repayment.getLoanId(), repayment.getAmount(),
                repayment.getPrincipalPortion(), repayment.getInterestPortion(), repayment.getPenaltyPortion(),
                repayment.getTransactionDate(), replayed);
    }
}
