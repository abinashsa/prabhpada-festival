package com.prabhupada.festival;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The row-matching rules, exercised without a live spreadsheet. */
class SheetRowMatchingTest {

    private static final List<List<Object>> ROWS = List.of(
            List.of("REG-0001", "2026-09-10 10:00:00", "Radha D", "radha@example.org", "555-0100", 2, "no onion"),
            List.of("REG-0002", "2026-09-10 11:00:00", "Gopal S", "Gopal@Example.ORG", "", 1),
            List.of("REG-0003", "2026-09-10 12:00:00", "Bhakta", "bhakta@example.org"));

    @Test
    void findsTheRowHoldingAnEmail() {
        assertThat(SheetRows.indexOfEmail(ROWS, "radha@example.org")).isZero();
        assertThat(SheetRows.indexOfEmail(ROWS, "bhakta@example.org")).isEqualTo(2);
    }

    @Test
    void matchesRegardlessOfCaseOrSurroundingSpace() {
        assertThat(SheetRows.indexOfEmail(ROWS, "  GOPAL@example.org ")).isEqualTo(1);
    }

    @Test
    void reportsNoMatchForAnUnknownOrBlankAddress() {
        assertThat(SheetRows.indexOfEmail(ROWS, "nobody@example.org")).isEqualTo(-1);
        assertThat(SheetRows.indexOfEmail(ROWS, "  ")).isEqualTo(-1);
        assertThat(SheetRows.indexOfEmail(List.of(), "radha@example.org")).isEqualTo(-1);
    }

    @Test
    void readsARowBackIntoARegistrationEvenWhenTrailingCellsAreMissing() {
        Registration r = SheetRows.toRegistration(ROWS.get(2));

        assertThat(r.regId()).isEqualTo("REG-0003");
        assertThat(r.email()).isEqualTo("bhakta@example.org");
        assertThat(r.phone()).isEmpty();
        assertThat(r.message()).isEmpty();
        assertThat(r.guestCount()).isEqualTo(1);
        assertThat(r.submittedAt()).isNotNull();
    }
}
