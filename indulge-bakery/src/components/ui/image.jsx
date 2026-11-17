import { cn } from "@/lib/utils";

// Local stand-in for Base44's media <Image>: same props, plain <img>.
// fittingType "fill" crops to cover, "fit" letterboxes; focal points pick the crop centre.
// Cake photos also have 480px copies (src/lib/cakes.js `small`), offered through srcset.
export function Image({ src, alt = "", className, fittingType = "fill", focalPointX = 0.5, focalPointY = 0.5, priority = false, sizes, style, ...rest }) {
  const small = typeof src === "string" && src.startsWith("/images/cakes/") && !src.includes("/sm/") ? src.replace("/images/cakes/", "/images/cakes/sm/") : null;
  return (
    <img
      src={src}
      srcSet={small ? `${small} 480w, ${src} 720w` : undefined}
      sizes={small ? sizes || "(min-width: 1024px) 40vw, 90vw" : undefined}
      alt={alt}
      loading={priority ? "eager" : "lazy"}
      decoding="async"
      className={cn(fittingType === "fit" ? "object-contain" : "object-cover", className)}
      style={{ objectPosition: `${focalPointX * 100}% ${focalPointY * 100}%`, ...style }}
      {...rest}
    />
  );
}

export default Image;
