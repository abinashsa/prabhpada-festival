package com.prabhupada.festival;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used when Google Sheets is switched off. Registrations stay in memory only,
 * so this logs loudly enough that the gap is obvious in a running deployment.
 */
public class LoggingRegistrationPublisher implements RegistrationPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingRegistrationPublisher.class);

    @Override
    public void upsert(Registration registration) {
        log.warn("Google Sheets is disabled (festival.sheets.enabled=false) - registration {} for {} "
                + "is held in memory only and will be lost on restart", registration.regId(), registration.email());
    }
}
