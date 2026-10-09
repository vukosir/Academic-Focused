/**
 * fetchJson: the single place where the app touches the network.
 *
 * Responsibilities:
 *  - enforce a timeout with AbortController
 *  - let callers cancel through their own AbortSignal
 *  - classify every failure into an AppError kind (see errors.ts)
 *  - retry once, after a short pause, for failures that are often transient
 */
import { config } from '../config';
import { AppError, isAbortError, toAppError } from './errors';

export interface FetchJsonOptions {
  /** Cancels the request. A cancelled request rejects with an AbortError, not an AppError. */
  signal?: AbortSignal;
  timeoutMs?: number;
  /** Extra attempts after a network or 5xx failure. Rate limits are never retried automatically. */
  retries?: number;
  retryDelayMs?: number;
  /** Injection point for tests. */
  fetchImpl?: typeof fetch;
}

function abortError(): DOMException {
  return new DOMException('The request was cancelled', 'AbortError');
}

function parseRetryAfter(header: string | null): number | undefined {
  if (!header) return undefined;
  const seconds = Number(header);
  if (Number.isFinite(seconds) && seconds >= 0) return seconds;
  const date = Date.parse(header);
  if (Number.isNaN(date)) return undefined;
  return Math.max(0, Math.round((date - Date.now()) / 1000));
}

/** Open-Meteo reports problems as { "error": true, "reason": "..." }. */
function apiReason(body: unknown): string | undefined {
  if (typeof body === 'object' && body !== null && (body as { error?: unknown }).error === true) {
    const reason = (body as { reason?: unknown }).reason;
    return typeof reason === 'string' ? reason : 'The API reported an error';
  }
  return undefined;
}

async function readBody(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return undefined;
  }
}

async function attempt(url: string, options: FetchJsonOptions): Promise<unknown> {
  const { signal, timeoutMs = config.requestTimeoutMs, fetchImpl = fetch } = options;

  if (signal?.aborted) throw abortError();
  if (typeof navigator !== 'undefined' && navigator.onLine === false) {
    throw new AppError('offline', 'The browser is offline');
  }

  const controller = new AbortController();
  let timedOut = false;
  const timer = setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, timeoutMs);
  const onCallerAbort = () => controller.abort();
  signal?.addEventListener('abort', onCallerAbort, { once: true });

  let response: Response;
  try {
    response = await fetchImpl(url, { signal: controller.signal, headers: { Accept: 'application/json' } });
  } catch (error) {
    if (timedOut) throw new AppError('timeout', `No response within ${timeoutMs} ms`, { cause: error });
    if (signal?.aborted || isAbortError(error)) throw abortError();
    // fetch rejects with a TypeError when no response arrives at all.
    throw new AppError('network', 'The request could not be completed', { cause: error });
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener('abort', onCallerAbort);
  }

  if (!response.ok) {
    const body = await readBody(response);
    const reason = apiReason(body) ?? `HTTP ${response.status}`;
    const status = response.status;
    if (status === 429) {
      throw new AppError('rate-limit', reason, { status, retryAfterSeconds: parseRetryAfter(response.headers.get('Retry-After')) });
    }
    if (status >= 500) throw new AppError('server', reason, { status });
    if (status === 404) throw new AppError('not-found', reason, { status });
    if (status === 400 || status === 422) throw new AppError('bad-request', reason, { status });
    throw new AppError('unknown', reason, { status });
  }

  let body: unknown;
  try {
    body = await response.json();
  } catch (error) {
    throw new AppError('malformed', 'The response was not valid JSON', { status: response.status, cause: error });
  }

  // Open-Meteo can answer 200 with an error document, for example
  // {"error":true,"reason":"The service is overloaded"}. Parameter problems
  // come back as HTTP 400, so an error inside a 200 is a service-side failure.
  const reason = apiReason(body);
  if (reason) throw new AppError('server', reason, { status: response.status });

  return body;
}

const wait = (ms: number, signal?: AbortSignal) =>
  new Promise<void>((resolve, reject) => {
    const timer = setTimeout(resolve, ms);
    signal?.addEventListener(
      'abort',
      () => {
        clearTimeout(timer);
        reject(abortError());
      },
      { once: true },
    );
  });

/**
 * GET a URL and parse the JSON body.
 * Rejects with AppError for every failure except caller cancellation.
 */
export async function fetchJson(url: string, options: FetchJsonOptions = {}): Promise<unknown> {
  const { retries = 1, retryDelayMs = 600 } = options;
  let lastError: AppError | undefined;

  for (let tryNumber = 0; tryNumber <= retries; tryNumber += 1) {
    try {
      return await attempt(url, options);
    } catch (error) {
      if (isAbortError(error)) throw error;
      lastError = toAppError(error);
      const transient = lastError.kind === 'network' || lastError.kind === 'server';
      if (!transient || tryNumber === retries) throw lastError;
      await wait(retryDelayMs * (tryNumber + 1), options.signal);
    }
  }
  // Unreachable, but keeps the return type honest.
  throw lastError ?? new AppError('unknown', 'Request failed');
}

/** Build a URL with query parameters, appending the API key when one is configured. */
export function buildUrl(base: string, params: Record<string, string | number | undefined>): string {
  const url = new URL(base);
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined) url.searchParams.set(key, String(value));
  }
  if (config.apiKey && url.hostname.endsWith('open-meteo.com')) url.searchParams.set('apikey', config.apiKey);
  return url.toString();
}
