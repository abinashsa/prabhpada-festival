package com.prabhupada.festival;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class RegistrationControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    RegistrationStore store;

    @Test
    void servesTheFestivalPageAtRoot() {
        ResponseEntity<String> res = rest.getForEntity("/", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        assertThat(res.getBody()).contains("The Legendary Festival");
    }

    @Test
    void registrationSectionNoLongerAsksForTierOrDonation() {
        String page = rest.getForObject("/", String.class);

        assertThat(page).doesNotContain("Registering As")
                .doesNotContain("Donation Due")
                .doesNotContain("Pay with Venmo");
    }

    @Test
    void registrationIsSavedWithItsHeadcount() {
        Map<String, Object> body = Map.of(
                "fullName", "Radha D",
                "email", "headcount@example.org",
                "phone", "555-0100",
                "guestCount", 4,
                "message", "no onion or garlic");

        ResponseEntity<Map> res = rest.postForEntity("/api/registrations", body, Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).containsEntry("guestCount", 4).containsEntry("fullName", "Radha D");
        assertThat(res.getBody().get("regId")).asString().startsWith("REG-");

        assertThat(store.findByEmail("headcount@example.org")).isPresent()
                .get().extracting(Registration::guestCount).isEqualTo(4);
    }

    @Test
    void rejectsAnInvalidEmail() {
        Map<String, Object> body = Map.of(
                "fullName", "Gopal",
                "email", "not-an-email",
                "guestCount", 1);

        ResponseEntity<String> res = rest.postForEntity("/api/registrations", body, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theRosterIsNotReadableOverHttp() {
        assertThat(rest.getForEntity("/api/registrations", String.class).getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(rest.getForEntity("/api/registrations/REG-0001", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
