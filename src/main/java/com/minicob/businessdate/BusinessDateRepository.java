package com.minicob.businessdate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessDateRepository extends JpaRepository<BusinessDateEntry, String> {
}
