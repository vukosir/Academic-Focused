// A customer's private inspiration photos. They are served by /api/order-photos/<name>,
// which only answers the bakery owner (or the customer who sent them).
export default function InspirationPhotos({ names = [], customer }) {
  if (!names.length) return null;
  return (
    <div className="mt-4">
      <p className="text-[10px] uppercase tracking-[0.14em] text-white/40">
        {names.length} inspiration photo{names.length === 1 ? "" : "s"}
      </p>
      <div className="mt-2 grid grid-cols-3 gap-2 sm:grid-cols-5">
        {names.map((name, index) => (
          <a
            key={name}
            href={`/api/order-photos/${name}`}
            target="_blank"
            rel="noreferrer"
            title="Open full size"
            className="relative block aspect-square overflow-hidden rounded-lg bg-white/5"
          >
            <img
              src={`/api/order-photos/${name}`}
              alt={`Inspiration photo ${index + 1}${customer ? ` from ${customer}` : ""}`}
              loading="lazy"
              className="h-full w-full object-cover"
            />
          </a>
        ))}
      </div>
    </div>
  );
}
