/**
 * useResource: load something remote and track its lifecycle.
 *
 * The state is a small machine:
 *
 *   idle ──► loading ──► success
 *                  └───► error
 *
 * From success, a refresh keeps the data on screen (`isRefreshing`) instead of
 * flashing skeletons. If a refresh fails but the cache still had older data,
 * the hook stays in success with `staleError` set, so the UI can say "showing
 * data from earlier" rather than dropping to an error screen.
 *
 * Responses that arrive after the key has changed (the user picked another
 * place mid-request) are ignored.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import type { CacheResult } from '../api/cache';
import { AppError, toAppError } from '../api/errors';

export type ResourceState<T> =
  | { status: 'idle'; data: null; error: null }
  | { status: 'loading'; data: null; error: null }
  | { status: 'success'; data: T; error: null; updatedAt: number; staleError: AppError | null }
  | { status: 'error'; data: null; error: AppError };

export interface Resource<T> {
  state: ResourceState<T>;
  /** True while a refresh runs with data already on screen. */
  isRefreshing: boolean;
  /** Fetch again, bypassing the cache. */
  refresh: () => void;
}

type Loader<T> = (options: { force: boolean }) => Promise<CacheResult<T>>;

const IDLE = { status: 'idle', data: null, error: null } as const;
const LOADING = { status: 'loading', data: null, error: null } as const;

/**
 * @param key      Identifies what is being loaded. null means "nothing to load".
 * @param loader   Performs the request. Must be stable for a given key.
 * @param options  refreshIntervalMs re-requests in the background while the tab is visible.
 */
export function useResource<T>(key: string | null, loader: Loader<T>, options: { refreshIntervalMs?: number } = {}): Resource<T> {
  const [state, setState] = useState<ResourceState<T>>(key === null ? IDLE : LOADING);
  const [isRefreshing, setRefreshing] = useState(false);

  // The latest request wins. Anything older is discarded when it resolves.
  const requestId = useRef(0);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;
  const hasData = useRef(false);

  const run = useCallback(async (force: boolean) => {
    const id = (requestId.current += 1);
    if (hasData.current) setRefreshing(true);
    else setState(LOADING);

    try {
      const result = await loaderRef.current({ force });
      if (id !== requestId.current) return;
      hasData.current = true;
      setState({
        status: 'success',
        data: result.value,
        error: null,
        updatedAt: result.storedAt,
        staleError: result.source === 'stale' ? (result.error ?? null) : null,
      });
    } catch (error) {
      if (id !== requestId.current) return;
      hasData.current = false;
      setState({ status: 'error', data: null, error: toAppError(error) });
    } finally {
      if (id === requestId.current) setRefreshing(false);
    }
  }, []);

  // Load whenever the key changes.
  useEffect(() => {
    hasData.current = false;
    setRefreshing(false);
    if (key === null) {
      requestId.current += 1;
      setState(IDLE);
      return;
    }
    void run(false);
    return () => {
      // Invalidate the in-flight request for the old key.
      requestId.current += 1;
    };
  }, [key, run]);

  // Background refresh. The cache decides whether this actually hits the network.
  const { refreshIntervalMs } = options;
  useEffect(() => {
    if (key === null || !refreshIntervalMs) return;
    const tick = () => {
      if (document.visibilityState === 'visible') void run(false);
    };
    const timer = window.setInterval(tick, refreshIntervalMs);
    document.addEventListener('visibilitychange', tick);
    window.addEventListener('online', tick);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener('visibilitychange', tick);
      window.removeEventListener('online', tick);
    };
  }, [key, refreshIntervalMs, run]);

  const refresh = useCallback(() => {
    if (key !== null) void run(true);
  }, [key, run]);

  return { state, isRefreshing, refresh };
}
