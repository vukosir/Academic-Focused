/**
 * A small time-based cache with three jobs:
 *
 *  1. Reuse a fresh answer instead of calling the API again.
 *  2. Share one in-flight request between callers that ask for the same key at
 *     the same moment (two widgets, a double click on Refresh).
 *  3. Keep the last good answer around so that, when a refresh fails, the UI can
 *     keep showing slightly old data instead of an error screen.
 *
 * Entries can optionally be mirrored to localStorage so point 3 also works
 * after a page reload.
 */
import { AppError, toAppError } from './errors';
import { readJson, writeJson } from '../lib/storage';

interface Entry<T> {
  value: T;
  storedAt: number;
}

export interface CacheResult<T> {
  value: T;
  storedAt: number;
  /** network: just fetched. cache: reused while fresh. stale: old data shown because the fetch failed. */
  source: 'network' | 'cache' | 'stale';
  /** Present only when source is "stale": the reason fresh data could not be loaded. */
  error?: AppError;
}

export interface ResolveOptions {
  ttlMs: number;
  /** Skip the freshness check and go to the network. */
  force?: boolean;
  /** Fall back to an expired entry if the loader throws. Defaults to true. */
  staleOnError?: boolean;
  /** Oldest entry that may still be served as stale. Defaults to 6 hours. */
  maxStaleMs?: number;
}

export class TtlCache<T> {
  private entries = new Map<string, Entry<T>>();
  private inFlight = new Map<string, Promise<CacheResult<T>>>();

  constructor(
    private readonly options: { maxEntries?: number; storageKey?: string; now?: () => number } = {},
  ) {
    if (options.storageKey) {
      const saved = readJson<Record<string, Entry<T>>>(options.storageKey, isEntryRecord, {});
      for (const [key, entry] of Object.entries(saved)) this.entries.set(key, entry);
    }
  }

  private now(): number {
    return this.options.now ? this.options.now() : Date.now();
  }

  /** Returns the entry if it is younger than ttlMs. */
  getFresh(key: string, ttlMs: number): Entry<T> | undefined {
    const entry = this.entries.get(key);
    return entry && this.now() - entry.storedAt < ttlMs ? entry : undefined;
  }

  /** Returns the entry regardless of age. */
  peek(key: string): Entry<T> | undefined {
    return this.entries.get(key);
  }

  set(key: string, value: T): Entry<T> {
    const entry = { value, storedAt: this.now() };
    // Re-insert so the Map's insertion order doubles as least-recently-written order.
    this.entries.delete(key);
    this.entries.set(key, entry);
    const max = this.options.maxEntries ?? 50;
    while (this.entries.size > max) {
      const oldest = this.entries.keys().next().value;
      if (oldest === undefined) break;
      this.entries.delete(oldest);
    }
    this.persist();
    return entry;
  }

  clear(): void {
    this.entries.clear();
    this.inFlight.clear();
    this.persist();
  }

  /**
   * Return a fresh cached value, or run `loader`, cache its result and return it.
   * Concurrent calls for the same key share one loader run.
   */
  resolve(key: string, loader: () => Promise<T>, options: ResolveOptions): Promise<CacheResult<T>> {
    const { ttlMs, force = false, staleOnError = true, maxStaleMs = 6 * 60 * 60_000 } = options;

    if (!force) {
      const fresh = this.getFresh(key, ttlMs);
      if (fresh) return Promise.resolve({ ...fresh, source: 'cache' });
    }

    const pending = this.inFlight.get(key);
    if (pending) return pending;

    const run = loader()
      .then((value): CacheResult<T> => ({ ...this.set(key, value), source: 'network' }))
      .catch((error: unknown): CacheResult<T> => {
        const appError = toAppError(error);
        const old = this.entries.get(key);
        if (staleOnError && old && this.now() - old.storedAt < maxStaleMs) {
          return { ...old, source: 'stale', error: appError };
        }
        throw appError;
      })
      .finally(() => {
        this.inFlight.delete(key);
      });

    this.inFlight.set(key, run);
    return run;
  }

  private persist(): void {
    if (!this.options.storageKey) return;
    writeJson(this.options.storageKey, Object.fromEntries(this.entries));
  }
}

function isEntryRecord(value: unknown): value is Record<string, Entry<never>> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return false;
  return Object.values(value).every(
    (entry) =>
      typeof entry === 'object' &&
      entry !== null &&
      typeof (entry as { storedAt?: unknown }).storedAt === 'number' &&
      'value' in entry,
  );
}
