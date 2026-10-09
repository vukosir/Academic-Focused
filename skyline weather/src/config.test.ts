import { describe, expect, it } from 'vitest';
import { buildConfig } from './config';

describe('buildConfig', () => {
  it('uses the free Open-Meteo endpoints when no key is set', () => {
    const config = buildConfig({});
    expect(config.apiKey).toBeUndefined();
    expect(config.endpoints.forecast).toBe('https://api.open-meteo.com/v1/forecast');
    expect(config.endpoints.geocoding).toBe('https://geocoding-api.open-meteo.com/v1/search');
    expect(config.endpoints.airQuality).toBe('https://air-quality-api.open-meteo.com/v1/air-quality');
  });

  it('switches to the customer endpoints when a key is configured', () => {
    const config = buildConfig({ VITE_OPEN_METEO_API_KEY: 'test-key' });
    expect(config.apiKey).toBe('test-key');
    expect(config.endpoints.forecast).toBe('https://customer-api.open-meteo.com/v1/forecast');
    expect(config.endpoints.airQuality).toBe('https://customer-air-quality-api.open-meteo.com/v1/air-quality');
  });

  it('reads the default location and falls back when values are invalid', () => {
    const custom = buildConfig({
      VITE_DEFAULT_LOCATION_NAME: 'Cape Town',
      VITE_DEFAULT_LOCATION_LAT: '-33.92',
      VITE_DEFAULT_LOCATION_LON: '18.42',
    });
    expect(custom.defaultPlace).toMatchObject({ name: 'Cape Town', latitude: -33.92, longitude: 18.42 });

    const broken = buildConfig({ VITE_DEFAULT_LOCATION_LAT: 'north', VITE_REQUEST_TIMEOUT_MS: '-5', VITE_FORECAST_CACHE_MINUTES: '' });
    expect(broken.defaultPlace.latitude).toBe(-25.74486);
    expect(broken.requestTimeoutMs).toBe(10_000);
    expect(broken.forecastTtlMs).toBe(600_000);
  });
});
