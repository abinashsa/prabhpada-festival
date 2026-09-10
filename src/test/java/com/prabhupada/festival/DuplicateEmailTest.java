package com.prabhupada.festival;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/** Email is the identity: registering again edits the existing entry. */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DuplicateEmailTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    RegistrationStore store;

    private Map<String, Object> submission(String email, String name, int guests) {
        return Map.of("fullName", name, "email", email, "guestCount", guests, "message", "note for " + name);
    }

    @Test
    void aSecondSubmissionUpdatesTheFirstInsteadOfAddingARow() {
        ResponseEntity<Map> first = rest.postForEntity(
                "/api/registrations", submission("repeat@example.org", "Radha D", 2), Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String regId = (String) first.getBody().get("regId");

        ResponseEntity<Map> second = rest.postForEntity(
                "/api/registrations", submission("repeat@example.org", "Radha Devi", 5), Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).containsEntry("regId", regId)      // same confirmation id
                .containsEntry("fullName", "Radha Devi")                 // new details
                .containsEntry("guestCount", 5);

        assertThat(store.findAll()).hasSize(1);
    }

    @Test
    void theOriginalSignUpTimeIsKeptAcrossAnUpdate() {
        Map first = rest.postForEntity(
                "/api/registrations", submission("timing@example.org", "Gopal", 1), Map.class).getBody();
        Map second = rest.postForEntity(
                "/api/registrations", submission("timing@example.org", "Gopal S", 3), Map.class).getBody();

        assertThat(second.get("submittedAt")).isEqualTo(first.get("submittedAt"));
    }

    @Test
    void addressesDifferingOnlyByCaseOrSpacingAreTheSamePerson() {
        rest.postForEntity("/api/registrations", submission("Mixed@Example.org", "Bhakta", 1), Map.class);

        ResponseEntity<Map> again = rest.postForEntity(
                "/api/registrations", submission("  mixed@example.ORG  ", "Bhakta J", 2), Map.class);

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(store.findAll()).hasSize(1);
    }

    @Test
    void differentAddressesStayDifferentRegistrations() {
        rest.postForEntity("/api/registrations", submission("one@example.org", "One", 1), Map.class);
        ResponseEntity<Map> other = rest.postForEntity(
                "/api/registrations", submission("two@example.org", "Two", 1), Map.class);

        assertThat(other.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(store.findAll()).hasSize(2);
    }
}
