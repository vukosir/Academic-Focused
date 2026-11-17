# AGENTS.md

## Project Context

Vukosi Rikhotso's personal career website (portfolio, Pretoria): a **TanStack Start** app (React 19, plain JavaScript — no TypeScript) with file-based routes, server functions and server rendering. It is self-hosted: its own Node server, its own auth and its own JSON-file database. There is no third-party backend platform.

Start with `README.md` for setup, configuration and deployment.

## Key Files

- `src/routes/__root.jsx`: the HTML document (`<html>`, `<head>` via `HeadContent`, `<Scripts />`) and the providers. There is **no `index.html`**.
- `src/routes/*.jsx`: one page per file (`index.jsx` → `/`, `posts.$id.jsx` → `/posts/$id`). `src/routes/_site/` holds public pages inside the site layout; `src/routes/_authed/` holds pages that need a session. `src/routeTree.gen.ts` is generated on every dev/build run — never edit or commit it.
- `src/lib/site.js` (`PROFILE`, nav, education, strengths, FAQs) and `src/lib/work.js` (`PROJECTS`, `SKILLS`, `LANGUAGES`, `GALLERY`): the site's content. Facts come from Vukosi's CV; publish only email, LinkedIn and GitHub as contact details (never the phone number or referees' details).
- `src/components/site/`: the site design (from the Indulge Cakes Bakery build). `ContactForm.jsx` sends messages through `sendMessage`. `src/components/admin/` is the owner's messages inbox UI.
- `src/lib/server-fns.js`: server functions (`createServerFn`) for contact messages: `sendMessage` (public) and admin-only `listMessages`, `updateMessage`, `deleteMessage`.
- `src/lib/auth-fns.js`: `register`, `login` (+ `verifyMfaLogin`), `logout`, `getMe`, `requestPasswordReset`, `resetPassword`, two-step verification (`mfaBeginSetup`, `mfaConfirmSetup`, `mfaDisable`, `mfaStatus`) and `listMySessions` / `signOutOtherDevices`. Sessions are server-side (`src/lib/session.server.js`); the cookie holds a random token whose hash is stored.
- `src/lib/auth-middleware.js`: `requireUser` (server functions) and `requireUserRoute` (server routes); both set `context.user`.
- `src/lib/db.server.js`: the JSON-file store (`find`, `findOne`, `insert`, `update`, `remove`). `src/lib/auth.server.js`: password hashing and rules, tokens, TOTP, encryption. `rate-limit.server.js`, `audit.server.js` (security log), `maintenance.server.js` (data clean-up), `request-meta.server.js` (client IP). `*.server.js` files must only be imported lazily from server code (`await import(...)`).
- `src/lib/AuthContext.jsx`: client-side session (`useAuth()`: `user`, `isAuthenticated`, `isLoadingAuth`, `logout`, `navigateToLogin`).
- `src/components/auth/`: login, register and password-reset pages and the login gate (`AuthGate.jsx`).
- `src/components/ui/`: shadcn/ui kit. `src/index.css` + `tailwind.config.js`: design tokens.
- `data/db.json`: the JSON database (`users`, `messages`, `resets`, `sessions`). `data/logs/security.log`: the security audit log. `public/`: static assets; project images are in `public/images/work/<name>.jpg` with 480px copies in `public/images/work/sm/`.
- `server.js`: production server (`npm start`) serving `dist/client` and the built TanStack Start handler.

## Server Security

- Every server function and server route is a public HTTP endpoint: anyone can call it directly, with any input. A page's `beforeLoad`, redirect or hidden button protects nothing on the server.
- A server function that needs a signed-in caller adds `.middleware([requireUser])` and reads `context.user`. Then check that this user may touch the record (owner or `role === "admin"`) before reading or changing it.
- Validate input with `.validator(...)` (zod) — never trust ids, emails or roles sent by the caller. Bound strings and arrays with `.max()`.
- Never return `password_hash` or reset tokens; use `publicUser()` for anything sent to the browser.
- Secrets (`AUTH_SECRET`, …) come from environment variables, read in server code only. Never put a secret in `import.meta.env.VITE_*` — it is compiled into the browser bundle.
- Admin (`role === "admin"`) is granted only on the server with `npm run admin -- grant <email>`, never by sign-up; admin actions also require `mfa_enabled` (see `requireAdmin` in `src/lib/server-fns.js`).
- Rate-limit anything that can be abused (`hit()` from `src/lib/rate-limit.server.js`) and record security events with `audit()` from `src/lib/audit.server.js`; never log passwords, codes, tokens or full emails.
- Security headers, CSP nonces, request limits and error masking live in `src/start.js`; see SECURITY.md. After security-related changes run `npm run security:check` against a local production build.
- The app's own code calls the server only through server functions. A server route (`src/routes/api/*`) is for a URL something else requests (webhook, feed, public API).

## Working Notes

- `npm run dev` runs the Vite dev server on port 3000. `npm run build` emits `dist/` (never commit it); `npm start` serves it.
- Data flows through the router: `loader` + `Route.useLoaderData()` for reads, `useServerFn` + TanStack Query mutations for writes. The router creates the `QueryClient` per request (`src/router.jsx`); never share one across requests.
- `ssr: false` only on pages that need the signed-in user, so personal data never lands in cacheable HTML.
- Server and first client render must match: read `window`, `document` and browser storage only in effects and handlers, and format dates and numbers with an explicit locale and time zone.
- Run `npm run lint` before finishing code changes. Write JavaScript/JSX only — no `.ts`/`.tsx` files.
