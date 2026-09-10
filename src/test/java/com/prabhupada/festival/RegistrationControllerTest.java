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

    @Test
    void servesTheFestivalPageAtRoot() {
        ResponseEntity<String> res = rest.getForEntity("/", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        assertThat(res.getBody()).contains("The Legendary Festival");
    }

    @Test
    void familyRegistrationIsPricedByTheServer() {
        Map<String, Object> body = Map.of(
                "fullName", "Radha D",
                "email", "radha@example.org",
                "type", "family",
                "guestCount", 4,
                "amount", 1,
                "message", "no onion or garlic");

        ResponseEntity<Map> res = rest.postForEntity("/api/registrations", body, Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).containsEntry("amount", 50)
                .containsEntry("guestCount", 4)
                .containsEntry("type", "family");
        assertThat(res.getBody().get("regId")).asString().startsWith("REG-");
    }

    @Test
    void individualRegistrationIsReadBackById() {
        Map<String, Object> body = Map.of(
                "fullName", "Gopal S",
                "email", "gopal@example.org",
                "type", "individual",
                "guestCount", 1);

        ResponseEntity<Map> created = rest.postForEntity("/api/registrations", body, Map.class);
        String regId = (String) created.getBody().get("regId");

        ResponseEntity<Map> found = rest.getForEntity("/api/registrations/" + regId, Map.class);

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).containsEntry("fullName", "Gopal S")
                .containsEntry("amount", 20)
                .containsEntry("guestCount", 1);
    }

    @Test
    void rejectsAnInvalidEmail() {
        Map<String, Object> body = Map.of(
                "fullName", "Gopal",
                "email", "not-an-email",
                "type", "individual",
                "guestCount", 1);

        ResponseEntity<String> res = rest.postForEntity("/api/registrations", body, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownRegistrationIdIsNotFound() {
        ResponseEntity<String> res = rest.getForEntity("/api/registrations/REG-9999", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
