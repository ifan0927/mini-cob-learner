package com.minicob.loan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "number_sequence")
public class NumberSequence {

    @Id
    @Column(name = "sequence_name", length = 40)
    private String name;

    @Column(name = "next_value", nullable = false)
    private long nextValue;

    protected NumberSequence() {
    }

    public NumberSequence(String name, long nextValue) {
        this.name = name;
        this.nextValue = nextValue;
    }

    long allocate() {
        return nextValue++;
    }
}
