/**
 * One error type for everything that can go wrong while talking to an API.
 *
 * The network layer converts every failure into an AppError with a `kind`, and
 * the UI turns that kind into friendly copy with describeError(). Raw messages,
 * status codes and stack traces never reach the screen.
 */

export type ErrorKind =
  | 'offline' // the browser reports no connection
  | 'network' // the request never got a response (DNS, CORS, dropped connection)
  | 'timeout' // no response within the configured time
  | 'rate-limit' // HTTP 429
  | 'server' // HTTP 5xx
  | 'not-found' // the place does not exist, or HTTP 404
  | 'bad-request' // the API rejected the parameters (HTTP 400)
  | 'malformed' // the response was not the JSON shape we need
  | 'empty' // a valid response with nothing usable in it
  | 'unknown';

const RETRYABLE: ReadonlySet<ErrorKind> = new Set(['offline', 'network', 'timeout', 'rate-limit', 'server', 'unknown']);

export class AppError extends Error {
  readonly kind: ErrorKind;
  readonly status?: number;
  /** Seconds the server asked us to wait, from a Retry-After header. */
  readonly retryAfterSeconds?: number;

  constructor(
    kind: ErrorKind,
    message: string,
    options: { status?: number; retryAfterSeconds?: number; cause?: unknown } = {},
  ) {
    super(message, { cause: options.cause });
    this.name = 'AppError';
    this.kind = kind;
    this.status = options.status;
    this.retryAfterSeconds = options.retryAfterSeconds;
  }

  /** Whether trying the same request again could plausibly succeed. */
  get retryable(): boolean {
    return RETRYABLE.has(this.kind);
  }
}

export function isAbortError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && (error as { name?: unknown }).name === 'AbortError';
}

/** Coerce anything thrown into an AppError. */
export function toAppError(error: unknown): AppError {
  if (error instanceof AppError) return error;
  return new AppError('unknown', error instanceof Error ? error.message : 'Unexpected error', { cause: error });
}

export interface ErrorCopy {
  title: string;
  message: string;
  /** Label for the primary action, or null when retrying cannot help. */
  action: string | null;
}

/**
 * User-facing wording for each kind of failure. `subject` names what failed to
 * load ("the forecast", "air quality") so messages stay specific.
 */
export function describeError(error: AppError, subject = 'the forecast'): ErrorCopy {
  switch (error.kind) {
    case 'offline':
      return {
        title: 'You are offline',
        message: `Reconnect to the internet and ${subject} will load again.`,
        action: 'Try again',
      };
    case 'network':
      return {
        title: 'Could not reach the weather service',
        message: 'Check your connection. A firewall, VPN or content blocker can also stop the request.',
        action: 'Try again',
      };
    case 'timeout':
      return {
        title: 'The weather service took too long',
        message: 'The request timed out before a reply arrived. This is usually a slow connection.',
        action: 'Try again',
      };
    case 'rate-limit': {
      const wait = error.retryAfterSeconds;
      const when =
        wait && wait > 0 ? `Wait about ${wait < 90 ? `${Math.ceil(wait)} seconds` : `${Math.ceil(wait / 60)} minutes`}` : 'Wait a minute';
      return {
        title: 'Too many requests',
        message: `The weather service is limiting requests from this connection. ${when}, then try again.`,
        action: 'Try again',
      };
    }
    case 'server':
      return {
        title: 'The weather service is having problems',
        message: 'The service returned an error on its side. It usually recovers within a few minutes.',
        action: 'Try again',
      };
    case 'not-found':
      return {
        title: 'Place not found',
        message: 'Check the spelling, or try a nearby larger city, a postal code, or coordinates such as -25.75, 28.19.',
        action: null,
      };
    case 'bad-request':
      return {
        title: 'That location cannot be used',
        message: 'The weather service rejected this location. Search for a different place.',
        action: null,
      };
    case 'malformed':
      return {
        title: 'The reply could not be read',
        message: `The weather service sent data in an unexpected format, so ${subject} cannot be shown right now.`,
        action: 'Try again',
      };
    case 'empty':
      return {
        title: 'No data for this place',
        message: `The weather service has nothing for ${subject} here. Try a nearby place.`,
        action: 'Try again',
      };
    default:
      return {
        title: 'Something went wrong',
        message: `We could not load ${subject}. Trying again usually fixes it.`,
        action: 'Try again',
      };
  }
}
