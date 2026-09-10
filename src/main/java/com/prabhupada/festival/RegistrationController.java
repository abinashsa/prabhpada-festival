package com.prabhupada.festival;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Write-only on purpose. The roster is personal data, and the app has no login,
 * so registrations are readable in the organizer's Google Sheet and nowhere else.
 */
@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final RegistrationService service;

    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    /**
     * @return 201 for a first-time registration, 200 when an existing one was
     * updated — email is the identity, so a repeat submission is an edit.
     */
    @PostMapping
    public ResponseEntity<Registration> register(@Valid @RequestBody Registration submitted) {
        RegistrationService.Outcome outcome;
        try {
            outcome = service.apply(submitted);
        } catch (RegistrationPublisher.PublishFailedException e) {
            // Better the registrant retries than we confirm a seat the organizers never see.
            log.error("Registration for {} was not recorded downstream", submitted.email(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        // No Location header: there is no readable resource to point at.
        Registration saved = outcome.registration();
        return outcome.created()
                ? ResponseEntity.status(HttpStatus.CREATED).body(saved)
                : ResponseEntity.ok(saved);
    }
}
