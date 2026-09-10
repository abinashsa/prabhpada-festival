package com.prabhupada.festival;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory registration store, and the source of confirmation ids. It is a
 * working copy for the running process: the Google Sheet is the durable record.
 * Email is the identity, so the store is indexed by it as well as by id.
 */
@Repository
public class RegistrationStore {

    private final Map<String, Registration> byId = new ConcurrentHashMap<>();
    private final Map<String, String> idByEmail = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public synchronized Registration put(Registration registration) {
        byId.put(registration.regId(), registration);
        idByEmail.put(registration.emailKey(), registration.regId());
        return registration;
    }

    public String nextRegId() {
        return "REG-%04d".formatted(sequence.incrementAndGet());
    }

    /**
     * Adopts registrations already recorded in the sheet, so a restart keeps
     * confirmation ids stable and does not hand out ones already in use.
     */
    public synchronized void seed(Collection<Registration> existing) {
        for (Registration registration : existing) {
            put(registration);
            sequence.updateAndGet(current -> Math.max(current, sequenceOf(registration.regId())));
        }
    }

    /** Reads the counter back out of a "REG-0007" id; 0 for anything unrecognised. */
    private static int sequenceOf(String regId) {
        if (regId == null || !regId.startsWith("REG-")) {
            return 0;
        }
        try {
            return Integer.parseInt(regId.substring(4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Drops a registration that could not be recorded downstream. */
    public synchronized void remove(String regId) {
        Registration removed = byId.remove(regId);
        if (removed != null) {
            idByEmail.remove(removed.emailKey(), regId);
        }
    }

    public Optional<Registration> find(String regId) {
        return Optional.ofNullable(byId.get(regId));
    }

    public Optional<Registration> findByEmail(String email) {
        return Optional.ofNullable(idByEmail.get(Registration.emailKey(email))).map(byId::get);
    }

    public List<Registration> findAll() {
        return new ArrayList<>(byId.values());
    }
}
