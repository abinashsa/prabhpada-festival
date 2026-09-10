package com.prabhupada.festival;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SheetsConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withUserConfiguration(SheetsConfig.class);

    @Test
    void withoutConfigurationTheAppFallsBackToLoggingOnly() {
        runner.run(context -> assertThat(context).hasSingleBean(RegistrationPublisher.class)
                .getBean(RegistrationPublisher.class).isInstanceOf(LoggingRegistrationPublisher.class));
    }

    @Test
    void enablingSheetsWithoutASpreadsheetIdFailsFastAtStartup() {
        runner.withPropertyValues("festival.sheets.enabled=true")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("spreadsheet-id is not set"));
    }
}
