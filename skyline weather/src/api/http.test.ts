import { describe, expect, it, vi } from 'vitest';
import { badRequestResponse, jsonResponse } from '../test/fixtures';
import { AppError } from './errors';
import { fetchJson } from './http';

/** Run fetchJson against a fake fetch and return the AppError it rejects with. */
async function failure(fetchImpl: typeof fetch, options: Parameters<typeof fetchJson>[1] = {}): Promise<AppError> {
  try {
    await fetchJson('https://api.example.test/data', { fetchImpl, retries: 0, ...options });
  } catch (error) {
    expect(error).toBeInstanceOf(AppError);
    return error as AppError;
  }
  throw new Error('Expected fetchJson to reject');
}

describe('fetchJson', () => {
  it('returns the parsed body on success', async () => {
    const fetchImpl = vi.fn(async () => jsonResponse({ ok: true }));
    await expect(fetchJson('https://api.example.test/data', { fetchImpl })).resolves.toEqual({ ok: true });
    expect(fetchImpl).toHaveBeenCalledTimes(1);
  });

  it('classifies a failed connection as a network error', async () => {
    const error = await failure(vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))));
    expect(error.kind).toBe('network');
    expect(error.retryable).toBe(true);
  });

  it('reports offline without calling fetch when the browser has no connection', async () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);
    const fetchImpl = vi.fn();
    const error = await failure(fetchImpl as unknown as typeof fetch);
    expect(error.kind).toBe('offline');
    expect(fetchImpl).not.toHaveBeenCalled();
  });

  it('classifies HTTP 429 as a rate limit and reads Retry-After', async () => {
    const error = await failure(vi.fn(async () => jsonResponse({ error: true, reason: 'Too many requests' }, { status: 429, headers: { 'Retry-After': '30' } })));
    expect(error.kind).toBe('rate-limit');
    expect(error.status).toBe(429);
    expect(error.retryAfterSeconds).toBe(30);
  });

  it('never retries a rate limit automatically', async () => {
    const fetchImpl = vi.fn(async () => jsonResponse({}, { status: 429 }));
    await failure(fetchImpl, { retries: 3, retryDelayMs: 0 });
    expect(fetchImpl).toHaveBeenCalledTimes(1);
  });

  it.each([500, 502, 503])('classifies HTTP %i as a server error', async (status) => {
    const error = await failure(vi.fn(async () => jsonResponse('upstream exploded', { status })));
    expect(error.kind).toBe('server');
    expect(error.status).toBe(status);
  });

  it('classifies HTTP 400 as a bad request and keeps the API reason for developers', async () => {
    const error = await failure(vi.fn(async () => jsonResponse(badRequestResponse, { status: 400 })));
    expect(error.kind).toBe('bad-request');
    expect(error.message).toContain('Latitude must be in range');
    expect(error.retryable).toBe(false);
  });

  it('classifies HTTP 404 as not found', async () => {
    const error = await failure(vi.fn(async () => jsonResponse({}, { status: 404 })));
    expect(error.kind).toBe('not-found');
  });

  it('treats an error document inside a 200 as a server error', async () => {
    const error = await failure(vi.fn(async () => jsonResponse({ error: true, reason: 'The service is overloaded' })));
    expect(error.kind).toBe('server');
  });

  it('classifies a body that is not JSON as malformed', async () => {
    const error = await failure(vi.fn(async () => jsonResponse('<html>gateway page</html>')));
    expect(error.kind).toBe('malformed');
  });

  it('times out when no response arrives in time', async () => {
    // A fetch that never answers, but honours cancellation like the real one.
    const hangingFetch = vi.fn(
      (_url: RequestInfo | URL, init?: RequestInit) =>
        new Promise<Response>((_resolve, reject) => {
          init?.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')));
        }),
    );
    const error = await failure(hangingFetch, { timeoutMs: 20 });
    expect(error.kind).toBe('timeout');
    expect(error.retryable).toBe(true);
  });

  it('rejects with AbortError, not AppError, when the caller cancels', async () => {
    const controller = new AbortController();
    const hangingFetch = vi.fn(
      (_url: RequestInfo | URL, init?: RequestInit) =>
        new Promise<Response>((_resolve, reject) => {
          init?.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')));
        }),
    );
    const pending = fetchJson('https://api.example.test/data', { fetchImpl: hangingFetch, signal: controller.signal });
    controller.abort();
    await expect(pending).rejects.toMatchObject({ name: 'AbortError' });
  });

  it('retries once after a server error and succeeds if the second attempt works', async () => {
    const fetchImpl = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(jsonResponse({}, { status: 503 }))
      .mockResolvedValueOnce(jsonResponse({ ok: true }));
    await expect(fetchJson('https://api.example.test/data', { fetchImpl, retries: 1, retryDelayMs: 0 })).resolves.toEqual({ ok: true });
    expect(fetchImpl).toHaveBeenCalledTimes(2);
  });

  it('gives up after the configured retries', async () => {
    const fetchImpl = vi.fn(async () => jsonResponse({}, { status: 500 }));
    const error = await failure(fetchImpl, { retries: 2, retryDelayMs: 0 });
    expect(error.kind).toBe('server');
    expect(fetchImpl).toHaveBeenCalledTimes(3);
  });
});
