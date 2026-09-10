package com.prabhupada.festival;

import java.util.List;
import java.util.Optional;

/** Sends a saved registration on to wherever the organizers read it. */
public interface RegistrationPublisher {

    /** Writes the registration, replacing any existing entry for the same email. */
    void upsert(Registration registration);

    /**
     * The registration already recorded for this email, if any. Consulted only when
     * the running process has no record of it — after a restart, say — so that a
     * repeat submission keeps its original confirmation id instead of gaining a new one.
     */
    default Optional<Registration> findByEmail(String email) {
        return Optional.empty();
    }

    /** Everything recorded so far, used to warm the store at startup. */
    default List<Registration> loadAll() {
        return List.of();
    }

    /** Thrown when a registration could not be recorded downstream. */
    class PublishFailedException extends RuntimeException {
        public PublishFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
