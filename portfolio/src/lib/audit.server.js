import { appendFileSync, existsSync, renameSync, statSync } from "node:fs";
import { join } from "node:path";
import { DATA_DIR, ensurePrivateDir } from "@/lib/db.server";
import { hashIp } from "@/lib/request-meta.server";

// Security audit log: one JSON object per line in DATA_DIR/logs/security.log
// (owner-only). Never contains passwords, tokens, codes or full email addresses;
// IPs are keyed hashes. Rotated at 5 MB (one previous file kept).
const LOG_DIR = join(DATA_DIR, "logs");
const LOG_FILE = join(LOG_DIR, "security.log");
const MAX_BYTES = 5 * 1024 * 1024;

// "someone@example.com" -> "so***@example.com"
export const maskEmail = (email = "") => {
  const [name, domain] = String(email).split("@");
  if (!domain) return "***";
  return `${name.slice(0, 2)}***@${domain}`;
};

const ALERTS = new Set(["login_locked", "admin_denied", "mfa_failed_repeatedly", "rate_limited", "login_new_device", "mfa_disabled", "password_reset"]);

export function audit(event, details = {}) {
  let ip;
  try {
    ip = hashIp();
  } catch {
    ip = "n/a"; // outside a request (scripts)
  }
  const entry = { ts: new Date().toISOString(), event, ip, ...details };
  if (entry.email) entry.email = maskEmail(entry.email);
  try {
    ensurePrivateDir(LOG_DIR);
    if (existsSync(LOG_FILE) && statSync(LOG_FILE).size > MAX_BYTES) renameSync(LOG_FILE, `${LOG_FILE}.1`);
    appendFileSync(LOG_FILE, `${JSON.stringify(entry)}\n`, { mode: 0o600 });
  } catch (error) {
    console.error("[security] could not write audit log:", error.code || error.message);
  }
  // Events worth a human's attention also go to the server console (hook alerts here).
  if (ALERTS.has(event)) console.warn(`[security] ${event}`, JSON.stringify({ ...details, email: entry.email }));
}
