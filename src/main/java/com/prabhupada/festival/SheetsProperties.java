package com.prabhupada.festival;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google Sheets settings. Disabled by default so the app runs without credentials;
 * see the README for how to create the service account and share the sheet with it.
 */
@ConfigurationProperties(prefix = "festival.sheets")
public class SheetsProperties {

    /** Whether registrations are appended to a Google Sheet. */
    private boolean enabled = false;

    /** The id from the sheet URL: docs.google.com/spreadsheets/d/<THIS>/edit */
    private String spreadsheetId = "";

    /**
     * Path to the service-account JSON key. When blank, Application Default
     * Credentials are used (e.g. the GOOGLE_APPLICATION_CREDENTIALS env var).
     */
    private String credentialsPath = "";

    /** Tab within the spreadsheet that rows are appended to. */
    private String tabName = "Registrations";

    /**
     * Deployment URL of the Apps Script web app bound to the sheet. When set, this
     * is used instead of the Sheets API and no Google credentials are needed.
     */
    private String webhookUrl = "";

    /** Shared secret the script checks, so only this app can post rows. */
    private String webhookSecret = "";

    /** True when the app should talk to the Apps Script web app rather than the Sheets API. */
    public boolean usesWebhook() {
        return !webhookUrl.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    public String getCredentialsPath() {
        return credentialsPath;
    }

    public void setCredentialsPath(String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}
