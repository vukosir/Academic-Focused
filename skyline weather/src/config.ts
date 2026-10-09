/**
 * Runtime configuration, read once from Vite environment variables.
 *
 * Nothing secret is hardcoded here. Open-Meteo works without a key for
 * non-commercial use; a commercial key, when present in VITE_OPEN_METEO_API_KEY,
 * switches every request to the customer endpoints. See .env.example.
 */
import type { Place } from './types';
import { placeId } from './lib/place';

type Env = Record<string, string | boolean | undefined>;

function readString(env: Env, key: string): string | undefined {
  const value = env[key];
  return typeof value === 'string' && value.trim() !== '' ? value.trim() : undefined;
}

function readNumber(env: Env, key: string, fallback: number, min: number, max: number): number {
  const raw = readString(env, key);
  if (raw === undefined) return fallback;
  const value = Number(raw);
  return Number.isFinite(value) && value >= min && value <= max ? value : fallback;
}

export interface AppConfig {
  apiKey: string | undefined;
  endpoints: { forecast: string; geocoding: string; airQuality: string; reverseGeocoding: string };
  defaultPlace: Place;
  requestTimeoutMs: number;
  forecastTtlMs: number;
  airQualityTtlMs: number;
  geocodingTtlMs: number;
  /** Delay between the last keystroke and the suggestions request. */
  searchDebounceMs: number;
  maxHistory: number;
  maxFavorites: number;
}

/** Exported for tests so configuration can be built from a plain object. */
export function buildConfig(env: Env): AppConfig {
  const apiKey = readString(env, 'VITE_OPEN_METEO_API_KEY');
  // Commercial keys are only accepted on the "customer-" hosts.
  const host = (name: string) => `https://${apiKey ? 'customer-' : ''}${name}.open-meteo.com/v1`;

  const latitude = readNumber(env, 'VITE_DEFAULT_LOCATION_LAT', -25.74486, -90, 90);
  const longitude = readNumber(env, 'VITE_DEFAULT_LOCATION_LON', 28.18783, -180, 180);

  return {
    apiKey,
    endpoints: {
      forecast: `${host('api')}/forecast`,
      geocoding: `${host('geocoding-api')}/search`,
      airQuality: `${host('air-quality-api')}/air-quality`,
      // Open-Meteo has no reverse geocoder. This keyless endpoint is only used
      // to put a name on raw coordinates and the app works without it.
      reverseGeocoding: 'https://api.bigdatacloud.net/data/reverse-geocode-client',
    },
    defaultPlace: {
      id: placeId(latitude, longitude),
      name: readString(env, 'VITE_DEFAULT_LOCATION_NAME') ?? 'Pretoria',
      region: readString(env, 'VITE_DEFAULT_LOCATION_REGION') ?? 'Gauteng',
      country: readString(env, 'VITE_DEFAULT_LOCATION_COUNTRY') ?? 'South Africa',
      latitude,
      longitude,
    },
    requestTimeoutMs: readNumber(env, 'VITE_REQUEST_TIMEOUT_MS', 10_000, 1_000, 60_000),
    forecastTtlMs: readNumber(env, 'VITE_FORECAST_CACHE_MINUTES', 10, 1, 120) * 60_000,
    airQualityTtlMs: 30 * 60_000,
    geocodingTtlMs: 24 * 60 * 60_000,
    searchDebounceMs: 300,
    maxHistory: 6,
    maxFavorites: 8,
  };
}

export const config: AppConfig = buildConfig(import.meta.env as Env);
