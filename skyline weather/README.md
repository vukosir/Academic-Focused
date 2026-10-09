# Skyline Weather

A weather dashboard that shows current conditions, the next 24 hours, a 7-day forecast, air quality, precipitation chances, and sun and moon information for any place in the world. Built with React, TypeScript and Vite. All data is live from the Open-Meteo API.

## Put it online (no terminal needed)

The project includes a GitHub Actions workflow that builds the app and publishes it to GitHub Pages. Once it is set up, the app is public at `https://<your-username>.github.io/<repository-name>/` and you never have to start a server.

1. On github.com, create a new **public** repository. Leave "Add a README" unticked.
2. On the empty repository page, choose "uploading an existing file".
3. Unzip the project, open the folder, select everything inside it (including the `.github` folder) and drag it onto the upload page. Do not upload a `node_modules` or `dist` folder if you have one.
4. Choose "Commit changes".
5. Go to Settings, then Pages. Under "Build and deployment", set Source to "GitHub Actions".
6. Open the Actions tab. If no run has started, select "Deploy to GitHub Pages" and choose "Run workflow". When the run turns green, the link to the live site is shown on the run page and under Settings, Pages.

Every later change to the `main` branch is tested, built and published automatically.

The app calls public APIs straight from the browser and needs no API key, so nothing secret is stored in the repository.

## Run it on your own computer (optional)

You need Node.js 22.12 or newer (Node 24 also works). Check your version with `node -v`.

```bash
npm install
npm run dev
```

Then open http://localhost:5173.

## Scripts

| Command | What it does |
| --- | --- |
| `npm run dev` | Starts the development server with hot reload on port 5173. |
| `npm run build` | Type-checks the project, then writes the production build to `dist/`. |
| `npm run preview` | Serves the production build on port 4173 so you can check it. |
| `npm test` | Runs the test suite once. |
| `npm run test:watch` | Runs the tests again whenever a file changes. |
| `npm run test:coverage` | Runs the tests and prints a coverage report. |
| `npm run typecheck` | Type-checks without building. |

## Configuration and API key

Configuration comes from environment variables, which Vite reads from `.env` files. Nothing is hardcoded.

1. Copy `.env.example` to `.env.local`.
2. Change the values you need.
3. Restart `npm run dev`.

`.env.local` is ignored by git, so it never ends up in the repository.

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_OPEN_METEO_API_KEY` | empty | Optional. Open-Meteo is free without a key for non-commercial use. If you have a commercial key, set it here and the app switches to the `customer-` endpoints and sends the key with each request. |
| `VITE_DEFAULT_LOCATION_NAME` | `Pretoria` | Name of the place used by "Reset to default". |
| `VITE_DEFAULT_LOCATION_REGION` | `Gauteng` | Region shown next to the name. |
| `VITE_DEFAULT_LOCATION_COUNTRY` | `South Africa` | Country shown next to the name. |
| `VITE_DEFAULT_LOCATION_LAT` | `-25.74486` | Latitude of the default place. |
| `VITE_DEFAULT_LOCATION_LON` | `28.18783` | Longitude of the default place. |
| `VITE_REQUEST_TIMEOUT_MS` | `10000` | How long a request may take before it counts as timed out. |
| `VITE_FORECAST_CACHE_MINUTES` | `10` | How long a forecast is reused before it is fetched again. |

A note on keys in front-end apps: any variable starting with `VITE_` is compiled into the JavaScript that the browser downloads, so visitors can read it. That is acceptable for a key that is restricted to your domain. For a key that must stay private, put a small server or serverless function in front of the API and call that instead.

## Features

**Search**

- Search by city name, postal or ZIP code, or coordinates such as `-25.75, 28.19` or `25.75S 28.19E`.
- Suggestions appear as you type. Input is debounced, so one request is sent when typing pauses.
- Full keyboard support: arrow keys move through suggestions, Enter selects, Escape closes.
- Recent searches are saved in the browser and shown when the search field is empty.
- "Use my location" uses the browser geolocation API.
- A clear button empties the field, and the home button returns to the default location.
- Save places with the star and switch between them from the bar under the header.

**Weather**

- Current temperature, feels like, conditions, humidity, wind speed and direction, gusts, pressure, visibility, UV index, dew point, cloud cover, sunrise and sunset.
- Next 24 hours in a scrollable strip with a temperature line.
- 7 days with highs, lows and chance of rain.
- Air quality index with individual pollutants.
- Hourly precipitation chart.
- Moon phase, with the dates of the next full and new moon.

**Experience**

- The background changes with the weather and time of day: clear, cloudy, fog, rain, snow, thunderstorm, night, and golden hour.
- Light and dark themes. The app follows the system setting until you choose one.
- Separate switches for °C or °F and for metric or imperial units. Both are remembered.
- Works from 320 px phones up to wide desktops.
- Respects the "reduce motion" system setting.

## How errors are handled

Every failure is converted to one `AppError` type with a `kind`, and each kind has its own message. Raw error text and stack traces are never shown.

| Situation | What the user sees |
| --- | --- |
| No connection | "You are offline", plus a banner while the browser is offline. |
| Request fails to connect | "Could not reach the weather service" with a retry button. |
| No reply in time | "The weather service took too long" with a retry button. |
| HTTP 429 | "Too many requests", with the wait time when the server provides one. |
| HTTP 5xx | "The weather service is having problems" with a retry button. |
| Unknown place | "No place matches ..." with tips on what to try. |
| Response in the wrong shape | "The reply could not be read" with a retry button. |
| Some fields missing | The app shows what it has and marks the rest as not reported. |
| Location permission denied | An explanation, and focus moves to the search field. |
| A panel crashes while drawing | Only that panel shows a message and a reload button. |

Other behaviour worth knowing:

- Network and server errors are retried once automatically. Rate limits are never retried automatically.
- If a refresh fails while a forecast is already on screen, the forecast stays and a note explains that it is from earlier.
- The last forecast for each place is kept in the browser, so it can still be shown after a reload when the network is down.
- Air quality is loaded separately. If it fails, the rest of the page is unaffected.

## Project structure

```
src/
  api/            Everything that talks to the network
    http.ts         fetch wrapper: timeout, cancellation, retries, error classification
    errors.ts       AppError and the user-facing wording for each kind of failure
    cache.ts        Time-based cache with request sharing and stale fallback
    geocoding.ts    Place search, coordinate parsing, reverse lookup
    forecast.ts     Forecast request and response normalisation
    airQuality.ts   Air quality request and response normalisation
    parse.ts        Helpers for reading untrusted JSON safely
  components/     UI components, one per file
  hooks/          Data loading, debouncing, geolocation, online status
  lib/            Pure helpers: units, time, weather codes, scene, moon, scales, storage
  state/          App state: reducer, persistence, React context
  styles/         CSS: tokens, base, scenes, layout, components
  test/           Test setup and recorded API responses used by tests
  App.tsx         Page layout and wiring
  main.tsx        Entry point
  config.ts       Environment variables
  types.ts        Shared types
```

## How it works

**State.** User choices (selected place, units, theme, favorites, history) live in one reducer in `src/state/reducer.ts`, exposed through React context and saved to `localStorage`. Loading and error state for each request lives in the `useResource` hook, which is a small state machine: idle, loading, success, error.

**Data flow.** A component asks for a forecast through `useForecast(place)`. The hook calls `getForecast`, which checks the cache, calls the API if needed, and converts the raw response into the app's own types. Components only ever see those types.

**Units.** Data is always requested and stored in metric. Conversion happens when values are displayed, so switching units is instant and makes no request.

**Caching.** Forecasts are cached for 10 minutes, air quality for 30 minutes and place searches for a day. Two requests for the same thing at the same time share one network call.

**Times.** The API returns times in the timezone of the place. The helpers in `src/lib/time.ts` format them without converting to the viewer's timezone, so "sunrise 05:38" always means 05:38 at that place.

## Testing

```bash
npm test
```

The suite uses Vitest and React Testing Library. It covers:

- `http.ts`: success, network failure, offline, timeout, 429, 5xx, 400, 404, invalid JSON, cancellation, retries.
- `cache.ts`: freshness, expiry, forced refresh, shared requests, stale fallback, eviction, persistence.
- `geocoding.ts`: coordinate parsing, empty results, unknown places, reverse lookup fallback.
- `forecast.ts` and `airQuality.ts`: full, partial and malformed responses.
- `errors.ts`: every kind of failure has distinct wording and never leaks internal text.
- Units, time, weather codes, scenes, moon phase and index scales.
- The reducer and `localStorage` persistence, including corrupted stored values.
- The whole app with the network mocked: first run, search, favorites, unit switching, and each failure state.

The files in `src/test/fixtures.ts` are real responses recorded from the Open-Meteo API. They are used only by tests. The running app has no mock data.

## Data sources

- Forecast, geocoding and air quality: [Open-Meteo](https://open-meteo.com/), licensed CC BY 4.0. The free tier is for non-commercial use.
- Names for raw coordinates (used by "Use my location"): BigDataCloud's free client-side reverse geocoding endpoint. If it is unavailable the app shows the coordinates instead.
- Moon phase: calculated in the browser in `src/lib/moon.ts`, because Open-Meteo does not provide it.

## Other hosts

`npm run build` produces static files in `dist/`. That folder can be uploaded to any static host such as Netlify, Vercel or Cloudflare Pages. Asset paths are relative, so the site works at a domain root or in a sub-folder. Geolocation only works on `https` pages and on `localhost`.

## Browser support

Current versions of Chrome, Edge, Firefox and Safari.
