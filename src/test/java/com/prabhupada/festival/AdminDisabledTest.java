package com.prabhupada.festival;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/** With no key configured, the admin API must refuse everything — including an empty key. */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AdminDisabledTest {

    @Autowired
    TestRestTemplate rest;

    private HttpEntity<Void> withKey(String key) {
        HttpHeaders headers = new HttpHeaders();
        if (key != null) {
            headers.set(AdminController.KEY_HEADER, key);
        }
        return new HttpEntity<>(headers);
    }

    @Test
    void anEmptyKeyDoesNotMatchAnUnsetKey() {
        assertThat(rest.exchange("/api/admin/unlock", HttpMethod.POST, withKey(""), Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.exchange("/api/admin/stats", HttpMethod.GET, withKey(""), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.exchange("/api/admin/stats", HttpMethod.GET, withKey(null), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
