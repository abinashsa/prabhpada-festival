package com.prabhupada.festival;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** A submitted festival registration. */
public record Registration(
        String regId,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        String phone,
        @NotNull RegistrationType type,
        @Min(1) int guestCount,
        int amount,
        String message,
        Instant submittedAt
) {
    /** Rebuilds the record with server-assigned id, amount and timestamp. */
    Registration accepted(String regId) {
        int headcount = type == RegistrationType.FAMILY ? Math.max(guestCount, 2) : 1;
        return new Registration(regId, fullName, email, phone, type,
                headcount, type.amount(), message, Instant.now());
    }
}
