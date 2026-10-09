/**
 * Air quality from the Open-Meteo air quality API. Loaded separately from the
 * forecast so that a failure here only affects the air quality panel.
 */
import { config } from '../config';
import type { AirQuality, Place } from '../types';
import { TtlCache, type CacheResult } from './cache';
import { AppError } from './errors';
import { buildUrl, fetchJson } from './http';
import { isObject, num, str } from './parse';

const FIELDS = ['us_aqi', 'european_aqi', 'pm2_5', 'pm10', 'ozone', 'nitrogen_dioxide', 'sulphur_dioxide', 'carbon_monoxide'];

const airQualityCache = new TtlCache<AirQuality>({ maxEntries: 12 });

export function buildAirQualityUrl(place: Pick<Place, 'latitude' | 'longitude'>): string {
  return buildUrl(config.endpoints.airQuality, {
    latitude: place.latitude.toFixed(4),
    longitude: place.longitude.toFixed(4),
    current: FIELDS.join(','),
    timezone: 'auto',
  });
}

export function normaliseAirQuality(raw: unknown, fetchedAt: number = Date.now()): AirQuality {
  if (!isObject(raw)) throw new AppError('malformed', 'Air quality response was not an object');
  const cur = raw.current;
  if (!isObject(cur)) throw new AppError('malformed', 'Air quality response has no current block');

  const result: AirQuality = {
    time: str(cur.time),
    usAqi: num(cur.us_aqi),
    europeanAqi: num(cur.european_aqi),
    pm25: num(cur.pm2_5),
    pm10: num(cur.pm10),
    ozone: num(cur.ozone),
    nitrogenDioxide: num(cur.nitrogen_dioxide),
    sulphurDioxide: num(cur.sulphur_dioxide),
    carbonMonoxide: num(cur.carbon_monoxide),
    fetchedAt,
  };

  // The model has no coverage for some remote places and returns nulls throughout.
  const hasAnything = Object.entries(result).some(([key, value]) => key !== 'fetchedAt' && key !== 'time' && value !== null);
  if (!hasAnything) throw new AppError('empty', 'No air quality data for this place');
  return result;
}

export function getAirQuality(place: Place, options: { force?: boolean } = {}): Promise<CacheResult<AirQuality>> {
  return airQualityCache.resolve(
    place.id,
    async () => normaliseAirQuality(await fetchJson(buildAirQualityUrl(place))),
    { ttlMs: config.airQualityTtlMs, force: options.force },
  );
}

/** Test helper. */
export function clearAirQualityCache(): void {
  airQualityCache.clear();
}
