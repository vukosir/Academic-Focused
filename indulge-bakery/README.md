# Indulge Cakes Bakery

The website for Indulge Cakes Bakery, custom cakes made to order by Innocentia N in Johannesburg & Pretoria. It is a [TanStack Start](https://tanstack.com/start) app (React 19, plain JavaScript) with server-rendered pages, an order-request form, an owner's orders inbox, its own email/password auth and a JSON-file database. It runs on plain Node.js; no external platform is required.

## Run locally

Requires Node.js 20 or newer.

```bash
npm install
cp .env.example .env
npm run dev              # http://localhost:3000
```

## The owner's orders inbox

Order requests sent from `/order` are saved to the database and listed at `/admin`.

1. The owner registers at `/register` like any customer.
2. On the server, make that account the admin: `npm run admin -- grant innocentian18@gmail.com` (also `revoke <email>` and `list`). Signing up never makes anyone an admin, because emails aren't verified.
3. The owner turns on two-step verification under My orders > Security (any authenticator app). The inbox stays locked until it is on.
4. Open `/admin` to see requests, sorted by the date needed. Photos customers attached show as thumbnails; click one to open it full size. You can WhatsApp the customer in one tap, change an order's status (new → quoted → confirmed → completed / cancelled), keep a private note, or delete it.

There is no email service wired in, so new orders don't send notifications. Check `/admin`. The form also offers customers a prefilled WhatsApp message.

## Design

The design was made with Base44's AI builder (app "Indulge Cakes Bakery" in the owner's Base44 account) and ported into this project: a champagne-minimalist look with Inter / Inter Tight and Instrument Serif italics, an editorial hero with a ribbed champagne backdrop, light and dark range sections, and a dark owner's inbox. It runs entirely here; nothing depends on Base44.

- **Layout**: `src/routes/_site.jsx` (header, footer, order bag), `src/components/site/` (Hero, SiteHeader with search and bag, RangeFeature, CollectionsRail, QuoteForm, …). Hero styles are in `src/styles/hero.css`; tokens in `src/index.css`.
- **Order bag** (`src/components/site/BagProvider.jsx`): cake range, option, palette and quantity, kept in the visitor's browser and sent with the quote request on `/order`. No payment is taken online.
- **Pages**: home, `/cakes`, `/cakes/<range>`, `/collections`, `/collections/<occasion>`, `/compare`, `/gallery`, `/about`, `/support`, `/order`, `/privacy`, `/terms`; `/dashboard` (a customer's own requests) and `/admin` (the owner's inbox).

## Editing the content

- **Contact details and site copy**: `src/lib/site.js` (`BAKERY`, navigation, how-to-order steps, FAQs).
- **Cakes, prices and photos**: `src/lib/cakes.js`. `CAKE_RANGES` holds each range (prices, options, features, photos), `COLLECTIONS` groups ranges by occasion, `PALETTES` are the colour choices, `GALLERY` lists the gallery. Only show prices the bakery has published.
- **Photos** live in `public/images/cakes/<name>.jpg` with 480px copies in `public/images/cakes/sm/`. They are stills from the bakery's TikTok videos. To add one, put both files there and reference it in `src/lib/cakes.js` (add the name to `FILES`).
- **Colours and fonts**: `src/index.css` (`:root` tokens) and `tailwind.config.js`.

## Build and run in production

```bash
npm run build
AUTH_SECRET=<32+ random characters> APP_URL=https://<your domain> npm start
```

| Variable | Purpose |
| --- | --- |
| `AUTH_SECRET` | At least 32 random characters. Signs sessions and encrypts two-step secrets. **Required in production.** |
| `APP_URL` | Public site URL (`https://` in production), used in password-reset links and the HTTPS redirect. |
| `DATA_DIR` | Where `db.json`, customers' photos (`uploads/`) and the security log (`logs/`) live (default `./data`). Put it on a persistent, private, backed-up volume. |
| `PORT` | HTTP port (default 3000). |
| `TRUST_PROXY` | `1` only behind a CDN or reverse proxy that sets `X-Forwarded-For` / `X-Forwarded-Proto`. |
| `RESET_LINKS_TO_LOG` | `1` to print password-reset links to the server log in production (no email service yet). |

Security is described in [SECURITY.md](SECURITY.md): what is protected, the host settings to do before going live (HTTPS, CDN/WAF, backups) and known limits. `npm run security:check` runs the automated checks against a local production build; `npm run audit:deps` checks dependencies.

## How it fits together

- **Pages** are files in `src/routes/`. `_site/` pages are public (home, cakes, gallery, about, order, privacy); `_authed/` pages need a login (`/dashboard` = a customer's own orders, `/admin` = the orders inbox).
- **API**: server functions in `src/lib/server-fns.js` (create an order, list a customer's orders, and owner-only list/update/delete) and `src/lib/auth-fns.js` (register, login, logout, password reset).
- **Auth**: passwords are hashed with scrypt; sessions are stored on the server behind an httpOnly cookie; optional two-step verification (authenticator app codes and recovery codes), required for the owner. There is no email service: plug a provider into `requestPasswordReset` to email reset links (until then they are only logged in development, or with `RESET_LINKS_TO_LOG=1`).
- **Data**: `src/lib/db.server.js` is a small JSON-file store with `users`, `orders`, `resets` and `sessions`. It is fine for a single server instance.
- **Order photos**: the order form shrinks pictures to JPEG (max 1600px) in the browser and sends up to 5 with the order. They are saved in `DATA_DIR/uploads/` (`src/lib/uploads.server.js`) and served only to the owner or the customer who sent them by `src/routes/api/order-photos.$name.js`. Deleting an order deletes its photos.
