// Who the site is about. Facts come from Vukosi's CV; contact details are limited
// to email, LinkedIn and GitHub (no phone number on the public site).
export const PROFILE = {
  name: "Vukosi Rikhotso",
  fullName: "Vukosi Bright Rikhotso",
  firstName: "Vukosi",
  role: "Software Developer",
  headline: "IT student · Diploma in IT (Software Development)",
  college: "Rosebank College",
  location: "Pretoria, Gauteng",
  email: "vukosir@icloud.com",
  linkedin: "https://www.linkedin.com/in/vukosi-rikhotso-1b016034a",
  linkedinHandle: "vukosi-rikhotso",
  github: "https://github.com/vukosir",
  githubHandle: "vukosir",
  languages: "English, Xitsonga & isiZulu",
  tagline: "Building software people can rely on.",
};

export function mailtoLink(subject) {
  return `mailto:${PROFILE.email}${subject ? `?subject=${encodeURIComponent(subject)}` : ""}`;
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
  { to: "/projects", label: "Projects" },
  { to: "/skills", label: "Skills" },
  { to: "/compare", label: "Compare" },
  { to: "/gallery", label: "Gallery" },
  { to: "/about", label: "About" },
  { to: "/faq", label: "FAQ" },
];

export const FOOTER_LINKS = [
  {
    title: "Projects",
    links: [
      { to: "/projects", label: "All projects" },
      { to: "/projects/kinplus", label: "Kin+" },
      { to: "/projects/indulge-cakes", label: "Indulge Cakes Bakery" },
      { to: "/projects/skyline-weather", label: "Skyline Weather" },
      { to: "/projects/spendsmart", label: "SpendSmart" },
      { to: "/projects/studytime", label: "StudyTime" },
    ],
  },
  {
    title: "Explore",
    links: [
      { to: "/skills", label: "Skills" },
      { to: "/gallery", label: "Gallery" },
      { to: "/compare", label: "Compare projects" },
      { to: "/about", label: "About me" },
    ],
  },
  {
    title: "Get in touch",
    links: [
      { to: "/contact", label: "Contact me" },
      { to: "/faq", label: "FAQ" },
      { to: "/privacy", label: "Privacy" },
    ],
  },
];

export const HOW_STEPS = [
  {
    step: "01",
    title: "Understand the problem",
    body: "I start with the people who will use it: what exists already, what is missing, and what the software has to do. The requirements come before the code.",
  },
  {
    step: "02",
    title: "Build in small steps",
    body: "A working version early, then small, tested improvements in clean, readable code, with every change tracked in Git on GitHub.",
  },
  {
    step: "03",
    title: "Test and ship",
    body: "Automated tests, security checks and automatic deployment, so what goes live keeps working after it is released.",
  },
];

export const EDUCATION = [
  {
    title: "Diploma in IT (Software Development)",
    place: "Rosebank College",
    years: "2024 – 2026",
    body: "Programming, database design, systems analysis, and mobile and web development, with practical projects such as Kin+.",
  },
  {
    title: "National Senior Certificate",
    place: "Marholeni High School",
    years: "2022",
    body: "Matric.",
  },
];

export const COMPETENCIES = [
  { title: "Analytical problem-solving", body: "Debugging, systems analysis and process improvement." },
  { title: "Technical teamwork", body: "Agile-style teamwork and cross-functional coordination." },
  { title: "Database & systems thinking", body: "Data organisation, integrity and workflow optimisation." },
  { title: "Clear communication", body: "Customer and stakeholder engagement, in three languages." },
  { title: "Time & task management", body: "Meeting deadlines in fast-paced environments." },
  { title: "Leadership & initiative", body: "Taking ownership in both academic and work settings." },
];

export const FAQS = [
  {
    q: "What kind of role are you looking for?",
    a: "Internships, learnerships and junior software developer roles where I can contribute to real products while learning from experienced developers. I enjoy both mobile and web work.",
  },
  {
    q: "Where are you studying, and when do you finish?",
    a: "I'm in the final year of a Diploma in IT (Software Development) at Rosebank College, which I complete in 2026.",
  },
  {
    q: "Which languages and tools do you use?",
    a: "Java, C#, Python, Kotlin, JavaScript, TypeScript, SQL and HTML. Day to day I work in Visual Studio, VS Code and Android Studio, with GitHub, MySQL, SQL Server and Postman.",
  },
  {
    q: "Can I see your code?",
    a: `Yes. Several of my projects are public on GitHub at github.com/${PROFILE.githubHandle}, and Skyline Weather and the HP 12C calculator are live on the web.`,
  },
  {
    q: "Have you worked in a team?",
    a: "Yes. Kin+ was a two-person project, taken from research and requirements through to an Android app and its own REST API. Outside of IT I have hands-on experience in administrative operations, digital marketing and IT project coordination.",
  },
  {
    q: "Where are you based?",
    a: `${PROFILE.location}, South Africa.`,
  },
  {
    q: "Which languages do you speak?",
    a: "English, Xitsonga and isiZulu.",
  },
  {
    q: "Can you provide references?",
    a: "Yes, references are available on request.",
  },
  {
    q: "How do I get hold of you?",
    a: `Send a message through the contact page, email ${PROFILE.email}, or connect with me on LinkedIn.`,
  },
];
