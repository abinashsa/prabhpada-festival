package com.prabhupada.festival;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory registration store. Contents are lost on restart — swap in a real
 * datasource (JPA, Mongo, a spreadsheet export) before running a live festival.
 */
@Repository
public class RegistrationStore {

    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public Registration save(Registration registration) {
        String regId = "REG-%04d".formatted(sequence.incrementAndGet());
        Registration accepted = registration.accepted(regId);
        registrations.put(regId, accepted);
        return accepted;
    }

    public Optional<Registration> find(String regId) {
        return Optional.ofNullable(registrations.get(regId));
    }

    public List<Registration> findAll() {
        return new ArrayList<>(registrations.values());
    }
}
