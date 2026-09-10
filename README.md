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
| `src/main/java/com/prabhupada/festival/` | Application, registration API, in-memory store |
| `src/test/java/com/prabhupada/festival/` | Tests, run over a real HTTP port |

## Registration API

The page's "Save Your Spot" form posts to the backend. The server assigns the
confirmation id and the donation amount (`$20` individual, `$50` family), so the
amount can't be set from the browser.

| Endpoint | Purpose |
| --- | --- |
| `POST /api/registrations` | Submit a registration; returns `201` with the saved record |
| `GET /api/registrations/{regId}` | Look up one registration |
| `GET /api/registrations` | List all registrations |

```
curl -X POST http://localhost:8080/api/registrations \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Radha D","email":"radha@example.org","type":"family","guestCount":4}'
```

## Before going live

- **Registrations are held in memory only** (`RegistrationStore`) and are lost on
  restart. Back it with a real datasource before taking real sign-ups.
- `GET /api/registrations` exposes every registrant's name, email and phone with
  no authentication. Put it behind a login, or remove it.
- The Venmo / PayPal / Zelle links in `index.html` are still placeholders.

## Tests

```
mvn test
```
