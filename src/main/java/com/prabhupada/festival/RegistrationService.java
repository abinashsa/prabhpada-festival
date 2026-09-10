package com.prabhupada.festival;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Applies a submission against the existing roster. Email is the identity, so a
 * repeat submission from an address already registered updates that entry rather
 * than adding a second one.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final RegistrationStore store;
    private final RegistrationPublisher publisher;

    public RegistrationService(RegistrationStore store, RegistrationPublisher publisher) {
        this.store = store;
        this.publisher = publisher;
    }

    /** @return the stored registration, and whether this created a new entry. */
    public synchronized Outcome apply(Registration submitted) {
        Optional<Registration> existing = existingFor(submitted.email());

        Registration toSave = existing
                .map(submitted::updating)
                .orElseGet(() -> submitted.accepted(store.nextRegId()));

        store.put(toSave);
        try {
            publisher.upsert(toSave);
        } catch (RegistrationPublisher.PublishFailedException e) {
            // Roll the store back so a retry behaves the same as the first attempt.
            if (existing.isPresent()) {
                store.put(existing.get());
            } else {
                store.remove(toSave.regId());
            }
            throw e;
        }
        return new Outcome(toSave, existing.isEmpty());
    }

    /**
     * The store knows about this address, or — after a restart, when the store is
     * empty but the sheet is not — the sheet does.
     */
    private Optional<Registration> existingFor(String email) {
        return store.findByEmail(email).or(() -> publisher.findByEmail(email));
    }

    /**
     * Counts taken from the sheet where there is one, so the dashboard reflects
     * rows added or edited by hand there, not just what this process has seen.
     */
    public RegistrationStats stats() {
        try {
            List<Registration> fromSheet = publisher.loadAll();
            if (!fromSheet.isEmpty()) {
                store.seed(fromSheet);
                return RegistrationStats.of(fromSheet);
            }
        } catch (RegistrationPublisher.PublishFailedException e) {
            log.warn("Could not refresh from the sheet, reporting what this process holds: {}", e.getMessage());
        }
        return RegistrationStats.of(store.findAll());
    }

    public record Outcome(Registration registration, boolean created) {
    }
}
