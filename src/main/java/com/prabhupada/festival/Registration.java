package com.prabhupada.festival;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Locale;

/** A submitted festival registration. Email is the identity: one row per address. */
public record Registration(
        String regId,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        String phone,
        @Min(1) int guestCount,
        String message,
        Instant submittedAt
) {
    /**
     * Text arrives as typed — pasted addresses often carry stray spaces, and an
     * untrimmed one would fail @Email outright instead of matching its own row.
     */
    public Registration {
        fullName = trimmed(fullName);
        email = trimmed(email);
        phone = trimmed(phone);
        message = trimmed(message);
    }

    private static String trimmed(String value) {
        return value == null ? null : value.strip();
    }

    /** First registration under this email: new id, stamped now. */
    Registration accepted(String regId) {
        return new Registration(regId, fullName, email, phone,
                Math.max(guestCount, 1), message, Instant.now());
    }

    /**
     * A repeat submission from the same address. The confirmation id and the
     * original sign-up time are kept; everything else takes the new values.
     */
    Registration updating(Registration existing) {
        return new Registration(existing.regId(), fullName, email, phone,
                Math.max(guestCount, 1), message, existing.submittedAt());
    }

    /** The identity key: addresses differing only by case or padding are the same person. */
    public String emailKey() {
        return emailKey(email);
    }

    public static String emailKey(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
