package com.prabhupada.festival;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Talks to the Apps Script web app bound to the sheet. Apps Script answers a POST
 * with a 302 to a content URL, which the client follows as a GET — the script has
 * already run by then, and the redirect only carries its reply.
 */
public class AppsScriptClient {

    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();
    private final SheetsProperties properties;

    public AppsScriptClient(SheetsProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    ObjectMapper json() {
        return json;
    }

    ObjectNode request(String action) {
        ObjectNode node = json.createObjectNode();
        node.put("action", action);
        node.put("secret", properties.getWebhookSecret());
        return node;
    }

    /** @param what a phrase describing the attempt, used in any error raised. */
    JsonNode call(ObjectNode request, String what) {
        try {
            HttpRequest post = HttpRequest.newBuilder(URI.create(properties.getWebhookUrl()))
                    .timeout(Duration.ofSeconds(60))   // a mail blast is slower than a row write
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(request)))
                    .build();

            HttpResponse<String> response = http.send(post, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RegistrationPublisher.PublishFailedException(
                        "Apps Script returned HTTP " + response.statusCode() + " trying to " + what, null);
            }
            JsonNode body = json.readTree(response.body());
            if (body.hasNonNull("error")) {
                throw new RegistrationPublisher.PublishFailedException(
                        "Apps Script refused to " + what + ": " + body.get("error").asText(), null);
            }
            return body;
        } catch (IOException e) {
            throw new RegistrationPublisher.PublishFailedException(
                    "Could not reach the Apps Script web app to " + what, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegistrationPublisher.PublishFailedException("Interrupted while trying to " + what, e);
        }
    }
}
