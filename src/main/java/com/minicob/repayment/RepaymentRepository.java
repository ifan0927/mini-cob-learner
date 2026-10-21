package com.minicob.repayment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    Optional<Repayment> findByIdempotencyKey(String idempotencyKey);

    List<Repayment> findByLoanIdOrderById(Long loanId);
}
