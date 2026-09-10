package com.prabhupada.festival;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A restart must not reissue confirmation ids the sheet already uses. */
class RegistrationStoreSeedTest {

    private Registration existing(String regId, String email) {
        return new Registration(regId, "Someone", email, "", 1, "", Instant.now());
    }

    @Test
    void idsContinuePastTheHighestOneAlreadyInTheSheet() {
        RegistrationStore store = new RegistrationStore();
        store.seed(List.of(existing("REG-0001", "a@example.org"), existing("REG-0007", "b@example.org")));

        assertThat(store.nextRegId()).isEqualTo("REG-0008");
    }

    @Test
    void seededAddressesAreFoundByEmail() {
        RegistrationStore store = new RegistrationStore();
        store.seed(List.of(existing("REG-0004", "Seeded@Example.org")));

        assertThat(store.findByEmail("seeded@example.org")).isPresent()
                .get().extracting(Registration::regId).isEqualTo("REG-0004");
    }

    @Test
    void unrecognisedIdsDoNotDisturbTheCounter() {
        RegistrationStore store = new RegistrationStore();
        store.seed(List.of(existing("hand-typed", "a@example.org"), existing("REG-0002", "b@example.org")));

        assertThat(store.nextRegId()).isEqualTo("REG-0003");
    }
}
