#!/usr/bin/env node
// Grant or remove the owner (admin) role. Run on the server, by someone with access
// to it, after the person has created a normal account on the website:
//
//   npm run admin -- grant vukosir@icloud.com
//   npm run admin -- revoke someone@example.com
//   npm run admin -- list
//
// Admin is never given by signing up: without email verification anyone could
// register with the owner's address first. Changing the role signs that account
// out everywhere. The running server picks up the change automatically.
import { existsSync, readFileSync, renameSync, writeFileSync } from "node:fs";
import { join } from "node:path";

if (existsSync(".env")) process.loadEnvFile(".env");
const DATA_DIR = process.env.DATA_DIR || join(process.cwd(), "data");
const DB_FILE = join(DATA_DIR, "db.json");
const [command, rawEmail] = process.argv.slice(2);
const email = String(rawEmail || "").trim().toLowerCase();

function fail(message) {
  console.error(message);
  process.exit(1);
}

if (!existsSync(DB_FILE)) fail(`No database at ${DB_FILE}. Start the site once and create the account first.`);
const db = JSON.parse(readFileSync(DB_FILE, "utf8"));
db.users ??= [];
db.sessions ??= [];

if (command === "list") {
  const admins = db.users.filter((u) => u.role === "admin");
  if (!admins.length) console.log("No admins yet.");
  for (const u of admins) console.log(`${u.email}  (two-step verification: ${u.mfa_enabled ? "on" : "OFF - must be turned on to open the inbox"})`);
  process.exit(0);
}

if (!["grant", "revoke"].includes(command) || !email.includes("@")) fail("Usage: npm run admin -- grant|revoke <email>   or   npm run admin -- list");
const user = db.users.find((u) => u.email === email);
if (!user) fail(`No account for ${email}. Ask them to create one on the website first.`);

user.role = command === "grant" ? "admin" : "user";
user.updated_date = new Date().toISOString();
db.sessions = db.sessions.filter((s) => s.user_id !== user.id);

const tmp = `${DB_FILE}.tmp`;
writeFileSync(tmp, JSON.stringify(db), { mode: 0o600 });
renameSync(tmp, DB_FILE);

const logDir = join(DATA_DIR, "logs");
try {
  const { mkdirSync, appendFileSync } = await import("node:fs");
  mkdirSync(logDir, { recursive: true, mode: 0o700 });
  appendFileSync(join(logDir, "security.log"), `${JSON.stringify({ ts: new Date().toISOString(), event: `admin_${command}`, ip: "cli", user_id: user.id })}\n`, { mode: 0o600 });
} catch {
  /* logging is best effort here */
}

console.log(`${email} is now ${user.role === "admin" ? "an admin" : "a customer"}. They have been signed out and must sign in again.`);
if (user.role === "admin" && !user.mfa_enabled) console.log("Next: sign in and turn on two-step verification under Account > Security. The inbox stays locked until then.");
