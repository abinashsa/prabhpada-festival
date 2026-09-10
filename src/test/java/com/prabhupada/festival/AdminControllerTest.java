package com.prabhupada.festival;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestPropertySource(properties = "festival.admin.key=test-key-1234")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminControllerTest {

    /** Records what would have been sent instead of sending it. */
    static class RecordingMailer implements FestivalMailer {
        final List<String> subjects = new ArrayList<>();

        @Override
        public Result sendToEveryone(String subject, String body) {
            subjects.add(subject);
            return new Result(3, 97);
        }
    }

    @TestConfiguration
    static class Mailbox {
        @Bean
        @Primary
        RecordingMailer recordingMailer() {
            return new RecordingMailer();
        }
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    RecordingMailer mailer;

    private HttpHeaders keyed(String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (key != null) {
            headers.set(AdminController.KEY_HEADER, key);
        }
        return headers;
    }

    private void register(String email, int guests) {
        rest.postForEntity("/api/registrations",
                Map.of("fullName", "Guest", "email", email, "guestCount", guests), Map.class);
    }

    @Test
    void theRightKeyUnlocks() {
        ResponseEntity<Void> res = rest.exchange("/api/admin/unlock", HttpMethod.POST,
                new HttpEntity<>(keyed("test-key-1234")), Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void awrongOrMissingKeyIsRefusedEverywhere() {
        assertThat(rest.exchange("/api/admin/unlock", HttpMethod.POST,
                new HttpEntity<>(keyed("nope")), Void.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(rest.exchange("/api/admin/stats", HttpMethod.GET,
                new HttpEntity<>(keyed(null)), String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(rest.exchange("/api/admin/email", HttpMethod.POST,
                new HttpEntity<>(Map.of("subject", "Hi", "message", "There"), keyed("nope")), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void statsCountPartiesAndPeopleSeparately() {
        register("one@example.org", 4);
        register("two@example.org", 2);
        register("three@example.org", 1);

        ResponseEntity<RegistrationStats> res = rest.exchange("/api/admin/stats", HttpMethod.GET,
                new HttpEntity<>(keyed("test-key-1234")), RegistrationStats.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().registrations()).isEqualTo(3);
        assertThat(res.getBody().attending()).isEqualTo(7);
        assertThat(res.getBody().largestParty()).isEqualTo(4);
    }

    @Test
    void aRepeatRegistrationDoesNotInflateTheHeadcount() {
        register("same@example.org", 2);
        register("same@example.org", 5);

        ResponseEntity<RegistrationStats> res = rest.exchange("/api/admin/stats", HttpMethod.GET,
                new HttpEntity<>(keyed("test-key-1234")), RegistrationStats.class);

        assertThat(res.getBody().registrations()).isEqualTo(1);
        assertThat(res.getBody().attending()).isEqualTo(5);
    }

    @Test
    void aBlastNeedsBothASubjectAndAMessage() {
        ResponseEntity<String> res = rest.exchange("/api/admin/email", HttpMethod.POST,
                new HttpEntity<>(Map.of("subject", "", "message", "body"), keyed("test-key-1234")), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mailer.subjects).isEmpty();
    }

    @Test
    void anAuthorizedBlastIsHandedToTheMailer() {
        ResponseEntity<Map> res = rest.exchange("/api/admin/email", HttpMethod.POST,
                new HttpEntity<>(Map.of("subject", "Parking", "message", "Behind the hall."),
                        keyed("test-key-1234")), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsEntry("sent", 3).containsEntry("remainingQuota", 97);
        assertThat(mailer.subjects).containsExactly("Parking");
    }
}
