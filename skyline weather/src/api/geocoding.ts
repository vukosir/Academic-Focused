/**
 * Turning what the user typed into a Place.
 *
 * Three kinds of input are supported:
 *  - a city or town name          "Cape Town"
 *  - a postal or ZIP code         "0002", "90210"
 *  - a pair of coordinates        "-25.75, 28.19" or "25.75S 28.19E"
 *
 * Names and postal codes go to the Open-Meteo geocoding API. Coordinates are
 * parsed locally and then given a name with a best-effort reverse lookup.
 */
import { config } from '../config';
import type { Place } from '../types';
import { formatCoordinates, placeId } from '../lib/place';
import { TtlCache } from './cache';
import { AppError, isAbortError } from './errors';
import { buildUrl, fetchJson } from './http';
import { isObject, list, num, str } from './parse';

export const MIN_QUERY_LENGTH = 2;

const searchCache = new TtlCache<Place[]>({ maxEntries: 80 });
const reverseCache = new TtlCache<Place>({ maxEntries: 20 });

export interface Coordinates {
  latitude: number;
  longitude: number;
}

/**
 * Recognise "lat, lon" in decimal degrees, with an optional hemisphere letter
 * on each number. Returns null for anything else, including out-of-range values.
 */
export function parseCoordinates(input: string): Coordinates | null {
  const match = input
    .trim()
    .match(/^([+-]?\d{1,3}(?:\.\d+)?)\s*°?\s*([NS])?\s*[,;\s]\s*([+-]?\d{1,3}(?:\.\d+)?)\s*°?\s*([EW])?$/i);
  if (!match) return null;

  let latitude = Number(match[1]);
  let longitude = Number(match[3]);
  if (match[2]?.toUpperCase() === 'S') latitude = -Math.abs(latitude);
  if (match[4]?.toUpperCase() === 'W') longitude = -Math.abs(longitude);

  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return null;
  if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) return null;
  return { latitude, longitude };
}

/** Convert one geocoding result. Returns null when the essentials are missing. */
function toPlace(raw: unknown): Place | null {
  if (!isObject(raw)) return null;
  const name = str(raw.name);
  const latitude = num(raw.latitude);
  const longitude = num(raw.longitude);
  if (name === null || latitude === null || longitude === null) return null;
  if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) return null;
  return {
    id: placeId(latitude, longitude),
    name,
    region: str(raw.admin1) ?? undefined,
    country: str(raw.country) ?? undefined,
    countryCode: str(raw.country_code) ?? undefined,
    latitude,
    longitude,
    timezone: str(raw.timezone) ?? undefined,
  };
}

/** Parse a geocoding response. An absent `results` key is how the API says "no matches". */
export function normalisePlaces(raw: unknown): Place[] {
  if (!isObject(raw)) throw new AppError('malformed', 'Geocoding response was not an object');
  const seen = new Set<string>();
  const places: Place[] = [];
  for (const item of list(raw, 'results')) {
    const place = toPlace(item);
    if (place && !seen.has(place.id)) {
      seen.add(place.id);
      places.push(place);
    }
  }
  return places;
}

/**
 * Look up places matching a name or postal code. Resolves to an empty array
 * when nothing matches. Results are cached for a day because places do not move.
 */
export async function searchPlaces(query: string, options: { signal?: AbortSignal; count?: number } = {}): Promise<Place[]> {
  const term = query.trim();
  if (term.length < MIN_QUERY_LENGTH) return [];

  const count = options.count ?? 6;
  const key = `${term.toLowerCase()}|${count}`;
  const cached = searchCache.getFresh(key, config.geocodingTtlMs);
  if (cached) return cached.value;

  const url = buildUrl(config.endpoints.geocoding, { name: term, count, language: 'en', format: 'json' });
  const places = normalisePlaces(await fetchJson(url, { signal: options.signal, retries: 0 }));
  searchCache.set(key, places);
  return places;
}

/** A Place for raw coordinates, named by coordinates until a reverse lookup succeeds. */
export function placeFromCoordinates({ latitude, longitude }: Coordinates): Place {
  return {
    id: placeId(latitude, longitude),
    name: formatCoordinates(latitude, longitude, 2),
    latitude,
    longitude,
  };
}

/**
 * Put a name on coordinates. This never rejects for network reasons: when the
 * lookup fails the place is simply labelled with its coordinates, because the
 * forecast itself only needs latitude and longitude.
 */
export async function reverseGeocode(coords: Coordinates, options: { signal?: AbortSignal } = {}): Promise<Place> {
  const fallback = placeFromCoordinates(coords);
  const cached = reverseCache.getFresh(fallback.id, config.geocodingTtlMs);
  if (cached) return cached.value;

  try {
    const url = buildUrl(config.endpoints.reverseGeocoding, {
      latitude: coords.latitude,
      longitude: coords.longitude,
      localityLanguage: 'en',
    });
    const raw = await fetchJson(url, { signal: options.signal, retries: 0, timeoutMs: 5_000 });
    if (!isObject(raw)) return fallback;
    const name = str(raw.city) ?? str(raw.locality);
    if (name === null) return fallback;
    const place: Place = {
      ...fallback,
      name,
      region: str(raw.principalSubdivision) ?? undefined,
      country: str(raw.countryName) ?? undefined,
      countryCode: str(raw.countryCode) ?? undefined,
    };
    reverseCache.set(fallback.id, place);
    return place;
  } catch (error) {
    if (isAbortError(error)) throw error;
    return fallback;
  }
}

/**
 * Resolve a submitted search to one Place: coordinates directly, otherwise the
 * best geocoding match. Rejects with a "not-found" AppError when nothing matches.
 */
export async function resolveQuery(query: string, options: { signal?: AbortSignal } = {}): Promise<Place> {
  const term = query.trim();
  if (term === '') throw new AppError('empty', 'Nothing to search for');

  const coords = parseCoordinates(term);
  if (coords) return reverseGeocode(coords, options);

  if (term.length < MIN_QUERY_LENGTH) throw new AppError('not-found', 'Query too short');
  const [first] = await searchPlaces(term, options);
  if (!first) throw new AppError('not-found', `No place matches "${term}"`);
  return first;
}

/** Test helper. */
export function clearGeocodingCache(): void {
  searchCache.clear();
  reverseCache.clear();
}
