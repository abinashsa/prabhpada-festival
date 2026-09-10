package com.prabhupada.festival;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Keeps the festival spreadsheet in step with the roster: one row per email
 * address, updated in place when that address registers again.
 */
public class GoogleSheetsPublisher implements RegistrationPublisher {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsPublisher.class);

    /** Data starts at row 2; row 1 is the header. */
    private static final int FIRST_DATA_ROW = 2;

    private final Sheets sheets;
    private final SheetsProperties properties;

    /** The tab actually written to — see {@link #resolveTab()}. */
    private volatile String tab;

    public GoogleSheetsPublisher(Sheets sheets, SheetsProperties properties) {
        this.sheets = sheets;
        this.properties = properties;
        this.tab = properties.getTabName();
    }

    /**
     * A brand-new spreadsheet's only tab is called "Sheet1", so a configured name
     * of "Registrations" would make every read fail on an unparseable range. Prefer
     * the configured tab when it exists, otherwise fall back to the first one.
     */
    void resolveTab() {
        try {
            List<String> titles = sheets.spreadsheets().get(properties.getSpreadsheetId()).execute()
                    .getSheets().stream()
                    .map(sheet -> sheet.getProperties().getTitle())
                    .toList();
            if (titles.contains(properties.getTabName())) {
                tab = properties.getTabName();
            } else if (!titles.isEmpty()) {
                tab = titles.get(0);
                log.warn("No tab named '{}' in the spreadsheet - using '{}' instead",
                        properties.getTabName(), tab);
            }
        } catch (IOException e) {
            throw new PublishFailedException("Could not read the spreadsheet's tabs. Check that the sheet id is "
                    + "correct and that it is shared with the service account", e);
        }
    }

    @Override
    public void upsert(Registration registration) {
        try {
            List<List<Object>> rows = readDataRows();
            int index = SheetRows.indexOfEmail(rows, registration.email());
            ValueRange body = new ValueRange().setValues(List.of(SheetRows.asRow(registration)));

            if (index < 0) {
                sheets.spreadsheets().values()
                        .append(properties.getSpreadsheetId(), range("A1"), body)
                        .setValueInputOption("RAW")
                        .setInsertDataOption("INSERT_ROWS")
                        .execute();
                log.info("Registration {} appended for {}", registration.regId(), registration.email());
            } else {
                int rowNumber = FIRST_DATA_ROW + index;
                sheets.spreadsheets().values()
                        .update(properties.getSpreadsheetId(), range("A" + rowNumber), body)
                        .setValueInputOption("RAW")
                        .execute();
                log.info("Registration {} updated in place at row {} for {}",
                        registration.regId(), rowNumber, registration.email());
            }
        } catch (IOException e) {
            throw new PublishFailedException(
                    "Could not write registration " + registration.regId() + " to the Google Sheet", e);
        }
    }

    @Override
    public Optional<Registration> findByEmail(String email) {
        try {
            List<List<Object>> rows = readDataRows();
            int index = SheetRows.indexOfEmail(rows, email);
            return index < 0 ? Optional.empty() : Optional.of(SheetRows.toRegistration(rows.get(index)));
        } catch (IOException e) {
            throw new PublishFailedException("Could not read the Google Sheet while looking up " + email, e);
        }
    }

    @Override
    public List<Registration> loadAll() {
        try {
            List<Registration> loaded = new ArrayList<>();
            for (List<Object> row : readDataRows()) {
                if (!SheetRows.cell(row, SheetRows.EMAIL).isBlank()) {
                    loaded.add(SheetRows.toRegistration(row));
                }
            }
            return loaded;
        } catch (IOException e) {
            throw new PublishFailedException("Could not read existing registrations from the Google Sheet", e);
        }
    }

    private List<List<Object>> readDataRows() throws IOException {
        ValueRange response = sheets.spreadsheets().values()
                .get(properties.getSpreadsheetId(), range("A" + FIRST_DATA_ROW + ":G"))
                .execute();
        return response.getValues() == null ? List.of() : response.getValues();
    }

    private String range(String cells) {
        return "'" + tab.replace("'", "''") + "'!" + cells;
    }

    /** Writes the header row once, if the tab is still empty. */
    void ensureHeaderRow() {
        try {
            ValueRange existing = sheets.spreadsheets().values()
                    .get(properties.getSpreadsheetId(), range("A1:G1"))
                    .execute();
            if (existing.getValues() == null || existing.getValues().isEmpty()) {
                sheets.spreadsheets().values()
                        .update(properties.getSpreadsheetId(), range("A1"),
                                new ValueRange().setValues(List.of(SheetRows.HEADER)))
                        .setValueInputOption("RAW")
                        .execute();
                log.info("Wrote header row to sheet tab '{}'", tab);
            }
        } catch (IOException e) {
            // Not fatal: a missing header only affects readability, not capture.
            log.warn("Could not verify the header row on tab '{}': {}", tab, e.getMessage());
        }
    }
}
