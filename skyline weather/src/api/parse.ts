/**
 * Helpers for reading untrusted JSON. API payloads are typed as `unknown` and
 * every field is checked before use, so a missing or wrongly typed value
 * becomes `null` in the UI instead of a crash.
 */

export type Json = Record<string, unknown>;

export function isObject(value: unknown): value is Json {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/** A finite number, or null. */
export function num(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

/** A non-empty string, or null. */
export function str(value: unknown): string | null {
  return typeof value === 'string' && value !== '' ? value : null;
}

/** The array stored under `key`, or an empty array. */
export function list(source: unknown, key: string): unknown[] {
  if (!isObject(source)) return [];
  const value = source[key];
  return Array.isArray(value) ? value : [];
}
