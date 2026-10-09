/**
 * End-to-end behaviour of the app with the network mocked at fetch().
 * These cover the paths users actually take: first run, search, and each kind
 * of failure, checking that a helpful state is shown every time.
 */
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { clearAirQualityCache } from './api/airQuality';
import { clearForecastCache } from './api/forecast';
import { clearGeocodingCache } from './api/geocoding';
import App from './App';
import { config } from './config';
import { AppStateProvider } from './state/AppState';
import { airQualityResponse, forecastResponse, geocodingEmptyResponse, geocodingResponse, jsonResponse } from './test/fixtures';

type Responder = () => Response | Promise<Response>;

/** Route mocked requests by API host. Each responder can be swapped per test. */
function mockApi(overrides: Partial<Record<'forecast' | 'air' | 'geocoding', Responder>> = {}) {
  const responders: Record<'forecast' | 'air' | 'geocoding', Responder> = {
    forecast: () => jsonResponse(forecastResponse),
    air: () => jsonResponse(airQualityResponse),
    geocoding: () => jsonResponse(geocodingResponse),
    ...overrides,
  };
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const host = new URL(String(input)).hostname;
    if (host.startsWith('geocoding')) return responders.geocoding();
    if (host.startsWith('air-quality')) return responders.air();
    return responders.forecast();
  });
  vi.stubGlobal('fetch', fetchMock);
  return { fetchMock, responders };
}

function renderApp(preselected = false) {
  // Units default from the browser locale, so pin them for predictable assertions.
  window.localStorage.setItem('skyline:temperatureUnit', '"celsius"');
  window.localStorage.setItem('skyline:system', '"metric"');
  if (preselected) window.localStorage.setItem('skyline:place', JSON.stringify(config.defaultPlace));
  return render(
    <AppStateProvider>
      <App />
    </AppStateProvider>,
  );
}

/** The current-conditions section, which is labelled by the place name. */
const hero = () => screen.findByRole('region', { name: 'Pretoria' });
/** The first-run screen. */
const onboarding = () => screen.getByRole('region', { name: 'Where should we look at the sky?' });

beforeEach(() => {
  clearForecastCache();
  clearAirQualityCache();
  clearGeocodingCache();
});

describe('first run', () => {
  it('shows the onboarding prompt and makes no requests', () => {
    const { fetchMock } = mockApi();
    renderApp();
    expect(screen.getByRole('heading', { name: 'Where should we look at the sky?' })).toBeInTheDocument();
    expect(within(onboarding()).getByRole('button', { name: 'Use my location' })).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('loads the default place from the onboarding screen', async () => {
    mockApi();
    renderApp();
    await userEvent.click(screen.getByRole('button', { name: `Show ${config.defaultPlace.name}` }));
    expect(await screen.findByRole('heading', { name: 'Pretoria', level: 1 })).toBeInTheDocument();
    expect(within(await hero()).getByText('Partly cloudy')).toBeInTheDocument();
    expect(screen.getByText('Temperature: 26 degrees Celsius')).toBeInTheDocument();
  });

  it('explains what to do when location access is denied', async () => {
    mockApi();
    vi.stubGlobal('isSecureContext', true);
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: { getCurrentPosition: (_ok: unknown, fail: (e: { code: number }) => void) => fail({ code: 1 }) },
    });
    renderApp();
    await userEvent.click(within(onboarding()).getByRole('button', { name: 'Use my location' }));
    expect(await screen.findByText(/Location access is blocked for this site/)).toBeInTheDocument();
    // The fallback is manual search, so the search field takes focus.
    expect(screen.getByRole('combobox', { name: 'Search for a place' })).toHaveFocus();
  });
});

describe('forecast', () => {
  it('renders every panel from live-shaped data', async () => {
    mockApi();
    renderApp(true);
    for (const title of ['Next 24 hours', '7-day forecast', 'Right now', 'Precipitation', 'Air quality', 'Sun and moon']) {
      expect(await screen.findByRole('heading', { name: title })).toBeInTheDocument();
    }
    const airQuality = screen.getByRole('region', { name: 'Air quality' });
    expect(await within(airQuality).findByText('Moderate')).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: '7-day forecast' })).getAllByRole('listitem')).toHaveLength(7);
  });

  it('switches temperature units without another request', async () => {
    const { fetchMock } = mockApi();
    renderApp(true);
    await screen.findByText('Temperature: 26 degrees Celsius');
    const callsBefore = fetchMock.mock.calls.length;
    await userEvent.click(screen.getByRole('radio', { name: 'Fahrenheit' }));
    expect(screen.getByText('Temperature: 78 degrees Fahrenheit')).toBeInTheDocument();
    expect(fetchMock.mock.calls.length).toBe(callsBefore);
    expect(window.localStorage.getItem('skyline:temperatureUnit')).toBe('"fahrenheit"');
  });

  it('saves and removes a favorite place', async () => {
    mockApi();
    renderApp(true);
    await userEvent.click(await screen.findByRole('button', { name: 'Save Pretoria' }));
    const saved = screen.getByRole('navigation', { name: 'Saved places' });
    expect(within(saved).getByRole('button', { name: 'Pretoria' })).toHaveAttribute('aria-current', 'true');
    await userEvent.click(within(saved).getByRole('button', { name: 'Remove Pretoria from saved places' }));
    expect(screen.queryByRole('navigation', { name: 'Saved places' })).not.toBeInTheDocument();
  });
});

describe('failures', () => {
  it('shows a rate limit message, then recovers when the user retries', async () => {
    const { responders } = mockApi({ forecast: () => jsonResponse({ error: true, reason: 'limit' }, { status: 429 }) });
    renderApp(true);
    expect(await screen.findByText('Too many requests')).toBeInTheDocument();

    responders.forecast = () => jsonResponse(forecastResponse);
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(within(await hero()).getByText('Partly cloudy')).toBeInTheDocument();
    expect(screen.queryByText('Too many requests')).not.toBeInTheDocument();
  });

  it.each([
    ['a server error', () => jsonResponse('oops', { status: 503 }), 'The weather service is having problems'],
    ['a network failure', () => Promise.reject(new TypeError('Failed to fetch')), 'Could not reach the weather service'],
    ['a malformed body', () => jsonResponse({ unexpected: true }), 'The reply could not be read'],
  ])(
    'shows a specific message for %s',
    async (_label, forecast, title) => {
      mockApi({ forecast: forecast as Responder });
      renderApp(true);
      expect(await screen.findByText(title, {}, { timeout: 4000 })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
      // No raw error text leaks into the page.
      expect(document.body).not.toHaveTextContent(/TypeError|Failed to fetch|HTTP 503|oops/);
    },
    8000,
  );

  it('keeps the forecast on screen when only air quality fails', async () => {
    mockApi({ air: () => jsonResponse({}, { status: 429 }) });
    renderApp(true);
    expect(within(await hero()).getByText('Partly cloudy')).toBeInTheDocument();
    const airQuality = screen.getByRole('region', { name: 'Air quality' });
    expect(await within(airQuality).findByText('Too many requests')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '7-day forecast' })).toBeInTheDocument();
  });

  it('renders a partial forecast, marking what is missing', async () => {
    mockApi({ forecast: () => jsonResponse({ current: { time: '2026-10-09T19:00', temperature_2m: 18 }, utc_offset_seconds: 7200 }) });
    renderApp(true);
    expect(await screen.findByText('Temperature: 18 degrees Celsius')).toBeInTheDocument();
    expect(screen.getByText('The hourly forecast is not available for this place right now.')).toBeInTheDocument();
    expect(screen.getByText('The daily forecast is not available for this place right now.')).toBeInTheDocument();
    expect(screen.getAllByText('Not reported for this place').length).toBeGreaterThan(3);
  });
});

describe('search', () => {
  it('suggests places while typing and loads the chosen one', async () => {
    const { fetchMock } = mockApi();
    renderApp();
    const input = screen.getByRole('combobox', { name: 'Search for a place' });
    await userEvent.type(input, 'Pretoria');

    const options = await screen.findAllByRole('option');
    expect(options[0]).toHaveTextContent('Pretoria');
    expect(options[0]).toHaveTextContent('Gauteng, South Africa');
    // Debounced: eight keystrokes, one geocoding request.
    const geocodingCalls = fetchMock.mock.calls.filter(([url]) => String(url).includes('geocoding'));
    expect(geocodingCalls).toHaveLength(1);

    await userEvent.keyboard('{ArrowDown}{Enter}');
    expect(await screen.findByRole('heading', { name: 'Pretoria', level: 1 })).toBeInTheDocument();
    expect(JSON.parse(window.localStorage.getItem('skyline:history') ?? '[]')).toHaveLength(1);
  });

  it('explains an unknown place and keeps the page usable', async () => {
    mockApi({ geocoding: () => jsonResponse(geocodingEmptyResponse) });
    renderApp();
    const input = screen.getByRole('combobox', { name: 'Search for a place' });
    await userEvent.type(input, 'zzzxqqqjk{Enter}');

    expect(await screen.findByText('No place matches "zzzxqqqjk"')).toBeInTheDocument();
    expect(screen.getByText(/Check the spelling, or try a nearby larger city/)).toBeInTheDocument();
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('heading', { name: 'Where should we look at the sky?' })).toBeInTheDocument();
  });

  it('clears the field with the clear button', async () => {
    mockApi();
    renderApp();
    const input = screen.getByRole('combobox', { name: 'Search for a place' });
    await userEvent.type(input, 'Pre');
    await userEvent.click(screen.getByRole('button', { name: 'Clear search' }));
    expect(input).toHaveValue('');
    await waitFor(() => expect(input).toHaveFocus());
  });
});
