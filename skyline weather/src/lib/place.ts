import type { Place } from '../types';

/** Coordinates rounded to about 100 m, so the same spot always gets the same id. */
export function placeId(latitude: number, longitude: number): string {
  return `${latitude.toFixed(3)},${longitude.toFixed(3)}`;
}

/** "Pretoria, Gauteng, South Africa", skipping parts that are missing or repeated. */
export function placeLabel(place: Place): string {
  const parts = [place.name, place.region, place.country].filter(
    (part, index, all): part is string => Boolean(part) && all.indexOf(part) === index,
  );
  return parts.join(', ');
}

/** The part after the name: "Gauteng, South Africa". */
export function placeContext(place: Place): string {
  return [place.region, place.country]
    .filter((part, index, all): part is string => Boolean(part) && part !== place.name && all.indexOf(part) === index)
    .join(', ');
}

/** "25.745°S, 28.188°E" */
export function formatCoordinates(latitude: number, longitude: number, digits = 3): string {
  const lat = `${Math.abs(latitude).toFixed(digits)}°${latitude < 0 ? 'S' : 'N'}`;
  const lon = `${Math.abs(longitude).toFixed(digits)}°${longitude < 0 ? 'W' : 'E'}`;
  return `${lat}, ${lon}`;
}

/** Runtime check used when reading places back out of localStorage. */
export function isPlace(value: unknown): value is Place {
  if (typeof value !== 'object' || value === null) return false;
  const p = value as Record<string, unknown>;
  return (
    typeof p.id === 'string' &&
    typeof p.name === 'string' &&
    typeof p.latitude === 'number' &&
    Number.isFinite(p.latitude) &&
    Math.abs(p.latitude) <= 90 &&
    typeof p.longitude === 'number' &&
    Number.isFinite(p.longitude) &&
    Math.abs(p.longitude) <= 180
  );
}
