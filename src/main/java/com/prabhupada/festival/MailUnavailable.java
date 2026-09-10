package com.prabhupada.festival;

/** Stands in when no Apps Script web app is configured, so there is nothing to send through. */
public class MailUnavailable implements FestivalMailer {

    @Override
    public Result sendToEveryone(String subject, String body) {
        throw new SendFailedException(
                "Email goes out through the Apps Script web app, which is not configured. "
                        + "Set festival.sheets.webhook-url to enable it.", null);
    }
}
