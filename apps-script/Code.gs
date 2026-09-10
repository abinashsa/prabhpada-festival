/**
 * Registration sink for The Legendary Festival.
 *
 * Bound to the registrations spreadsheet and deployed as a web app that runs as
 * you, so the sheet itself stays private — nothing is published to the web. The
 * Spring app posts here; the shared secret stops anyone else who learns the URL.
 *
 * Setup: Extensions > Apps Script, paste this in, set SECRET below, then
 * Deploy > New deployment > Web app, "Execute as: Me", "Who has access: Anyone".
 */

// Must match festival.sheets.webhook-secret in the app. Change it to a long random string.
const SECRET = 'CHANGE_ME_TO_A_LONG_RANDOM_STRING';

const HEADER = ['Confirmation ID', 'Submitted At', 'Full Name', 'Email', 'Phone', 'Attending', 'Note'];
const EMAIL_COLUMN = 4;   // 1-based: column D
const FIRST_DATA_ROW = 2;

function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);           // two people submitting at once must not interleave
  try {
    const request = JSON.parse(e.postData.contents);
    if (request.secret !== SECRET) {
      return reply({ error: 'unauthorized' });
    }
    const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheets()[0];
    ensureHeader(sheet);

    if (request.action === 'list') {
      return reply({ rows: dataRows(sheet) });
    }
    if (request.action === 'upsert') {
      return reply(upsert(sheet, request.row));
    }
    if (request.action === 'email') {
      return reply(emailEveryone(sheet, request.subject, request.body));
    }
    return reply({ error: 'unknown action: ' + request.action });
  } catch (err) {
    return reply({ error: String(err) });
  } finally {
    lock.releaseLock();
  }
}

/** Replaces the row for this email, or appends one if the address is new. */
function upsert(sheet, row) {
  const wanted = emailKey(row[EMAIL_COLUMN - 1]);
  const rows = dataRows(sheet);

  for (let i = 0; i < rows.length; i++) {
    if (emailKey(rows[i][EMAIL_COLUMN - 1]) === wanted) {
      sheet.getRange(FIRST_DATA_ROW + i, 1, 1, HEADER.length).setValues([row]);
      return { updated: true, row: FIRST_DATA_ROW + i };
    }
  }
  sheet.appendRow(row);
  return { updated: false, row: sheet.getLastRow() };
}

/**
 * Writes to every registered address, one message each — never a shared To or CC,
 * so registrants do not see each other's addresses. Duplicate addresses are
 * collapsed, so someone who registered twice is not written to twice.
 */
function emailEveryone(sheet, subject, body) {
  if (!subject || !body) {
    return { error: 'subject and body are both required' };
  }
  const seen = {};
  let sent = 0;
  const failed = [];

  dataRows(sheet).forEach(function (row) {
    const address = String(row[EMAIL_COLUMN - 1] || '').trim();
    const key = emailKey(address);
    if (!key || seen[key]) {
      return;
    }
    seen[key] = true;
    try {
      MailApp.sendEmail(address, subject, body);
      sent++;
    } catch (err) {
      failed.push(address + ': ' + err);
    }
  });

  return {
    sent: sent,
    failed: failed,
    remainingQuota: remainingQuota()
  };
}

/** Reported when available; -1 rather than failing a send that already went out. */
function remainingQuota() {
  try {
    return MailApp.getRemainingDailyQuota();
  } catch (err) {
    return -1;
  }
}

/**
 * Run this once from the editor (Run > authorizeMail) to grant the mail permission.
 * Updating the code does not re-ask for it, so sending fails until this is approved.
 */
function authorizeMail() {
  Logger.log('Mail sends left today: ' + MailApp.getRemainingDailyQuota());
}

function dataRows(sheet) {
  const lastRow = sheet.getLastRow();
  if (lastRow < FIRST_DATA_ROW) {
    return [];
  }
  return sheet.getRange(FIRST_DATA_ROW, 1, lastRow - FIRST_DATA_ROW + 1, HEADER.length)
      .getDisplayValues();
}

function ensureHeader(sheet) {
  if (sheet.getLastRow() === 0) {
    sheet.getRange(1, 1, 1, HEADER.length).setValues([HEADER]).setFontWeight('bold');
    sheet.setFrozenRows(1);
  }
}

/** Addresses differing only by case or padding are the same person. */
function emailKey(value) {
  return String(value == null ? '' : value).trim().toLowerCase();
}

function reply(payload) {
  return ContentService.createTextOutput(JSON.stringify(payload))
      .setMimeType(ContentService.MimeType.JSON);
}
