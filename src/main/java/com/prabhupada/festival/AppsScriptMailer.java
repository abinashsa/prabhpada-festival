package com.prabhupada.festival;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends through the same Apps Script web app, which means mail leaves from the
 * organizer's own Gmail — no SMTP credentials here. The script addresses each
 * registrant separately, so no one sees anybody else's address.
 */
public class AppsScriptMailer implements FestivalMailer {

    private static final Logger log = LoggerFactory.getLogger(AppsScriptMailer.class);

    private final AppsScriptClient script;

    public AppsScriptMailer(AppsScriptClient script) {
        this.script = script;
    }

    @Override
    public Result sendToEveryone(String subject, String body) {
        ObjectNode request = script.request("email");
        request.put("subject", subject);
        request.put("body", body);
        try {
            JsonNode response = script.call(request, "send the message to everyone");
            Result result = new Result(response.path("sent").asInt(), response.path("remainingQuota").asInt(-1));
            log.info("Blast sent to {} registrant(s); {} sends left today", result.sent(), result.remainingQuota());
            return result;
        } catch (RegistrationPublisher.PublishFailedException e) {
            throw new SendFailedException(e.getMessage(), e.getCause());
        }
    }
}
