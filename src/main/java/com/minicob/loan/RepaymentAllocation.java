package com.minicob.loan;

import java.math.BigDecimal;

public record RepaymentAllocation(BigDecimal principalPortion, BigDecimal interestPortion, BigDecimal penaltyPortion) {
}
