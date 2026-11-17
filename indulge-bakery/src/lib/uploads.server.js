import { randomUUID } from "node:crypto";
import { existsSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { DATA_DIR, ensurePrivateDir } from "@/lib/db.server";

// Customers' inspiration photos. They are private (only the owner and the customer
// who sent them can see them), so they live next to db.json, not in public/.
// Files get random names (no user input reaches the path), owner-only permissions,
// and are served only by src/routes/api/order-photos.$name.js after an access check.
const UPLOAD_DIR = join(DATA_DIR, "uploads");
const MAX_BYTES = 1.5 * 1024 * 1024;
const NAME = /^[0-9a-f-]{36}\.jpg$/;

export const isPhotoName = (name) => typeof name === "string" && NAME.test(name);

const badPhoto = () => Object.assign(new Error("One of the photos could not be used. Please try a different picture."), { status: 400 });

// Rebuilds a baseline/progressive JPEG keeping only the segments needed to show it:
// metadata (EXIF with GPS location, comments, ICC/app data) and anything appended
// after the image are dropped. Anything that doesn't parse as a JPEG is rejected.
function cleanJpeg(buffer) {
  if (buffer.length < 4 || buffer[0] !== 0xff || buffer[1] !== 0xd8) throw badPhoto();
  const parts = [Buffer.from([0xff, 0xd8])];
  let i = 2;
  while (i < buffer.length) {
    if (buffer[i] !== 0xff) throw badPhoto();
    const marker = buffer[i + 1];
    if (marker === 0xd9) break; // EOI before SOS: no image data
    if (marker === 0xda) {
      // Start of scan: copy the rest up to and including the end-of-image marker.
      const end = buffer.lastIndexOf(Buffer.from([0xff, 0xd9]));
      if (end < i) throw badPhoto();
      parts.push(buffer.subarray(i, end + 2));
      return Buffer.concat(parts);
    }
    const length = buffer.readUInt16BE(i + 2);
    if (length < 2 || i + 2 + length > buffer.length) throw badPhoto();
    const keep = !((marker >= 0xe1 && marker <= 0xef) || marker === 0xfe); // drop APP1..APP15 and COM
    if (keep) parts.push(buffer.subarray(i, i + 2 + length));
    i += 2 + length;
  }
  throw badPhoto();
}

// The browser shrinks photos to JPEG before sending; re-check here because anyone
// can call the endpoint with anything.
export function decodeJpeg(dataUrl) {
  const buffer = Buffer.from(dataUrl.slice(dataUrl.indexOf(",") + 1), "base64");
  if (buffer.length > MAX_BYTES) throw badPhoto();
  return cleanJpeg(buffer);
}

export function savePhoto(buffer) {
  ensurePrivateDir(UPLOAD_DIR);
  const name = `${randomUUID()}.jpg`;
  writeFileSync(join(UPLOAD_DIR, name), buffer, { mode: 0o600 });
  return name;
}

export function readPhoto(name) {
  if (!isPhotoName(name)) return null;
  const file = join(UPLOAD_DIR, name);
  return existsSync(file) ? readFileSync(file) : null;
}

export function deletePhotos(names = []) {
  for (const name of names) if (isPhotoName(name)) rmSync(join(UPLOAD_DIR, name), { force: true });
}
