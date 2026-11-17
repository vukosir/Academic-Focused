export const BAKERY = {
  name: "Indulge Cakes Bakery",
  short: "Indulge",
  baker: "Innocentia N.",
  areas: "Johannesburg & Pretoria",
  phone: "076 582 5167",
  email: "innocentian18@gmail.com",
  whatsapp: "https://wa.me/27765825167",
  tiktok: "https://www.tiktok.com/@indulge.cakes.bakery",
  tiktokHandle: "@indulge.cakes.bakery",
  tagline: "Life's sweeter with Indulge",
};

export function whatsappLink(message) {
  const text = encodeURIComponent(message || "Hi Indulge, I'd like to ask about a cake.");
  return `${BAKERY.whatsapp}?text=${text}`;
}

export function waNumber(phone = "") {
  const digits = String(phone).replace(/\D/g, "");
  if (!digits) return "";
  return digits.startsWith("0") ? `27${digits.slice(1)}` : digits;
}

export function formatDate(value) {
  if (!value) return "Not set";
  const raw = String(value);
  const date = new Date(raw.includes("T") ? raw : `${raw}T00:00:00Z`);
  if (Number.isNaN(date.getTime())) return raw;
  return date.toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  });
}

export const NAV_LINKS = [
  { to: "/cakes", label: "All cakes" },
  { to: "/collections", label: "Collections" },
  { to: "/compare", label: "Compare" },
  { to: "/gallery", label: "Gallery" },
  { to: "/about", label: "About" },
  { to: "/support", label: "Support" },
];

export const FOOTER_LINKS = [
  {
    title: "The cakes",
    links: [
      { to: "/cakes", label: "All cakes" },
      { to: "/cakes/bento", label: "Bento cakes" },
      { to: "/cakes/celebration", label: "Celebration cakes" },
      { to: "/cakes/kids", label: "Kids' theme cakes" },
      { to: "/cakes/cupcakes", label: "Cupcakes" },
      { to: "/cakes/biscuits", label: "Biscuits & scones" },
    ],
  },
  {
    title: "Explore",
    links: [
      { to: "/collections", label: "Collections" },
      { to: "/gallery", label: "Gallery" },
      { to: "/compare", label: "Compare the range" },
      { to: "/about", label: "About Indulge" },
    ],
  },
  {
    title: "Ordering",
    links: [
      { to: "/order", label: "Request a quote" },
      { to: "/support", label: "How to order" },
      { to: "/support", label: "FAQ" },
      { to: "/privacy", label: "Privacy" },
      { to: "/terms", label: "Terms" },
    ],
  },
];

export const HOW_STEPS = [
  {
    step: "01",
    title: "Choose your cake",
    body: "Pick a range and an option, then choose a colour palette. Add one cake or several to your order bag.",
  },
  {
    step: "02",
    title: "Send your design brief",
    body: "Tell us the date, the occasion, servings and the design you have in mind, and attach up to five inspiration photos.",
  },
  {
    step: "03",
    title: "We confirm the details",
    body: "Indulge replies with the design, the date and the final price before anything is baked, usually on WhatsApp.",
  },
];

export const FAQS = [
  {
    q: "How do I order a cake?",
    a: "Build your order bag on this site and send a quote request, or message us directly on WhatsApp. Every cake is made to order, so we confirm the design, date and final price with you before we begin.",
  },
  {
    q: "Is payment taken on the website?",
    a: "No. No payment is taken on this site. You send a quote request, we come back to you with the design and final price, and payment is arranged with Indulge directly.",
  },
  {
    q: "Can I choose any design?",
    a: "Yes. Every cake is made to order in any design you choose: a design you have seen, an idea of your own, or something we work out together from your inspiration photos.",
  },
  {
    q: "Why do some cakes say “Quoted per order”?",
    a: "Only the starting prices for bento and celebration cakes are published. Everything else depends on size, servings and how detailed the design is, so it is quoted once we know what you need.",
  },
  {
    q: "How far ahead should I order?",
    a: "Send your date as early as you can. Every cake is baked to order, so your date is confirmed as part of your quote. We will tell you honestly whether we can take it on.",
  },
  {
    q: "Collection or delivery?",
    a: "Both are possible. Choose collection or delivery in your request and add your suburb, and we will confirm what is available for your area.",
  },
  {
    q: "Which areas do you serve?",
    a: "Johannesburg and Pretoria.",
  },
  {
    q: "How do I get hold of you?",
    a: `WhatsApp ${BAKERY.phone}, email ${BAKERY.email}, or send a message on TikTok at ${BAKERY.tiktokHandle}.`,
  },
];
