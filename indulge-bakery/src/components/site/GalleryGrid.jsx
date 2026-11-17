import { Image } from "@/components/ui/image";

export default function GalleryGrid({ images, showRange = false }) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 xl:grid-cols-4">
      {images.map((image, index) => (
        <figure key={`${image.src}-${index}`} className="group relative overflow-hidden rounded-[20px] bg-secondary">
          <div className="aspect-[4/5]">
            <Image
              src={image.src}
              alt={image.alt}
              className="h-full w-full transition-transform duration-700 group-hover:scale-[1.04]"
              fittingType="fill"
              sizes="(min-width: 1280px) 25vw, (min-width: 768px) 33vw, 50vw"
            />
          </div>
          {showRange && (
            <figcaption className="pointer-events-none absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/55 to-transparent p-4 pt-10 text-[10px] font-medium uppercase tracking-[0.14em] text-white opacity-0 transition-opacity duration-300 group-hover:opacity-100">
              {image.range}
            </figcaption>
          )}
        </figure>
      ))}
    </div>
  );
}
