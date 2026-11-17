// Decode any image the browser can read (JPG, PNG, WebP, and HEIC on iPhones) and
// re-encode it as a JPEG no larger than 1600px, so uploads stay small and uniform.
const MAX_SIDE = 1600;
const QUALITY = 0.82;
export const MAX_INPUT_BYTES = 25 * 1024 * 1024;

// createImageBitmap decodes off the rendering pipeline (works even while the tab is in
// the background); fall back to an <img> load for formats it can't read (e.g. HEIC on Safari).
async function decode(file) {
  if (typeof createImageBitmap === "function") {
    try {
      const bitmap = await createImageBitmap(file);
      return { source: bitmap, width: bitmap.width, height: bitmap.height, done: () => bitmap.close() };
    } catch {
      /* fall through */
    }
  }
  const url = URL.createObjectURL(file);
  try {
    const img = await new Promise((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = () => reject(new Error("unreadable image"));
      el.src = url;
    });
    return { source: img, width: img.naturalWidth, height: img.naturalHeight, done: () => URL.revokeObjectURL(url) };
  } catch (err) {
    URL.revokeObjectURL(url);
    throw err;
  }
}

export async function toJpeg(file) {
  const { source, width, height, done } = await decode(file);
  try {
    const scale = Math.min(1, MAX_SIDE / Math.max(width, height));
    const canvas = document.createElement("canvas");
    canvas.width = Math.round(width * scale);
    canvas.height = Math.round(height * scale);
    const ctx = canvas.getContext("2d");
    ctx.fillStyle = "#fff"; // transparent PNGs become white, not black
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.drawImage(source, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL("image/jpeg", QUALITY);
  } finally {
    done();
  }
}

export const isImageFile = (file) => file.type.startsWith("image/") || /\.(heic|heif)$/i.test(file.name);
