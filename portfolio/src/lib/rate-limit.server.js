// In-memory fixed-window rate limiter. One server process keeps its own counters,
// which is right for this single-instance app; behind several instances, move
// this to a shared store (Redis) or the CDN/WAF.
const buckets = new Map();
let lastSweep = 0;

function sweep(now) {
  if (now - lastSweep < 60_000) return;
  lastSweep = now;
  for (const [key, bucket] of buckets) if (bucket.reset <= now) buckets.delete(key);
}

// Counts one hit against `key`. Returns { ok, retryAfter (seconds) }.
export function hit(key, limit, windowMs) {
  const now = Date.now();
  sweep(now);
  let bucket = buckets.get(key);
  if (!bucket || bucket.reset <= now) {
    bucket = { count: 0, reset: now + windowMs };
    buckets.set(key, bucket);
  }
  bucket.count += 1;
  return { ok: bucket.count <= limit, retryAfter: Math.ceil((bucket.reset - now) / 1000) };
}

// Looks without counting.
export function peek(key, limit) {
  const bucket = buckets.get(key);
  if (!bucket || bucket.reset <= Date.now()) return { ok: true, retryAfter: 0 };
  return { ok: bucket.count < limit, retryAfter: Math.ceil((bucket.reset - Date.now()) / 1000) };
}

export function clear(key) {
  buckets.delete(key);
}

export class RateLimitError extends Error {
  constructor(retryAfter, message) {
    const minutes = Math.max(1, Math.ceil(retryAfter / 60));
    super(message || `Too many attempts. Please try again in ${minutes} minute${minutes === 1 ? "" : "s"}.`);
    this.status = 429;
    this.retryAfter = retryAfter;
  }
}

// Counts a hit and throws a friendly 429 when over the limit.
export function limit(key, max, windowMs, message) {
  const result = hit(key, max, windowMs);
  if (!result.ok) throw new RateLimitError(result.retryAfter, message);
}
