import { describe, expect, it, vi } from 'vitest';
import { TtlCache } from './cache';
import { AppError } from './errors';

/** A cache with a clock the test controls. */
function setup() {
  let now = 1_000_000;
  const cache = new TtlCache<string>({ now: () => now });
  return { cache, advance: (ms: number) => (now += ms) };
}

describe('TtlCache', () => {
  it('calls the loader once and then serves from cache while fresh', async () => {
    const { cache, advance } = setup();
    const loader = vi.fn(async () => 'sunny');

    const first = await cache.resolve('pretoria', loader, { ttlMs: 60_000 });
    advance(59_000);
    const second = await cache.resolve('pretoria', loader, { ttlMs: 60_000 });

    expect(first.source).toBe('network');
    expect(second.source).toBe('cache');
    expect(second.value).toBe('sunny');
    expect(loader).toHaveBeenCalledTimes(1);
  });

  it('loads again once the entry has expired', async () => {
    const { cache, advance } = setup();
    const loader = vi.fn<() => Promise<string>>().mockResolvedValueOnce('sunny').mockResolvedValueOnce('rain');

    await cache.resolve('pretoria', loader, { ttlMs: 60_000 });
    advance(60_001);
    const result = await cache.resolve('pretoria', loader, { ttlMs: 60_000 });

    expect(result).toMatchObject({ value: 'rain', source: 'network' });
    expect(loader).toHaveBeenCalledTimes(2);
  });

  it('bypasses a fresh entry when forced', async () => {
    const { cache } = setup();
    const loader = vi.fn<() => Promise<string>>().mockResolvedValueOnce('sunny').mockResolvedValueOnce('rain');

    await cache.resolve('pretoria', loader, { ttlMs: 60_000 });
    const result = await cache.resolve('pretoria', loader, { ttlMs: 60_000, force: true });

    expect(result.value).toBe('rain');
  });

  it('shares one request between simultaneous callers', async () => {
    const { cache } = setup();
    let release: (value: string) => void = () => {};
    const loader = vi.fn(() => new Promise<string>((resolve) => (release = resolve)));

    const a = cache.resolve('pretoria', loader, { ttlMs: 60_000 });
    const b = cache.resolve('pretoria', loader, { ttlMs: 60_000 });
    release('sunny');

    expect((await a).value).toBe('sunny');
    expect((await b).value).toBe('sunny');
    expect(loader).toHaveBeenCalledTimes(1);
  });

  it('keeps keys separate', async () => {
    const { cache } = setup();
    await cache.resolve('pretoria', async () => 'sunny', { ttlMs: 60_000 });
    const other = await cache.resolve('cape-town', async () => 'windy', { ttlMs: 60_000 });
    expect(other.value).toBe('windy');
  });

  it('falls back to the expired entry when a refresh fails, and reports why', async () => {
    const { cache, advance } = setup();
    await cache.resolve('pretoria', async () => 'sunny', { ttlMs: 60_000 });
    advance(120_000);

    const result = await cache.resolve('pretoria', async () => Promise.reject(new AppError('server', 'HTTP 503')), { ttlMs: 60_000 });

    expect(result.source).toBe('stale');
    expect(result.value).toBe('sunny');
    expect(result.error?.kind).toBe('server');
  });

  it('rejects when a load fails and there is nothing to fall back to', async () => {
    const { cache } = setup();
    await expect(cache.resolve('pretoria', async () => Promise.reject(new AppError('timeout', 'slow')), { ttlMs: 60_000 })).rejects.toMatchObject({
      kind: 'timeout',
    });
  });

  it('does not serve data older than the stale limit', async () => {
    const { cache, advance } = setup();
    await cache.resolve('pretoria', async () => 'sunny', { ttlMs: 60_000 });
    advance(10 * 60 * 60_000);
    await expect(
      cache.resolve('pretoria', async () => Promise.reject(new AppError('network', 'down')), { ttlMs: 60_000, maxStaleMs: 60 * 60_000 }),
    ).rejects.toMatchObject({ kind: 'network' });
  });

  it('can retry after a failure, because failures are not cached', async () => {
    const { cache } = setup();
    const loader = vi.fn<() => Promise<string>>().mockRejectedValueOnce(new AppError('network', 'down')).mockResolvedValueOnce('sunny');

    await expect(cache.resolve('pretoria', loader, { ttlMs: 60_000 })).rejects.toBeInstanceOf(AppError);
    await expect(cache.resolve('pretoria', loader, { ttlMs: 60_000 })).resolves.toMatchObject({ value: 'sunny' });
  });

  it('evicts the oldest entries beyond its size limit', () => {
    const cache = new TtlCache<number>({ maxEntries: 2 });
    cache.set('a', 1);
    cache.set('b', 2);
    cache.set('c', 3);
    expect(cache.peek('a')).toBeUndefined();
    expect(cache.peek('c')?.value).toBe(3);
  });

  it('restores persisted entries and ignores corrupted storage', () => {
    const first = new TtlCache<string>({ storageKey: 'cache:test' });
    first.set('pretoria', 'sunny');
    expect(new TtlCache<string>({ storageKey: 'cache:test' }).peek('pretoria')?.value).toBe('sunny');

    window.localStorage.setItem('skyline:cache:test', '{not json');
    expect(new TtlCache<string>({ storageKey: 'cache:test' }).peek('pretoria')).toBeUndefined();
  });
});
