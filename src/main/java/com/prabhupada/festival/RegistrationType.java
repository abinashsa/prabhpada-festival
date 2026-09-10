package com.prabhupada.festival;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Registration tier and its suggested donation, in whole dollars. */
public enum RegistrationType {
    INDIVIDUAL(20),
    FAMILY(50);

    private final int amount;

    RegistrationType(int amount) {
        this.amount = amount;
    }

    public int amount() {
        return amount;
    }

    @JsonValue
    public String jsonValue() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static RegistrationType from(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
