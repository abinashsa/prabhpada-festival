package com.prabhupada.festival;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises the publisher against a stand-in for the Apps Script deployment. */
class AppsScriptPublisherTest {

    private final ObjectMapper json = new ObjectMapper();
    private final List<JsonNode> received = new ArrayList<>();

    private HttpServer server;
    private AppsScriptPublisher publisher;
    private String cannedResponse = "{\"rows\":[]}";
    private int status = 200;

    @BeforeEach
    void startStubScript() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/exec", exchange -> {
            received.add(json.readTree(exchange.getRequestBody()));
            byte[] body = cannedResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        SheetsProperties properties = new SheetsProperties();
        properties.setWebhookUrl("http://localhost:" + server.getAddress().getPort() + "/exec");
        properties.setWebhookSecret("s3cret");
        publisher = new AppsScriptPublisher(new AppsScriptClient(properties));
    }

    @AfterEach
    void stopStubScript() {
        server.stop(0);
    }

    private Registration registration() {
        return new Registration("REG-0001", "Radha D", "radha@example.org", "555-0100", 3,
                "no onion", Instant.parse("2026-09-10T15:04:05Z"));
    }

    @Test
    void sendsTheSecretAndTheRowInSheetColumnOrder() {
        publisher.upsert(registration());

        JsonNode sent = received.get(0);
        assertThat(sent.get("action").asText()).isEqualTo("upsert");
        assertThat(sent.get("secret").asText()).isEqualTo("s3cret");
        assertThat(sent.get("row")).hasSize(7);
        assertThat(sent.get("row").get(SheetRows.ID).asText()).isEqualTo("REG-0001");
        assertThat(sent.get("row").get(SheetRows.EMAIL).asText()).isEqualTo("radha@example.org");
        assertThat(sent.get("row").get(SheetRows.ATTENDING).asInt()).isEqualTo(3);
    }

    @Test
    void readsExistingRowsBackIntoRegistrations() {
        cannedResponse = """
                {"rows":[["REG-0004","2026-09-01 09:00:00","Gopal S","Gopal@Example.ORG","","2","note"]]}""";

        Optional<Registration> found = publisher.findByEmail("  gopal@example.org ");

        assertThat(found).isPresent();
        assertThat(found.get().regId()).isEqualTo("REG-0004");
        assertThat(found.get().guestCount()).isEqualTo(2);
        assertThat(publisher.loadAll()).hasSize(1);
    }

    @Test
    void anUnknownAddressIsSimplyAbsent() {
        cannedResponse = "{\"rows\":[[\"REG-0004\",\"\",\"Gopal\",\"gopal@example.org\"]]}";

        assertThat(publisher.findByEmail("nobody@example.org")).isEmpty();
    }

    @Test
    void aRejectedSecretFailsLoudlyRatherThanLookingLikeSuccess() {
        cannedResponse = "{\"error\":\"unauthorized\"}";

        assertThatThrownBy(() -> publisher.upsert(registration()))
                .isInstanceOf(RegistrationPublisher.PublishFailedException.class)
                .hasMessageContaining("unauthorized");
    }

    @Test
    void anHttpErrorFromTheScriptIsReportedToo() {
        status = 500;
        cannedResponse = "boom";

        assertThatThrownBy(() -> publisher.upsert(registration()))
                .isInstanceOf(RegistrationPublisher.PublishFailedException.class)
                .hasMessageContaining("HTTP 500");
    }
}
