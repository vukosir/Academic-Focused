/**
 * localStorage access that cannot throw.
 *
 * Storage may be unavailable (private windows, blocked site data, quota), and
 * whatever comes back may be from an older version of the app or edited by
 * hand. Every read is therefore parsed defensively and checked with a guard
 * before it is trusted.
 */

const PREFIX = 'skyline:';

export function readJson<T>(key: string, guard: (value: unknown) => value is T, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(PREFIX + key);
    if (raw === null) return fallback;
    const parsed: unknown = JSON.parse(raw);
    return guard(parsed) ? parsed : fallback;
  } catch {
    return fallback;
  }
}

export function writeJson(key: string, value: unknown): void {
  try {
    window.localStorage.setItem(PREFIX + key, JSON.stringify(value));
  } catch {
    // Quota exceeded or storage disabled. The app keeps working from memory.
  }
}

export function removeKey(key: string): void {
  try {
    window.localStorage.removeItem(PREFIX + key);
  } catch {
    // Nothing to do.
  }
}
