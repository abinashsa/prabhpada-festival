# The Legendary Festival

A Spring Boot web app for the festival page in honor of Srila Prabhupada.
The festival page is the application's start page — `GET /` serves it.

## Running

Requires JDK 21 and Maven.

```
mvn spring-boot:run
```

Then open http://localhost:8080 — the festival page loads at the root.

Build a runnable jar instead:

```
mvn clean package
java -jar target/legendary-festival-0.0.1-SNAPSHOT.jar
```

## Layout

| Path | What it holds |
| --- | --- |
| `src/main/resources/static/index.html` | The festival page, served as the welcome page at `/` |
| `src/main/java/com/prabhupada/festival/` | Application, registration API, in-memory store, Sheets publisher |
| `src/test/java/com/prabhupada/festival/` | Tests, run over a real HTTP port |

## Registration API

The page's "Save Your Spot" form posts to the backend, and the server assigns the
confirmation id. The form collects name, email, phone, headcount and a note.

Email is the unique key: submitting the same address again updates that
registration instead of creating a second one, and the confirmation screen says so.

The registration tier ("Registering As") and the donation total were removed from
the form for now, so the API no longer takes `type` or `amount`. The standalone
"Suggested Donation" section further up the page still shows the $20 / $50 figures.

| Endpoint | Purpose |
| --- | --- |
| `POST /api/registrations` | Submit a registration. `201` for a new one, `200` when an existing email was updated |
| `GET /api/registrations/{regId}` | Look up one registration |
| `GET /api/registrations` | List all registrations |

```
curl -X POST http://localhost:8080/api/registrations \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Radha D","email":"radha@example.org","guestCount":4}'
```

## Google Sheets

Registrations are written to a Google Sheet, **one row per email address**:

| Confirmation ID | Submitted At | Full Name | Email | Phone | Attending | Note |
|---|---|---|---|---|---|---|

Email is the identity. If someone registers again with an address already in the
sheet, that row is updated in place — same confirmation id, same original sign-up
time, new details — rather than a second row being added. Addresses are matched
ignoring case and surrounding spaces, so `Radha@Example.org ` and
`radha@example.org` are one person. On startup the app reads the sheet back in, so
a restart keeps ids stable instead of reissuing ones already in use.

### Route A — Apps Script web app (no credentials)

The same spirit as the sponsor board's published-CSV trick, but able to *write*
and without making the sheet public. A script bound to the sheet runs as you and
accepts rows over one URL.

1. Open the sheet → *Extensions* → *Apps Script*.
2. Paste in `apps-script/Code.gs` and set `SECRET` to a long random string.
   (`apps-script/Code.local.gs` is the same file with this project's secret
   already filled in — it is gitignored.)
3. *Deploy* → *New deployment* → **Web app**, "Execute as: **Me**",
   "Who has access: **Anyone**". Copy the `/exec` URL.
4. Put the URL and the matching secret in `application-local.properties`:

```
festival.sheets.webhook-url=https://script.google.com/macros/s/…/exec
festival.sheets.webhook-secret=<the same string as SECRET>
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

"Anyone" means anyone with the URL can reach the script, not that the sheet is
readable — the script only appends and updates rows, and rejects any request
without the secret. The spreadsheet itself is never shared or published.

### Route B — Sheets API (service account or gcloud)

It is **off by default**, so the app runs without credentials — it logs a warning
per registration and keeps them in memory only. To turn it on:

1. **Create the sheet.** A new Google Sheet. The header row and the tab are sorted
   out on first startup: the app uses a tab named `Registrations` if there is one,
   otherwise the first tab (a new sheet's `Sheet1`). The id is the long string in
   the URL: `docs.google.com/spreadsheets/d/`**`<SPREADSHEET_ID>`**`/edit`.
2. **Create a service account.** In the [Google Cloud console](https://console.cloud.google.com):
   pick or create a project → *APIs & Services* → enable the **Google Sheets API**
   → *Credentials* → *Create credentials* → *Service account*. Then open the
   account → *Keys* → *Add key* → *Create new key* → **JSON**, and save the file
   somewhere outside this repo.
3. **Share the sheet with it.** Copy the service account's email (it ends in
   `.iam.gserviceaccount.com`) and share the sheet with that address as an
   **Editor**. This is the step people miss — without it every write is a 403.
4. **Point the app at both:**

```
export FESTIVAL_SHEETS_ENABLED=true
export FESTIVAL_SHEETS_SPREADSHEET_ID=<the id from step 1>
export FESTIVAL_SHEETS_CREDENTIALS_PATH=/path/to/service-account.json
mvn spring-boot:run
```

Or put the same settings in `src/main/resources/application-local.properties`
(gitignored) and run with `-Dspring-boot.run.profiles=local`.

`FESTIVAL_SHEETS_TAB` overrides the tab name. If `..._CREDENTIALS_PATH` is left
unset, Application Default Credentials are used instead (`GOOGLE_APPLICATION_CREDENTIALS`,
or `gcloud auth application-default login`).

The key file is a live credential for that sheet: keep it out of the repo — the
`.gitignore` covers `*service-account*.json` and `*credentials*.json` — and off
any machine you don't control.

**If the sheet write fails**, the API returns `503` and the page shows its error
message rather than confirming. A registration is only confirmed once it is
actually in the sheet, so nothing is lost silently; the registrant retries.

## Publishing the registration page

`.github/workflows/pages.yml` publishes **only** `index.html` to GitHub Pages on
each push to `main`, rewriting the form to post straight to the Apps Script web
app. The admin page and the Spring app are deliberately not published: Pages
serves static files, so an admin key checked in the browser would protect
nothing.

The published page carries **no secret**. It calls the script's `register`
action, which only writes one row and returns that row — it cannot read the
roster or send mail, both of which still require the shared secret.

To switch it on:

1. Deploy the current `apps-script/Code.local.gs` (it adds `register`).
2. *Settings* → *Secrets and variables* → *Actions* → *Variables* → add
   **`APPS_SCRIPT_URL`** with the `/exec` URL.
3. *Settings* → *Pages* → *Source*: **GitHub Actions**.
4. Push to `main`.

**What a public form means:** anyone can submit a registration, as with any
sign-up page that has no login. They cannot read what others submitted. If junk
rows appear, delete them in the sheet — and remember the roster is still only
readable by you.

## Before going live

- **Turn the Google Sheet on** (above). Until then registrations live only in
  memory and are lost on restart.
- `GET /api/registrations` exposes every registrant's name, email and phone with
  no authentication. Put it behind a login, or remove it.
- Payment collection is gone from the confirmation screen along with the donation
  total; the old Venmo / PayPal / Zelle links were placeholders anyway.

## Tests

```
mvn test
```
