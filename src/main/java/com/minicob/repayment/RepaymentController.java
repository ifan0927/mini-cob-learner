package com.minicob.repayment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/loans/{loanId}/repayments")
public class RepaymentController {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    public record RepaymentRequest(@NotNull @Positive BigDecimal amount) {
    }

    @PostMapping
    public RepaymentResult repay(@PathVariable Long loanId,
                                 @RequestHeader(IDEMPOTENCY_KEY_HEADER) @Size(min = 1, max = 100) String idempotencyKey,
                                 @Valid @RequestBody RepaymentRequest request) {
        return repaymentService.repay(loanId, idempotencyKey, request.amount());
    }

    @GetMapping
    public List<RepaymentResult> history(@PathVariable Long loanId) {
        return repaymentService.history(loanId);
    }
}
