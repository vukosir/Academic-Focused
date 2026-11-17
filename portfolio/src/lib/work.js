// Projects, skills and the gallery. Images live in public/images/work/<name>.jpg
// (960x1200) with 480px copies in public/images/work/sm/. They are screenshots of
// each app; StudyTime has a cover image in the app's own colours instead.
const img = (name, alt) => ({ src: `/images/work/${name}.jpg`, small: `/images/work/sm/${name}.jpg`, alt });

const REPO = "https://github.com/vukosir/Academic-Focused/tree/main";

// The languages Vukosi works in, with each one's familiar colours.
export const LANGUAGES = [
  { name: "Kotlin", swatches: ["#7f52ff", "#e44857"] },
  { name: "Java", swatches: ["#5382a1", "#e76f00"] },
  { name: "C#", swatches: ["#68217a", "#9b4f96"] },
  { name: "Python", swatches: ["#3776ab", "#ffd43b"] },
  { name: "JavaScript", swatches: ["#f7df1e", "#1b1b1b"] },
  { name: "TypeScript", swatches: ["#3178c6", "#1b3a63"] },
  { name: "SQL", swatches: ["#336791", "#cc2927"] },
  { name: "HTML & CSS", swatches: ["#e34f26", "#1572b6"] },
];

export const PROJECTS = [
  {
    slug: "kinplus",
    name: "Kin+",
    shortName: "Kin+",
    eyebrow: "Android app · REST API",
    tagline: "Stay connected. Stay safe.",
    meta: "Kotlin · ASP.NET Core · Team of two",
    platform: "Android + API",
    summary:
      "A South African safety and location-sharing app for families, student groups and neighbourhoods, with an emergency SOS, geofenced Safe Zones and community alerts, backed by our own REST API.",
    intro:
      "Kin+ helps people stay connected with the people they trust and get help quickly in an emergency. Research into Namola, Life360 and Google Family Link showed that each solves part of the problem; Kin+ brings location circles, a deliberate SOS, Safe Zones, Safe Journey monitoring, community safety reports, offline support and a multilingual South African interface together in one app. Built as a two-person project for Open Source Coding at Rosebank College.",
    highlights: [
      { label: "Emergency SOS with siren, strobe and fake call", detail: "Safety" },
      { label: "Safe Zones with geofencing", detail: "Location" },
      { label: "Offline mode that syncs when back online", detail: "Room + WorkManager" },
      { label: "English, isiZulu and Sepedi", detail: "Multilingual" },
    ],
    features: [
      "Android app in Kotlin and Jetpack Compose (MVVM)",
      "Our own REST API in ASP.NET Core (.NET 8) with seven controllers",
      "Firebase sign-in, including Google single sign-on",
      "Real-time notifications with Firebase Cloud Messaging",
      "Automated tests for the API",
    ],
    stack: ["Kotlin", "Jetpack Compose", "C#", "ASP.NET Core", "Firebase", "Room", "Retrofit", "WorkManager"],
    links: {},
    hero: img("kinplus-safety", "Kin+ Safety Hub and SOS screens on two phones"),
    images: [
      img("kinplus-safety", "Kin+ Safety Hub and SOS screens"),
      img("kinplus-fake-call", "Kin+ scheduling a fake incoming call"),
      img("kinplus-help", "Kin+ nearby help, emergency numbers and settings"),
      img("kinplus-circles", "Kin+ circles screen"),
    ],
  },
  {
    slug: "indulge-cakes",
    name: "Indulge Cakes Bakery",
    shortName: "Indulge",
    eyebrow: "Full-stack website",
    tagline: "A small business, online.",
    meta: "React · TanStack Start · Node.js",
    platform: "Web",
    summary:
      "A server-rendered website for a custom-cake bakery in Johannesburg and Pretoria: cake ranges, an order bag and quote form with photo uploads, customer accounts and a private orders inbox for the owner.",
    intro:
      "Indulge Cakes Bakery bakes every cake to order, so the website is built around the quote request: customers browse the ranges, build an order bag, attach inspiration photos and send a design brief. The owner works through requests in a private inbox. The site runs on its own Node server with its own sign-in, sessions and JSON database, and it is hardened against the common web attacks.",
    highlights: [
      { label: "Quote requests with inspiration photo uploads", detail: "Ordering" },
      { label: "Owner's inbox locked behind two-step verification", detail: "Security" },
      { label: "Own sign-in, sessions and password reset", detail: "Auth" },
      { label: "Security headers, CSP and rate limiting", detail: "Hardening" },
    ],
    features: [
      "Server-rendered React 19 pages with TanStack Start",
      "Server functions with input validation (Zod)",
      "Password hashing, server-side sessions and TOTP two-step verification",
      "A security audit log and an automated security check script",
    ],
    stack: ["React", "JavaScript", "TanStack Start", "Tailwind CSS", "Node.js", "Zod"],
    links: {},
    hero: img("indulge-home", "Indulge Cakes Bakery home page"),
    images: [
      img("indulge-home", "Indulge Cakes Bakery home page"),
      img("indulge-range", "Celebration cakes range page"),
      img("indulge-sections", "Indulge Cakes Bakery home page sections"),
    ],
  },
  {
    slug: "skyline-weather",
    name: "Skyline Weather",
    shortName: "Skyline",
    eyebrow: "Web app · Live",
    tagline: "Live weather for anywhere.",
    meta: "React · TypeScript · Live",
    platform: "Web",
    summary:
      "A weather dashboard with current conditions, the next 24 hours, a 7-day forecast, air quality, and sun and moon times for any place in the world, built on live data from the Open-Meteo API.",
    intro:
      "Search by city, postal code or coordinates, save places, and switch between light and dark themes, Celsius and Fahrenheit, metric and imperial. It needs no API key. GitHub Actions type-checks, builds and publishes it to GitHub Pages whenever its code changes.",
    highlights: [
      { label: "Hourly and 7-day forecasts", detail: "Forecast" },
      { label: "Air quality, sun and moon times", detail: "Data" },
      { label: "Search by city, postal code or coordinates", detail: "Search" },
      { label: "Automatic build and deploy", detail: "GitHub Actions" },
    ],
    features: [
      "Live data from the Open-Meteo API, with caching",
      "Saved places and search history",
      "Light and dark themes, °C/°F, metric/imperial",
      "Automated tests and continuous deployment to GitHub Pages",
    ],
    stack: ["React", "TypeScript", "Vite", "Open-Meteo API", "GitHub Actions"],
    links: { live: "https://vukosir.github.io/Academic-Focused/", code: `${REPO}/skyline%20weather` },
    hero: img("skyline-forecast", "Skyline Weather showing the weather in Pretoria"),
    images: [
      img("skyline-forecast", "Skyline Weather current conditions for Pretoria"),
      img("skyline-week", "Skyline Weather 7-day forecast"),
      img("skyline-air-quality", "Skyline Weather air quality, precipitation, sun and moon"),
      img("skyline-desktop", "Skyline Weather on a desktop screen"),
    ],
  },
  {
    slug: "spendsmart",
    name: "SpendSmart",
    shortName: "SpendSmart",
    eyebrow: "Android app",
    tagline: "Spend less. Track more.",
    meta: "Kotlin · Room · Android",
    platform: "Android",
    summary:
      "A personal budget tracker for Android: log expenses by category, set monthly budget goals, see where the money goes in charts, and earn badges for good habits.",
    intro:
      "SpendSmart gives a clear picture of spending: a dashboard of totals and trends, expenses with categories and filters, budget goals with progress for each category, insights written in plain language, and achievement badges that reward staying on track. Everything is stored on the phone with Room, and there is a light and a dark theme.",
    highlights: [
      { label: "Dashboard with spending charts", detail: "Insights" },
      { label: "Monthly budget goals per category", detail: "Budgets" },
      { label: "Expense tracking with filters", detail: "Expenses" },
      { label: "Achievement badges", detail: "Habits" },
    ],
    features: [
      "Native Android app in Kotlin (Android 8.0 and newer)",
      "Local database with Room",
      "Charts with MPAndroidChart",
      "Material Design, light and dark themes",
    ],
    stack: ["Kotlin", "Android SDK", "Room", "Material Design", "MPAndroidChart"],
    links: { code: `${REPO}/SpendSmart` },
    hero: img("spendsmart-dashboard", "SpendSmart dashboard in light and dark themes"),
    images: [
      img("spendsmart-dashboard", "SpendSmart dashboard in light and dark themes"),
      img("spendsmart-budget", "SpendSmart budgets and expenses"),
      img("spendsmart-badges", "SpendSmart achievements and insights"),
    ],
  },
  {
    slug: "studytime",
    name: "StudyTime",
    shortName: "StudyTime",
    eyebrow: "Android app",
    tagline: "A calm study companion.",
    meta: "Kotlin · Jetpack Compose",
    platform: "Android",
    summary:
      "A pastel study companion for Android: plan a weekly timetable, focus with a timer, track tasks and exams, revise by topic with spaced reviews, and earn points, streaks and badges.",
    intro:
      "StudyTime plans study around real life. The planner refuses sessions that clash with classes or exams and gives more time to subjects with an exam coming up. The timer stores timestamps rather than a ticking counter, so it stays correct in the background. Points, streaks and statistics are counted from saved sessions, so they never drift. Everything is saved on the phone, so it works without an account or internet connection.",
    highlights: [
      { label: "Weekly timetable and study planner", detail: "Planning" },
      { label: "Pomodoro, custom and stopwatch timers", detail: "Focus" },
      { label: "Tasks, exams and spaced revision", detail: "Revision" },
      { label: "Points, streaks and badges", detail: "Motivation" },
    ],
    features: [
      "Native Android app in Kotlin and Jetpack Compose",
      "Works fully offline, saved as one JSON file on the phone",
      "Reminders for sessions, deadlines, exams and revision",
      "Accessibility settings: high contrast, larger text, reduced motion",
    ],
    stack: ["Kotlin", "Jetpack Compose", "Android SDK", "JSON storage"],
    links: { code: `${REPO}/studytime` },
    hero: img("studytime-cover", "StudyTime cover in the app's pastel colours"),
    images: [img("studytime-cover", "StudyTime cover in the app's pastel colours")],
  },
  {
    slug: "hp12c-calculator",
    name: "HP 12C Financial Calculator",
    shortName: "HP 12C",
    eyebrow: "Web app · Live",
    tagline: "A classic, rebuilt for the browser.",
    meta: "React · TypeScript · Live",
    platform: "Web",
    summary:
      "A web version of the HP 12C with the real RPN stack and the gold f and blue g shift keys: time value of money, cash flows, bonds, depreciation, dates and statistics.",
    intro:
      "A working HP 12C in the browser. It keeps the real calculator's RPN logic and four-level stack, and adds a printed tape, a registers view, an in-app guide with worked examples and full keyboard support. It runs entirely in the browser, so nothing is sent anywhere. It replaces an earlier Windows (WPF) version of the project and is hosted on Netlify.",
    highlights: [
      { label: "RPN with a four-level stack", detail: "Core" },
      { label: "TVM, cash flows, bonds and depreciation", detail: "Finance" },
      { label: "Printed tape and registers view", detail: "Tools" },
      { label: "Full keyboard support and guide", detail: "Usability" },
    ],
    features: [
      "Financial maths: NPV, IRR, amortisation, bonds and depreciation",
      "Date and statistics functions",
      "Light and dark themes",
      "Static site on Netlify, no server needed",
    ],
    stack: ["React", "TypeScript", "Vite", "Netlify"],
    links: { live: "https://hp12c-calculator.netlify.app/", code: `${REPO}/HP12CFinancialCalculator` },
    hero: img("hp12c-calculator", "HP 12C calculator running in the browser"),
    images: [img("hp12c-calculator", "HP 12C calculator running in the browser"), img("hp12c-guide", "HP 12C in-app guide")],
  },
  {
    slug: "explore-south-africa",
    name: "Explore South Africa",
    shortName: "Explore SA",
    eyebrow: "Full-stack web app",
    tagline: "270 places, one travel guide.",
    meta: "React · TanStack Start · Node.js",
    platform: "Web",
    summary:
      "A full-stack travel guide to South Africa: 270 destinations across the nine provinces, filters by category, wishlists, reviews and a trip planner, with its own sign-in and admin tools.",
    intro:
      "Explore South Africa gathers destinations, provinces, food and culture into one guide. Visitors filter places by category and province, save a wishlist, leave reviews and plan itineraries. It is server-rendered with TanStack Start and runs on its own Node server with email and password sign-in and a JSON database. Destination photos come from Wikimedia Commons with their credits.",
    highlights: [
      { label: "270 destinations in nine provinces", detail: "Content" },
      { label: "Wishlists, reviews and itineraries", detail: "Accounts" },
      { label: "Filters by category and province", detail: "Search" },
      { label: "Admin tools for content", detail: "Admin" },
    ],
    features: [
      "Server-rendered React 19 pages with TanStack Start",
      "Email and password sign-in with hashed passwords",
      "Seed data built from a list of places by script",
      "Photo credits kept for every licensed image",
    ],
    stack: ["React", "JavaScript", "TanStack Start", "Tailwind CSS", "Node.js"],
    links: {},
    hero: img("explore-sa-home", "Explore South Africa home page"),
    images: [img("explore-sa-home", "Explore South Africa home page"), img("explore-sa-destinations", "Explore South Africa destinations list")],
  },
];

export const SKILLS = [
  {
    slug: "android",
    name: "Android development",
    blurb: "Native apps in Kotlin and Jetpack Compose, from offline storage to push notifications.",
    tools: ["Kotlin", "Jetpack Compose", "Room", "WorkManager", "Material Design", "Android Studio"],
    image: img("kinplus-fake-call", "Kin+ on two Android phones"),
    projects: ["kinplus", "spendsmart", "studytime"],
  },
  {
    slug: "web",
    name: "Web development",
    blurb: "Fast, accessible web apps in React, TypeScript and JavaScript.",
    tools: ["React", "TypeScript", "JavaScript", "HTML & CSS", "Tailwind CSS", "Vite", "TanStack Start"],
    image: img("skyline-forecast", "Skyline Weather web app"),
    projects: ["indulge-cakes", "skyline-weather", "hp12c-calculator", "explore-south-africa"],
  },
  {
    slug: "apis",
    name: "APIs & back end",
    blurb: "REST APIs, server functions and sign-in, in C# and Node.js.",
    tools: ["C#", "ASP.NET Core", "Node.js", "REST", "Postman", "Zod"],
    image: img("kinplus-help", "Kin+ screens backed by its REST API"),
    projects: ["kinplus", "indulge-cakes", "explore-south-africa"],
  },
  {
    slug: "databases",
    name: "Databases",
    blurb: "Designing and querying data, from SQL Server and MySQL to on-device storage.",
    tools: ["SQL", "MySQL", "SQL Server", "Room (SQLite)", "Firebase"],
    image: img("spendsmart-budget", "SpendSmart budgets stored with Room"),
    projects: ["spendsmart", "kinplus", "indulge-cakes"],
  },
  {
    slug: "security",
    name: "Security & quality",
    blurb: "Two-step verification, rate limiting, security headers and automated tests.",
    tools: ["TOTP two-step verification", "Content Security Policy", "Rate limiting", "Unit tests", "GitHub Actions"],
    image: img("indulge-home", "Indulge Cakes Bakery website"),
    projects: ["indulge-cakes", "kinplus", "skyline-weather"],
  },
];

export const GALLERY = PROJECTS.flatMap((project) => project.images.map((image) => ({ ...image, project: project.shortName })));

export function getProject(slug) {
  return PROJECTS.find((project) => project.slug === slug);
}

export function getSkill(slug) {
  return SKILLS.find((skill) => skill.slug === slug);
}

export const SEARCH_ITEMS = [
  ...PROJECTS.map((project) => ({
    label: project.name,
    hint: project.meta,
    to: `/projects/${project.slug}`,
    kind: "Project",
  })),
  ...SKILLS.map((skill) => ({
    label: skill.name,
    hint: skill.tools.slice(0, 3).join(", "),
    to: `/skills/${skill.slug}`,
    kind: "Skill",
  })),
  { label: "Gallery", hint: "Screens from every project", to: "/gallery", kind: "Page" },
  { label: "Compare projects", hint: "Side by side", to: "/compare", kind: "Page" },
  { label: "About me", hint: "Education & strengths", to: "/about", kind: "Page" },
  { label: "FAQ", hint: "For recruiters", to: "/faq", kind: "Page" },
  { label: "Contact", hint: "Send me a message", to: "/contact", kind: "Page" },
];
