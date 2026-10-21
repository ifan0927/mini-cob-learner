package com.minicob.loan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @EntityGraph(attributePaths = "installments")
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findWithInstallmentsById(@Param("id") Long id);

    @Query("""
            select coalesce(sum(l.outstandingPrincipal), 0) from Loan l
            where l.customerId = :customerId and l.currency = :currency
              and l.status <> com.minicob.loan.LoanStatus.CLOSED
            """)
    BigDecimal sumOutstandingPrincipal(@Param("customerId") String customerId, @Param("currency") String currency);

    List<Loan> findByCustomerIdOrderById(String customerId);

    List<Loan> findByStatusOrderById(LoanStatus status);
}
