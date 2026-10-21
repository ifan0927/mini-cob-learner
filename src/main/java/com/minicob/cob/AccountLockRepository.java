package com.minicob.cob;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountLockRepository extends JpaRepository<AccountLock, Long> {

    @Modifying
    @Query("delete from AccountLock l where l.loanId in :loanIds and l.errorMessage is null")
    int deleteErrorFreeLocks(@Param("loanIds") Collection<Long> loanIds);
}
