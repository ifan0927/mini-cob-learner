package com.minicob.cob;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountLockService {

    private final AccountLockRepository repository;

    public AccountLockService(AccountLockRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isLocked(Long loanId) {
        return repository.existsById(loanId);
    }

    /** Places (or takes over) a COB lock for every loan in {@code loanIds}. */
    @Transactional
    public void lockForCob(Collection<Long> loanIds, LocalDate cobDate) {
        Map<Long, AccountLock> existing = repository.findAllById(loanIds).stream()
                .collect(Collectors.toMap(AccountLock::getLoanId, Function.identity()));
        List<AccountLock> toSave = new ArrayList<>();
        for (Long loanId : loanIds) {
            AccountLock lock = existing.get(loanId);
            if (lock == null) {
                toSave.add(new AccountLock(loanId, LockOwner.LOAN_COB, cobDate));
            } else {
                lock.takeOver(cobDate);
            }
        }
        repository.saveAll(toSave);
    }

    /** Releases the locks of loans COB finished successfully. Joins the caller's transaction. */
    @Transactional
    public void release(Collection<Long> loanIds) {
        repository.deleteAllByIdInBatch(loanIds);
    }

    /** Releases every lock among {@code loanIds} that does not carry an error. */
    @Transactional
    public int releaseErrorFree(Collection<Long> loanIds) {
        return loanIds.isEmpty() ? 0 : repository.deleteErrorFreeLocks(loanIds);
    }
}
