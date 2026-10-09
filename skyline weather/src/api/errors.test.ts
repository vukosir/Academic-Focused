import { describe, expect, it } from 'vitest';
import { AppError, describeError, toAppError, type ErrorKind } from './errors';

const KINDS: ErrorKind[] = ['offline', 'network', 'timeout', 'rate-limit', 'server', 'not-found', 'bad-request', 'malformed', 'empty', 'unknown'];

describe('describeError', () => {
  it('gives every kind of failure its own title', () => {
    const titles = KINDS.map((kind) => describeError(new AppError(kind, 'internal detail')).title);
    expect(new Set(titles).size).toBe(KINDS.length);
  });

  it('never shows the internal message to the user', () => {
    for (const kind of KINDS) {
      const copy = describeError(new AppError(kind, 'ECONNRESET at Socket.onerror stack trace'));
      expect(`${copy.title} ${copy.message}`).not.toMatch(/ECONNRESET|stack trace/);
    }
  });

  it('offers a retry only when retrying can help', () => {
    expect(describeError(new AppError('timeout', '')).action).toBe('Try again');
    expect(describeError(new AppError('rate-limit', '')).action).toBe('Try again');
    expect(describeError(new AppError('not-found', '')).action).toBeNull();
    expect(describeError(new AppError('bad-request', '')).action).toBeNull();
  });

  it('tells the user how long to wait when the server said so', () => {
    const copy = describeError(new AppError('rate-limit', '', { retryAfterSeconds: 45 }));
    expect(copy.message).toContain('45 seconds');
  });

  it('names what failed to load', () => {
    expect(describeError(new AppError('unknown', ''), 'air quality').message).toContain('air quality');
  });
});

describe('toAppError', () => {
  it('passes AppErrors through and wraps everything else as unknown', () => {
    const original = new AppError('server', 'boom');
    expect(toAppError(original)).toBe(original);
    expect(toAppError(new Error('boom')).kind).toBe('unknown');
    expect(toAppError('a string').kind).toBe('unknown');
  });
});
