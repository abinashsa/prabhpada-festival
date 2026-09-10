package com.prabhupada.festival;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Writes to the sheet through a Google Apps Script web app bound to it, so the
 * spreadsheet stays private and no Google credentials are needed here — the
 * script runs as its owner. The shared secret keeps the deployment URL, which is
 * reachable by anyone who has it, from accepting rows from strangers.
 */
public class AppsScriptPublisher implements RegistrationPublisher {

    private static final Logger log = LoggerFactory.getLogger(AppsScriptPublisher.class);

    private final AppsScriptClient script;

    public AppsScriptPublisher(AppsScriptClient script) {
        this.script = script;
    }

    @Override
    public void upsert(Registration registration) {
        ObjectNode request = script.request("upsert");
        request.set("row", script.json().valueToTree(SheetRows.asRow(registration)));
        JsonNode response = script.call(request, "write registration " + registration.regId());
        log.info("Registration {} {} via Apps Script", registration.regId(),
                response.path("updated").asBoolean(false) ? "updated in place" : "appended");
    }

    @Override
    public Optional<Registration> findByEmail(String email) {
        List<List<Object>> rows = rowsFrom(script.call(script.request("list"), "look up " + email));
        int index = SheetRows.indexOfEmail(rows, email);
        return index < 0 ? Optional.empty() : Optional.of(SheetRows.toRegistration(rows.get(index)));
    }

    @Override
    public List<Registration> loadAll() {
        List<Registration> loaded = new ArrayList<>();
        for (List<Object> row : rowsFrom(script.call(script.request("list"), "read existing registrations"))) {
            if (!SheetRows.cell(row, SheetRows.EMAIL).isBlank()) {
                loaded.add(SheetRows.toRegistration(row));
            }
        }
        return loaded;
    }

    private List<List<Object>> rowsFrom(JsonNode response) {
        List<List<Object>> rows = new ArrayList<>();
        for (JsonNode row : response.path("rows")) {
            List<Object> cells = new ArrayList<>();
            row.forEach(cell -> cells.add(cell.isNull() ? "" : cell.asText()));
            rows.add(cells);
        }
        return rows;
    }
}
