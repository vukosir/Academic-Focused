import { randomUUID } from "node:crypto";
import { chmodSync, existsSync, mkdirSync, readFileSync, renameSync, statSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

// A small JSON-file store: one array per collection, kept in memory and written
// back atomically on every change. Plenty for this app; swap the body of these
// helpers for a real database (Postgres, SQLite, ...) without touching callers.
//
// Security: the file and its folder are created owner-only (0600 / 0700), and an
// edit made from outside the server (scripts/admin.mjs) is picked up on the next
// call, so the running server never overwrites it.
export const DATA_DIR = process.env.DATA_DIR || join(process.cwd(), "data");
const DB_FILE = join(DATA_DIR, "db.json");
const COLLECTIONS = ["users", "orders", "resets", "sessions"];

let state;
let loadedMtime = 0;

const readJson = (file, fallback) => {
  try {
    return JSON.parse(readFileSync(file, "utf8"));
  } catch {
    return fallback;
  }
};

const mtime = () => {
  try {
    return statSync(DB_FILE).mtimeMs;
  } catch {
    return 0;
  }
};

function load() {
  if (state && mtime() === loadedMtime) return state;
  const stored = existsSync(DB_FILE) ? readJson(DB_FILE, {}) : null;
  state = Object.fromEntries(COLLECTIONS.map((name) => [name, stored?.[name] ?? []]));
  state.meta = stored?.meta ?? {};
  if (!stored) save();
  loadedMtime = mtime();
  return state;
}

export function ensurePrivateDir(dir) {
  mkdirSync(dir, { recursive: true, mode: 0o700 });
  try {
    chmodSync(dir, 0o700);
  } catch {
    /* not supported on this filesystem */
  }
}

function save() {
  ensurePrivateDir(dirname(DB_FILE));
  const tmp = `${DB_FILE}.tmp`;
  writeFileSync(tmp, JSON.stringify(state), { mode: 0o600 });
  renameSync(tmp, DB_FILE);
  loadedMtime = mtime();
}

const matches = (row, where) => Object.entries(where).every(([key, value]) => (Array.isArray(row[key]) ? row[key].includes(value) : row[key] === value));

export function find(collection, { where = {}, test, sort, limit } = {}) {
  let rows = load()[collection].filter((row) => matches(row, where) && (!test || test(row)));
  if (sort) {
    const desc = sort.startsWith("-");
    const key = desc ? sort.slice(1) : sort;
    rows = [...rows].sort((a, b) => {
      const x = a[key] ?? "";
      const y = b[key] ?? "";
      return (x < y ? -1 : x > y ? 1 : 0) * (desc ? -1 : 1);
    });
  }
  return limit ? rows.slice(0, limit) : rows;
}

export const findOne = (collection, options) => find(collection, { ...options, limit: 1 })[0] ?? null;

export function insert(collection, data) {
  const row = { ...data, id: randomUUID(), created_date: new Date().toISOString() };
  load()[collection].push(row);
  save();
  return row;
}

export function update(collection, id, patch) {
  const row = load()[collection].find((r) => r.id === id);
  if (!row) return null;
  Object.assign(row, patch, { updated_date: new Date().toISOString() });
  save();
  return row;
}

export function remove(collection, id) {
  const rows = load()[collection];
  const index = rows.findIndex((r) => r.id === id);
  if (index === -1) return false;
  rows.splice(index, 1);
  save();
  return true;
}

// Removes every row the test matches; returns how many went.
export function removeWhere(collection, test) {
  const data = load();
  const before = data[collection].length;
  data[collection] = data[collection].filter((row) => !test(row));
  const removed = before - data[collection].length;
  if (removed) save();
  return removed;
}
