package com.minicob.businessdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "business_date")
public class BusinessDateEntry {

    public static final String BUSINESS_DATE = "BUSINESS_DATE";

    @Id
    @Column(name = "date_type", length = 30)
    private String type;

    @Column(name = "value_date", nullable = false)
    private LocalDate date;

    protected BusinessDateEntry() {
    }

    public BusinessDateEntry(String type, LocalDate date) {
        this.type = type;
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void moveTo(LocalDate date) {
        this.date = date;
    }
}
