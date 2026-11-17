# Vukosi Rikhotso, portfolio

The personal career website of Vukosi Rikhotso, a software developer in Pretoria completing a Diploma in IT (Software Development) at Rosebank College. It shows Vukosi's projects, skills, education and strengths, and has a contact form whose messages land in a private inbox.

It is a [TanStack Start](https://tanstack.com/start) app (React 19, plain JavaScript) with server-rendered pages, its own email/password sign-in for the site owner, and a JSON-file database. It runs on plain Node.js; no external platform is required. The design is the champagne-minimalist design first built for the Indulge Cakes Bakery site, reused with new content.

## Run locally

Requires Node.js 20 or newer.

```bash
npm install
cp .env.example .env
npm run dev              # http://localhost:3000
```

## Pages

Home, `/projects`, `/projects/<slug>`, `/skills`, `/skills/<slug>`, `/compare`, `/gallery`, `/about`, `/faq`, `/contact` and `/privacy`. Owner only: `/admin` (messages inbox) and `/account` (security settings). `/login`, `/register`, `/forgot-password` and `/reset-password` are not linked from the public pages.

## Editing the content

- **Profile, contact details, navigation, education, strengths and FAQs**: `src/lib/site.js` (`PROFILE`, `NAV_LINKS`, `FOOTER_LINKS`, `HOW_STEPS`, `EDUCATION`, `COMPETENCIES`, `FAQS`). Contact details are limited to email, LinkedIn and GitHub on purpose: the phone number and referees' details are not published.
- **Projects, skills, languages and gallery**: `src/lib/work.js`. `PROJECTS` holds each project (summary, highlights, features, stack, links, images), `SKILLS` groups projects by skill, `LANGUAGES` are the coloured language chips, and `GALLERY` is built from every project's images.
- **Images** live in `public/images/work/<name>.jpg` (960×1200) with 480px copies in `public/images/work/sm/`. They are screenshots of each app; StudyTime uses a cover in the app's own colours because it has no screenshots yet. To add one, put both sizes there and reference it with `img("<name>", "<alt text>")` in `src/lib/work.js`.
- **Colours and fonts**: `src/index.css` (`:root` tokens) and `tailwind.config.js`.

## The messages inbox

Messages sent from `/contact` are saved to the database and listed at `/admin`.

1. Register at `/register` with your email.
2. On the server, make that account the admin: `npm run admin -- grant vukosir@icloud.com` (also `revoke <email>` and `list`). Signing up never makes anyone an admin, because emails aren't verified.
3. Sign in and turn on two-step verification under Account > Security (any authenticator app). The inbox stays locked until it is on.
4. Open `/admin` to read messages, newest first. Reply by email in one click, mark a message as replied or archived, keep a private note, or delete it.

There is no email service wired in, so new messages don't send notifications. Check `/admin`.

## Build and run in production

```bash
npm run build
AUTH_SECRET=<32+ random characters> APP_URL=https://<your domain> npm start
```

| Variable | Purpose |
| --- | --- |
| `AUTH_SECRET` | At least 32 random characters. Signs sessions and encrypts two-step secrets. **Required in production.** |
| `APP_URL` | Public site URL (`https://` in production), used in password-reset links and the HTTPS redirect. |
| `DATA_DIR` | Where `db.json` and the security log (`logs/`) live (default `./data`). Put it on a persistent, private, backed-up volume. |
| `PORT` | HTTP port (default 3000). |
| `TRUST_PROXY` | `1` only behind a CDN or reverse proxy that sets `X-Forwarded-For` / `X-Forwarded-Proto`. |
| `RESET_LINKS_TO_LOG` | `1` to print password-reset links to the server log in production (no email service yet). |

Security is described in [SECURITY.md](SECURITY.md). `npm run security:check` runs the automated checks against a local production build; `npm run audit:deps` checks dependencies.

## How it fits together

- **Pages** are files in `src/routes/`. `_site/` pages are public; `_authed/` pages need a login (`/admin` = the messages inbox, `/account` = security settings).
- **API**: server functions in `src/lib/server-fns.js` (`sendMessage` for anyone, and owner-only `listMessages`, `updateMessage`, `deleteMessage`) and `src/lib/auth-fns.js` (register, login, logout, password reset, two-step verification).
- **Auth**: passwords are hashed with scrypt; sessions are stored on the server behind an httpOnly cookie; two-step verification (authenticator app codes and recovery codes) is required for the inbox.
- **Data**: `src/lib/db.server.js` is a small JSON-file store with `users`, `messages`, `resets` and `sessions`. It is fine for a single server instance.
