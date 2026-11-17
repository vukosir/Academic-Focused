// The cake range, collections and gallery, as designed in Base44. Photos are served
// locally from public/images/cakes/ (the bakery's own TikTok stills, letterbox bars
// trimmed). FILES maps each photo name used in the Base44 design to its local file.
const FILES = {
  "pink-rosette-bento": "little-white-cake",
  "november-bento": "november-bento",
  "bye-29-bento": "black-30th-bento",
  "baby-elephant-bento": "baby-bento",
  "character-bento-cupcakes": "mr-bean-bento",
  "custom-masterpiece": "custom-masterpiece",
  "84th-birthday": "84th-birthday",
  "tuxedo-70th-cake": "tuxedo-cake",
  "40-and-fabulous-cake": "fabulous-40-cake",
  "safari-birthday-cake": "signature-birthday-cake",
  "hello-30-heart-cake": "black-white-heart",
  "oh-baby-shower-cake": "baby-shower-cake",
  "seashell-pearl-birthday-cake": "beige-birthday",
  "engagement-set": "fiancee-cake",
  "frozen-kids-cake": "frozen-kids-cake",
  "rainbow-cake": "rainbow-cake",
  "superhero-theme-cake": "theme-cake",
  "construction-cake": "construction-cake",
  "unicorn-cupcakes": "unicorn-bento",
  "bento-12-cupcakes-combo": "bento-cupcake-combo",
  "floral-cupcakes": "indulge-cupcakes",
  "butterfly-cupcakes": "butterfly-cake",
  "frozen-cupcakes": "frozen-cupcakes",
  "biscuit-box": "biscuits",
  scones: "scones",
  "car-birthday-cake": "range-rover-cake",
};

const img = (name, alt) => ({ src: `/images/cakes/${FILES[name]}.jpg`, small: `/images/cakes/sm/${FILES[name]}.jpg`, alt });

export const PALETTES = [
  { name: "Blush & white", swatches: ["#f2dcdb", "#ffffff"] },
  { name: "Black & gold", swatches: ["#1b1b1b", "#c6a664"] },
  { name: "Baby blue & white", swatches: ["#cfe0ec", "#ffffff"] },
  { name: "Lilac & silver", swatches: ["#dfd6ea", "#c9cdd2"] },
  { name: "Pastel rainbow", swatches: ["#f7d6e0", "#fcebcb", "#dcefd9", "#d6e6f4"] },
  { name: "Your own colours", swatches: ["#b89c7d", "#fbfbfd"] },
];

export const CAKE_RANGES = [
  {
    slug: "bento",
    name: "Bento cakes",
    shortName: "Bento",
    eyebrow: "Mini cakes",
    tagline: "Small cake. Big moment.",
    priceLabel: "From R400",
    from: 400,
    summary: "Mini cakes for one or two people, boxed and finished in any design you choose.",
    intro:
      "A bento is a small, boxed cake made for one or two people: a birthday for two, a thank-you, a quiet celebration. Every one is finished to order, so the design is entirely yours.",
    options: [
      { name: "Bento cake", price: "From R400" },
      { name: "Bento + 6 cupcakes", price: "Quoted per order" },
      { name: "Bento + 12 cupcakes", price: "Quoted per order" },
    ],
    features: ["Sized for one or two people", "Boxed and ready to gift", "Any design you choose", "Can be paired with matching cupcakes"],
    hero: img("pink-rosette-bento", "Pink rosette bento cake with matching cupcakes by Indulge Cakes"),
    images: [
      img("november-bento", "November birthday bento cake set"),
      img("bye-29-bento", "“Bye 29” bento cake with cupcakes"),
      img("baby-elephant-bento", "Baby elephant bento cake"),
      img("pink-rosette-bento", "Pink rosette bento cake and cupcakes"),
      img("character-bento-cupcakes", "Character bento cake with cupcakes"),
    ],
  },
  {
    slug: "celebration",
    name: "Celebration cakes",
    shortName: "Celebration",
    eyebrow: "For the milestones",
    tagline: "Made for the milestones.",
    priceLabel: "From R750",
    from: 750,
    summary:
      "Thirtieths, fortieths, seventieths and eighty-fourths, engagements, baby showers and graduations, hand-finished with gold toppers, sugar flowers and pearls.",
    intro:
      "The cakes that carry the moment: birthdays that end in a zero, engagements, baby showers and graduations. Gold toppers, sugar flowers, pearls and hand-finished detail, built around the design you have in mind.",
    options: [
      { name: "Celebration cake", price: "From R750" },
      { name: "Tiered cake", price: "Quoted per order" },
      { name: "Double cake for two birthdays", price: "Quoted per order" },
    ],
    features: ["Gold toppers, sugar flowers and pearls", "Hand-finished detail", "Tiered and double-cake builds on request", "Any design you choose"],
    hero: img("custom-masterpiece", "White and gold line-art birthday cake by Indulge Cakes"),
    images: [
      img("custom-masterpiece", "White and gold line-art birthday cake"),
      img("84th-birthday", "Rose and gold 84th birthday cake"),
      img("tuxedo-70th-cake", "Tuxedo 70th birthday cake"),
      img("40-and-fabulous-cake", "“40 and Fabulous” birthday cake"),
      img("safari-birthday-cake", "Safari birthday cake"),
      img("hello-30-heart-cake", "“Hello 30” heart cake"),
      img("oh-baby-shower-cake", "“Oh Baby” baby shower cake"),
      img("seashell-pearl-birthday-cake", "Seashell and pearl birthday cake"),
      img("engagement-set", "Engagement cake set"),
    ],
  },
  {
    slug: "kids",
    name: "Kids' theme cakes",
    shortName: "Kids",
    eyebrow: "Little worlds, in cake",
    tagline: "Their favourite world, in cake.",
    priceLabel: "From R400",
    from: 400,
    summary: "Princess castles, superheroes, rainbows, unicorns and construction sites. Bento designs from R400, with matching cupcakes.",
    intro:
      "Princess castles, superheroes, rainbows, unicorns, construction sites: their favourite world built in cake. Bento designs start at R400; full-size party cakes are quoted per order.",
    options: [
      { name: "Kids' bento", price: "From R400" },
      { name: "Full-size party cake", price: "Quoted per order" },
      { name: "Bento + cupcakes", price: "Quoted per order" },
    ],
    features: ["Characters, castles, animals and more", "Bento and full-size party cakes", "Matching cupcakes available", "Any theme you choose"],
    hero: img("frozen-kids-cake", "Frozen castle kids' birthday cake"),
    images: [
      img("frozen-kids-cake", "Frozen castle kids' birthday cake"),
      img("rainbow-cake", "Two-tier rainbow cake"),
      img("superhero-theme-cake", "Superhero theme cake"),
      img("construction-cake", "Construction site cake"),
      img("unicorn-cupcakes", "Unicorn cupcakes"),
      img("bento-12-cupcakes-combo", "Bento cake with twelve cupcakes"),
    ],
  },
  {
    slug: "cupcakes",
    name: "Cupcakes",
    shortName: "Cupcakes",
    eyebrow: "By the box",
    tagline: "Piped, topped, made to match.",
    priceLabel: "Quoted per order",
    from: null,
    summary: "Themed toppers, piped florals and pull-apart designs. Boxes of six or twelve, or one pull-apart centrepiece.",
    intro:
      "Boxes of six or twelve for the table, or a pull-apart design that reads as one cake. Piped florals, themed toppers, and colours matched to your cake.",
    options: [
      { name: "Box of 6 cupcakes", price: "Quoted per order" },
      { name: "Box of 12 cupcakes", price: "Quoted per order" },
      { name: "Pull-apart design", price: "Quoted per order" },
    ],
    features: ["Boxes of six or twelve", "Piped florals and themed toppers", "Pull-apart designs", "Colours matched to your cake"],
    hero: img("floral-cupcakes", "Peach floral piped cupcakes"),
    images: [
      img("floral-cupcakes", "Peach floral piped cupcakes"),
      img("butterfly-cupcakes", "Butterfly pull-apart cupcakes"),
      img("frozen-cupcakes", "Frozen themed cupcakes"),
      img("unicorn-cupcakes", "Unicorn cupcakes"),
    ],
  },
  {
    slug: "biscuits",
    name: "Biscuits & scones",
    shortName: "Biscuits",
    eyebrow: "For the table",
    tagline: "Made for tea, gifts and gatherings.",
    priceLabel: "Quoted per order",
    from: null,
    summary: "Homemade biscuit boxes and fresh scones for tea, gifts and gatherings.",
    intro:
      "Homemade biscuit boxes and fresh scones for a table set for tea, a gift, or a gathering that needs something home-baked alongside the cake.",
    options: [
      { name: "Biscuit box", price: "Quoted per order" },
      { name: "Scones", price: "Quoted per order" },
      { name: "Biscuits & scones together", price: "Quoted per order" },
    ],
    features: ["Baked in small batches", "Gift-ready boxes", "Fresh scones for tea", "Quoted per order"],
    hero: img("biscuit-box", "Homemade biscuit box"),
    images: [img("biscuit-box", "Homemade biscuit box"), img("scones", "Fresh scones")],
  },
];

export const COLLECTIONS = [
  {
    slug: "birthdays",
    name: "Birthdays",
    blurb: "Cake for the year that was, and the one starting.",
    image: img("hello-30-heart-cake", "“Hello 30” heart birthday cake"),
    ranges: ["celebration", "bento"],
  },
  {
    slug: "milestones",
    name: "Milestones",
    blurb: "Thirtieths, seventieths, eighty-fourths: the ones that get a speech.",
    image: img("tuxedo-70th-cake", "Tuxedo 70th birthday cake"),
    ranges: ["celebration"],
  },
  {
    slug: "kids-parties",
    name: "Kids' parties",
    blurb: "Unicorns, castles and construction sites, in cake.",
    image: img("frozen-kids-cake", "Frozen castle kids' cake"),
    ranges: ["kids"],
  },
  {
    slug: "love-and-new-beginnings",
    name: "Love & new beginnings",
    blurb: "Engagements, baby showers and brand-new arrivals.",
    image: img("engagement-set", "Engagement cake set"),
    ranges: ["celebration"],
  },
  {
    slug: "tea-time",
    name: "Tea time",
    blurb: "Biscuit boxes and fresh scones for the table.",
    image: img("scones", "Fresh scones for tea"),
    ranges: ["biscuits", "cupcakes"],
  },
];

export const GALLERY = [
  { ...img("custom-masterpiece", "White and gold line-art birthday cake"), range: "Celebration" },
  { ...img("pink-rosette-bento", "Pink rosette bento cake and cupcakes"), range: "Bento" },
  { ...img("frozen-kids-cake", "Frozen castle kids' cake"), range: "Kids" },
  { ...img("84th-birthday", "Rose and gold 84th birthday cake"), range: "Celebration" },
  { ...img("bye-29-bento", "“Bye 29” bento cake with cupcakes"), range: "Bento" },
  { ...img("superhero-theme-cake", "Superhero theme cake"), range: "Kids" },
  { ...img("tuxedo-70th-cake", "Tuxedo 70th birthday cake"), range: "Celebration" },
  { ...img("baby-elephant-bento", "Baby elephant bento cake"), range: "Bento" },
  { ...img("floral-cupcakes", "Peach floral piped cupcakes"), range: "Cupcakes" },
  { ...img("40-and-fabulous-cake", "“40 and Fabulous” birthday cake"), range: "Celebration" },
  { ...img("rainbow-cake", "Two-tier rainbow cake"), range: "Kids" },
  { ...img("biscuit-box", "Homemade biscuit box"), range: "Biscuits" },
  { ...img("oh-baby-shower-cake", "“Oh Baby” baby shower cake"), range: "Celebration" },
  { ...img("november-bento", "November birthday bento set"), range: "Bento" },
  { ...img("butterfly-cupcakes", "Butterfly pull-apart cupcakes"), range: "Cupcakes" },
  { ...img("engagement-set", "Engagement cake set"), range: "Celebration" },
  { ...img("construction-cake", "Construction site cake"), range: "Kids" },
  { ...img("character-bento-cupcakes", "Character bento cake with cupcakes"), range: "Bento" },
  { ...img("seashell-pearl-birthday-cake", "Seashell and pearl birthday cake"), range: "Celebration" },
  { ...img("unicorn-cupcakes", "Unicorn cupcakes"), range: "Cupcakes" },
  { ...img("scones", "Fresh scones"), range: "Biscuits" },
  { ...img("car-birthday-cake", "Car birthday cake"), range: "Celebration" },
  { ...img("safari-birthday-cake", "Safari birthday cake"), range: "Celebration" },
  { ...img("bento-12-cupcakes-combo", "Bento cake with twelve cupcakes"), range: "Kids" },
];

export function getRange(slug) {
  return CAKE_RANGES.find((range) => range.slug === slug);
}

export function getCollection(slug) {
  return COLLECTIONS.find((collection) => collection.slug === slug);
}

export const SEARCH_ITEMS = [
  ...CAKE_RANGES.map((range) => ({
    label: range.name,
    hint: range.priceLabel,
    to: `/cakes/${range.slug}`,
    kind: "Cake range",
  })),
  ...COLLECTIONS.map((collection) => ({
    label: collection.name,
    hint: "Occasion",
    to: `/collections/${collection.slug}`,
    kind: "Collection",
  })),
  { label: "Gallery", hint: "Every cake we've made", to: "/gallery", kind: "Page" },
  { label: "Compare the range", hint: "Side by side", to: "/compare", kind: "Page" },
  { label: "How to order", hint: "Support & FAQ", to: "/support", kind: "Page" },
  { label: "About Indulge", hint: "The bakery", to: "/about", kind: "Page" },
  { label: "Request a quote", hint: "Send your design brief", to: "/order", kind: "Page" },
];
