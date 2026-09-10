package com.prabhupada.festival;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    static final String KEY_HEADER = "X-Admin-Key";

    private final AdminProperties properties;
    private final RegistrationService service;
    private final FestivalMailer mailer;

    public AdminController(AdminProperties properties, RegistrationService service, FestivalMailer mailer) {
        this.properties = properties;
        this.service = service;
        this.mailer = mailer;
    }

    /** Confirms a key without returning anything else, so the page can gate itself. */
    @PostMapping("/unlock")
    public ResponseEntity<Void> unlock(@RequestHeader(value = KEY_HEADER, required = false) String key) {
        return authorized(key) ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/stats")
    public ResponseEntity<RegistrationStats> stats(
            @RequestHeader(value = KEY_HEADER, required = false) String key) {
        if (!authorized(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(service.stats());
    }

    @PostMapping("/email")
    public ResponseEntity<?> email(@RequestHeader(value = KEY_HEADER, required = false) String key,
                                   @Valid @RequestBody Blast blast) {
        if (!authorized(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            FestivalMailer.Result result = mailer.sendToEveryone(blast.subject(), blast.message());
            return ResponseEntity.ok(result);
        } catch (FestivalMailer.SendFailedException e) {
            log.error("Blast email failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new Problem(e.getMessage()));
        }
    }

    /**
     * Compared in constant time so the key cannot be guessed a character at a time,
     * and refused outright when no key is configured.
     */
    private boolean authorized(String presented) {
        if (!properties.isEnabled() || presented == null) {
            return false;
        }
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                properties.getKey().getBytes(StandardCharsets.UTF_8));
    }

    public record Blast(@NotBlank String subject, @NotBlank String message) {
    }

    public record Problem(String message) {
    }
}
