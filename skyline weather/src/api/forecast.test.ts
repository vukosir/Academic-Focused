import { beforeEach, describe, expect, it, vi } from 'vitest';
import { forecastResponse, jsonResponse } from '../test/fixtures';
import type { Place } from '../types';
import { buildForecastUrl, clearForecastCache, getForecast, normaliseForecast } from './forecast';

const pretoria: Place = { id: '-25.745,28.188', name: 'Pretoria', latitude: -25.74486, longitude: 28.18783 };

describe('buildForecastUrl', () => {
  it('asks for current, hourly and daily data in the place timezone', () => {
    const url = new URL(buildForecastUrl(pretoria));
    expect(url.origin + url.pathname).toBe('https://api.open-meteo.com/v1/forecast');
    expect(url.searchParams.get('latitude')).toBe('-25.7449');
    expect(url.searchParams.get('timezone')).toBe('auto');
    expect(url.searchParams.get('current')).toContain('temperature_2m');
    expect(url.searchParams.get('hourly')).toContain('precipitation_probability');
    expect(url.searchParams.get('daily')).toContain('sunrise');
    expect(url.searchParams.has('apikey')).toBe(false);
  });
});

describe('normaliseForecast', () => {
  it('maps a full response', () => {
    const forecast = normaliseForecast(forecastResponse, 123);
    expect(forecast.current).toMatchObject({
      temperature: 25.6,
      apparentTemperature: 24.9,
      isDay: false,
      weatherCode: 2,
      humidity: 39,
      windDirection: 251,
      pressure: 1018.7,
    });
    expect(forecast.utcOffsetSeconds).toBe(7200);
    expect(forecast.fetchedAt).toBe(123);
    expect(forecast.daily).toHaveLength(7);
    expect(forecast.daily[0]).toMatchObject({ date: '2026-10-09', tempMax: 30.1, tempMin: 17.4, sunrise: '2026-10-09T05:38' });
  });

  it('starts the hourly list at the current hour and keeps 24 hours', () => {
    const { hourly } = normaliseForecast(forecastResponse);
    expect(hourly).toHaveLength(24);
    expect(hourly[0]).toMatchObject({ time: '2026-10-09T19:00', temperature: 25.6, isDay: false });
    expect(hourly[23]?.time).toBe('2026-10-10T18:00');
  });

  it('reads visibility, UV and dew point from the current hour', () => {
    const { current } = normaliseForecast(forecastResponse);
    expect(current).toMatchObject({ visibility: 39460, uvIndex: 0, dewPoint: 10.8 });
  });

  it('turns missing or wrongly typed fields into null instead of failing', () => {
    const partial = {
      current: { time: '2026-10-09T19:00', temperature_2m: 20, relative_humidity_2m: 'high', wind_speed_10m: null },
      hourly: { time: ['2026-10-09T19:00', '2026-10-09T20:00'], temperature_2m: [20] },
      daily: { time: ['2026-10-09'], temperature_2m_max: [25] },
    };
    const forecast = normaliseForecast(partial);
    expect(forecast.current).toMatchObject({ temperature: 20, humidity: null, windSpeed: null, weatherCode: null, uvIndex: null });
    expect(forecast.hourly).toHaveLength(2);
    expect(forecast.hourly[1]).toMatchObject({ temperature: null, precipitationProbability: null });
    expect(forecast.daily[0]).toMatchObject({ tempMax: 25, tempMin: null, sunrise: null });
  });

  it('accepts a response with no hourly or daily blocks', () => {
    const forecast = normaliseForecast({ current: { temperature_2m: 12.5 } });
    expect(forecast.current.temperature).toBe(12.5);
    expect(forecast.hourly).toEqual([]);
    expect(forecast.daily).toEqual([]);
  });

  it.each([
    ['null', null],
    ['an array', []],
    ['no current block', { hourly: {} }],
    ['no temperature', { current: { relative_humidity_2m: 40 } }],
  ])('rejects %s as malformed', (_label, raw) => {
    expect(() => normaliseForecast(raw)).toThrowError(expect.objectContaining({ kind: 'malformed' }));
  });
});

describe('getForecast', () => {
  beforeEach(() => clearForecastCache());

  it('fetches once and then serves the cached forecast', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(forecastResponse));
    vi.stubGlobal('fetch', fetchMock);

    const first = await getForecast(pretoria);
    const second = await getForecast(pretoria);

    expect(first.source).toBe('network');
    expect(second.source).toBe('cache');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('returns the previous forecast, marked stale, when a forced refresh fails', async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse(forecastResponse))
      .mockResolvedValue(jsonResponse({}, { status: 429 }));
    vi.stubGlobal('fetch', fetchMock);

    await getForecast(pretoria);
    const refreshed = await getForecast(pretoria, { force: true });

    expect(refreshed.source).toBe('stale');
    expect(refreshed.error?.kind).toBe('rate-limit');
    expect(refreshed.value.current.temperature).toBe(25.6);
  });

  it('rejects with the classified error when there is nothing cached', async () => {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({}, { status: 429 })));
    await expect(getForecast(pretoria)).rejects.toMatchObject({ kind: 'rate-limit' });
  });

  it('rejects as malformed when the body is valid JSON in the wrong shape', async () => {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({ hello: 'world' })));
    await expect(getForecast(pretoria)).rejects.toMatchObject({ kind: 'malformed' });
  });
});
