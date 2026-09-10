package com.prabhupada.festival;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * The shape of a registration as a spreadsheet row, shared by both ways of
 * reaching the sheet (the Sheets API, and the Apps Script web app).
 */
final class SheetRows {

    static final List<Object> HEADER = List.of(
            "Confirmation ID", "Submitted At", "Full Name", "Email", "Phone", "Attending", "Note");

    /** Column positions within a row, matching HEADER. */
    static final int ID = 0, SUBMITTED = 1, NAME = 2, EMAIL = 3, PHONE = 4, ATTENDING = 5, NOTE = 6;

    static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SheetRows() {
    }

    static List<Object> asRow(Registration r) {
        return List.of(
                blankIfNull(r.regId()),
                r.submittedAt() == null ? "" : TIMESTAMP.format(
                        LocalDateTime.ofInstant(r.submittedAt(), ZoneId.systemDefault())),
                blankIfNull(r.fullName()),
                blankIfNull(r.email()),
                blankIfNull(r.phone()),
                r.guestCount(),
                blankIfNull(r.message()));
    }

    static Registration toRegistration(List<Object> row) {
        return new Registration(
                cell(row, ID),
                cell(row, NAME),
                cell(row, EMAIL),
                cell(row, PHONE),
                parseCount(cell(row, ATTENDING)),
                cell(row, NOTE),
                parseTimestamp(cell(row, SUBMITTED)));
    }

    /** Position of the row holding this email, or -1. */
    static int indexOfEmail(List<List<Object>> rows, String email) {
        String wanted = Registration.emailKey(email);
        if (wanted.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (Registration.emailKey(cell(rows.get(i), EMAIL)).equals(wanted)) {
                return i;
            }
        }
        return -1;
    }

    /** Trailing empty cells are simply absent from the rows we are handed. */
    static String cell(List<Object> row, int column) {
        return column < row.size() && row.get(column) != null ? row.get(column).toString().trim() : "";
    }

    private static int parseCount(String value) {
        try {
            return Math.max(Integer.parseInt(value), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** A hand-edited cell may hold anything; an unreadable timestamp is not worth failing over. */
    private static Instant parseTimestamp(String value) {
        try {
            return LocalDateTime.parse(value, TIMESTAMP).atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String blankIfNull(String value) {
        return value == null ? "" : value;
    }
}
