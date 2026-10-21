package com.minicob.businessdate;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessDateService {

    private final BusinessDateRepository repository;

    public BusinessDateService(BusinessDateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LocalDate getBusinessDate() {
        return repository.findById(BusinessDateEntry.BUSINESS_DATE)
                .map(BusinessDateEntry::getDate)
                .orElseThrow(() -> new IllegalStateException("Business date has not been initialized"));
    }

    @Transactional
    public void advanceTo(LocalDate newBusinessDate) {
        BusinessDateEntry entry = repository.findById(BusinessDateEntry.BUSINESS_DATE).orElseThrow();
        if (!newBusinessDate.isAfter(entry.getDate())) {
            throw new IllegalArgumentException("Business date can only move forward: " + entry.getDate() + " -> " + newBusinessDate);
        }
        entry.moveTo(newBusinessDate);
    }

    @Transactional
    public void initializeIfMissing(LocalDate initial) {
        if (!repository.existsById(BusinessDateEntry.BUSINESS_DATE)) {
            repository.save(new BusinessDateEntry(BusinessDateEntry.BUSINESS_DATE, initial));
        }
    }
}
