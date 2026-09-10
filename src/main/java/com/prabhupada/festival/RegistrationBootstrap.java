package com.prabhupada.festival;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adopts whatever the sheet already holds. Without this a restart would forget
 * which addresses are registered and start handing out confirmation ids that
 * rows in the sheet are already using.
 */
@Component
public class RegistrationBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RegistrationBootstrap.class);

    private final RegistrationStore store;
    private final RegistrationPublisher publisher;

    public RegistrationBootstrap(RegistrationStore store, RegistrationPublisher publisher) {
        this.store = store;
        this.publisher = publisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Registration> existing = publisher.loadAll();
        if (!existing.isEmpty()) {
            store.seed(existing);
            log.info("Adopted {} existing registration(s) from the sheet", existing.size());
        }
    }
}
