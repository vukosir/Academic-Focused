import { describe, expect, it } from 'vitest';
import { airQualityResponse } from '../test/fixtures';
import { normaliseAirQuality } from './airQuality';

describe('normaliseAirQuality', () => {
  it('maps a full response', () => {
    expect(normaliseAirQuality(airQualityResponse, 5)).toMatchObject({
      usAqi: 86,
      europeanAqi: 44,
      pm25: 21.9,
      pm10: 22.2,
      ozone: 94,
      carbonMonoxide: 300,
      fetchedAt: 5,
    });
  });

  it('keeps what is present when some pollutants are missing', () => {
    const result = normaliseAirQuality({ current: { us_aqi: 40, pm2_5: null } });
    expect(result.usAqi).toBe(40);
    expect(result.pm25).toBeNull();
    expect(result.ozone).toBeNull();
  });

  it('reports "empty" when the model has no values for the place', () => {
    expect(() => normaliseAirQuality({ current: { time: '2026-10-09T19:00', us_aqi: null, pm2_5: null } })).toThrowError(
      expect.objectContaining({ kind: 'empty' }),
    );
  });

  it('reports "malformed" when the current block is missing', () => {
    expect(() => normaliseAirQuality({})).toThrowError(expect.objectContaining({ kind: 'malformed' }));
  });
});
