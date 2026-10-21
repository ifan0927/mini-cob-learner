package com.minicob.cob;

import com.minicob.loan.Loan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Queries used by the COB job. */
public interface CobLoanRepository extends Repository<Loan, Long> {

    @Query("""
            select l.id from Loan l
            where l.status <> com.minicob.loan.LoanStatus.CLOSED
              and l.disbursementDate <= :cobDate
            order by l.id
            """)
    List<Long> findIdsToClose(@Param("cobDate") LocalDate cobDate);

    @EntityGraph(attributePaths = "installments")
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findForCob(@Param("id") Long id);
}
