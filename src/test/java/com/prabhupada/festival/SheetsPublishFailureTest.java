package com.prabhupada.festival;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * A sheet that cannot be written to must not produce a confirmed registration —
 * the registrant should be told to retry rather than given a seat no organizer sees.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class SheetsPublishFailureTest {

    @TestConfiguration
    static class FailingPublisherConfig {
        @Bean
        @Primary
        RegistrationPublisher failingPublisher() {
            return registration -> {
                throw new RegistrationPublisher.PublishFailedException("sheet unreachable", new RuntimeException());
            };
        }
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    RegistrationStore store;

    @Test
    void aFailedSheetWriteIsReportedAndLeavesNoConfirmedRegistration() {
        Map<String, Object> body = Map.of(
                "fullName", "Radha D",
                "email", "radha@example.org",
                "guestCount", 2);

        ResponseEntity<String> res = rest.postForEntity("/api/registrations", body, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(store.findAll()).isEmpty();
    }
}
