# The Legendary Festival

Registration site for the festival in honour of Srila Prabhupada —
**Saturday, December 5, 2026, 10:00 AM–8:00 PM**.

Two HTML pages and one Google Apps Script. No server, no build, no dependencies.

- **Registration** — https://abinashsa.github.io/prabhpada-festival/
- **Organizer desk** — https://abinashsa.github.io/prabhpada-festival/admin.html

## How it works

```
index.html  ──┐
              ├── POST ──▶  Apps Script web app  ──▶  Google Sheet
admin.html  ──┘              (runs as the sheet's owner)
```

| File | What it is |
| --- | --- |
| `index.html` | The public registration form |
| `admin.html` | Headcount and the email blast, behind a key you type |
| `apps-script/Code.gs` | The script bound to the sheet — the only thing that touches it |

The sheet is **private**: never shared, never published. Only the script reads or
writes it, and the script runs as its owner.

## The three actions

| Action | Needs a key? | What it does |
| --- | --- | --- |
| `register` | No | Adds or updates **one** row, returns only that row |
| `list` | Yes | Returns every row — the roster |
| `email` | Yes | Writes to every registrant, one message each |

`register` needs no key on purpose: that is what makes publishing the form safe.
The worst a stranger can do is add a junk row — delete it in the sheet. They
cannot read the roster or send mail.

`admin.html` **asks** for the key and keeps it for that browser tab only. It is
never written into the page. A published key would let anyone who found the URL
mail every registrant from the owner's Gmail, so the publish workflow fails if it
finds one.

## Registering twice

Email is the identity. Registering again with an address already on the list
updates that row — same confirmation id, same original sign-up time, new details —
rather than adding a second row. Addresses are matched ignoring case and spacing,
so `Radha@Example.org ` and `radha@example.org` are one person.

## Editing the sheet by hand

Go ahead. The desk reads the sheet live, so hand-added rows are counted and hand-
edited ones are respected. Keep the columns in order: Confirmation ID, Submitted
At, Full Name, Email, Phone, Attending, Note.

## Changing the pages

Edit `index.html` or `admin.html` and push to `main`. The workflow in
`.github/workflows/pages.yml` publishes them; there is nothing to build. Open
either file directly in a browser to try a change before pushing — they talk to
the live script from `file://` too.

## Changing how registration behaves

That lives in `apps-script/Code.gs`, not in the pages. To deploy a change:

1. Open the sheet → *Extensions* → *Apps Script*
2. Paste in `apps-script/Code.local.gs` (same file, real secrets) and **save**
3. *Deploy* → *Manage deployments* → pencil → **Version: New version** → *Deploy*

The committed `Code.gs` carries placeholders. The real `SECRET` and `ADMIN_KEY`
live in `Code.local.gs` and `local-secrets.txt`, both gitignored.

**Setting it up from scratch:** create the script bound to the sheet, set `SECRET`
and `ADMIN_KEY`, deploy as a **Web app** with *Execute as: **Me*** and *Who has
access: **Anyone***, then put that `/exec` URL into `SCRIPT_URL` at the top of the
script block in both pages. Run `authorizeMail` once from the editor to approve
sending mail — updating code never re-asks for permissions.

## Gotchas worth knowing

- `curl -L` cannot test the script. A POST to `/exec` answers 302 to a content
  URL that must be fetched with GET; curl re-posts and gets `411`. Use Python or
  a browser.
- Pages must stay on **Source: GitHub Actions**. On "Deploy from a branch" Jekyll
  renders `README.md` as the homepage instead of the form.
- Mail is capped near 100 recipients a day on a consumer Gmail account. The desk
  reports what is left after each send.

## Sending a message to everyone

Open the desk, enter the key, write a subject and message, then *Review before
sending*. It shows who it reaches and that it cannot be undone. Each person is
written to separately — nobody sees anyone else's address.
