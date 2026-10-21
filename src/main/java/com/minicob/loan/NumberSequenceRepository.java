package com.minicob.loan;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from NumberSequence s where s.name = :name")
    Optional<NumberSequence> findForUpdate(@Param("name") String name);
}
