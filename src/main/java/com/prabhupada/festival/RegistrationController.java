package com.prabhupada.festival;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationStore store;

    public RegistrationController(RegistrationStore store) {
        this.store = store;
    }

    @PostMapping
    public ResponseEntity<Registration> register(@Valid @RequestBody Registration submitted) {
        Registration saved = store.save(submitted);
        return ResponseEntity.created(URI.create("/api/registrations/" + saved.regId())).body(saved);
    }

    @GetMapping("/{regId}")
    public ResponseEntity<Registration> byId(@PathVariable String regId) {
        return store.find(regId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Registration> all() {
        return store.findAll();
    }
}
