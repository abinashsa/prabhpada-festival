package com.prabhupada.festival;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

@Configuration
@EnableConfigurationProperties({SheetsProperties.class, AdminProperties.class})
public class SheetsConfig {

    /**
     * The Apps Script web app, when one is configured. Checked first because it needs
     * no Google credentials on this machine — the script runs as the sheet's owner.
     */
    @Bean
    @ConditionalOnProperty(prefix = "festival.sheets", name = "webhook-url")
    AppsScriptClient appsScriptClient(SheetsProperties properties) {
        if (!StringUtils.hasText(properties.getWebhookSecret())) {
            throw new IllegalStateException(
                    "festival.sheets.webhook-url is set but festival.sheets.webhook-secret is not - "
                            + "the deployment URL would accept rows from anyone who has it");
        }
        return new AppsScriptClient(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "festival.sheets", name = "webhook-url")
    RegistrationPublisher appsScriptPublisher(AppsScriptClient script) {
        return new AppsScriptPublisher(script);
    }

    @Bean
    @ConditionalOnProperty(prefix = "festival.sheets", name = "webhook-url")
    FestivalMailer appsScriptMailer(AppsScriptClient script) {
        return new AppsScriptMailer(script);
    }

    @Bean
    @ConditionalOnMissingBean(FestivalMailer.class)
    FestivalMailer mailUnavailable() {
        return new MailUnavailable();
    }

    @Bean
    @ConditionalOnMissingBean(RegistrationPublisher.class)
    @ConditionalOnProperty(prefix = "festival.sheets", name = "enabled", havingValue = "true")
    // Guarded by the publisher above: a configured webhook wins over the Sheets API.
    Sheets sheetsClient(SheetsProperties properties) throws IOException, GeneralSecurityException {
        if (!StringUtils.hasText(properties.getSpreadsheetId())) {
            throw new IllegalStateException(
                    "festival.sheets.enabled is true but festival.sheets.spreadsheet-id is not set");
        }
        GoogleCredentials credentials = loadCredentials(properties)
                .createScoped(List.of(SheetsScopes.SPREADSHEETS));
        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("legendary-festival")
                .build();
    }

    private GoogleCredentials loadCredentials(SheetsProperties properties) throws IOException {
        if (!StringUtils.hasText(properties.getCredentialsPath())) {
            // Falls back to GOOGLE_APPLICATION_CREDENTIALS / gcloud ADC.
            return GoogleCredentials.getApplicationDefault();
        }
        try (InputStream key = new FileInputStream(properties.getCredentialsPath())) {
            return GoogleCredentials.fromStream(key);
        }
    }

    @Bean
    @ConditionalOnMissingBean(RegistrationPublisher.class)
    @ConditionalOnProperty(prefix = "festival.sheets", name = "enabled", havingValue = "true")
    GoogleSheetsPublisher googleSheetsPublisher(Sheets sheets, SheetsProperties properties) {
        GoogleSheetsPublisher publisher = new GoogleSheetsPublisher(sheets, properties);
        publisher.resolveTab();
        publisher.ensureHeaderRow();
        return publisher;
    }

    @Bean
    @ConditionalOnMissingBean(RegistrationPublisher.class)
    RegistrationPublisher loggingRegistrationPublisher() {
        return new LoggingRegistrationPublisher();
    }
}
