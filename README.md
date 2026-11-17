# Academic-Focused

Tertiary work projects by Vukosi Rikhotso. Each project lives in its own folder and can be built and run on its own.

| Project | What it is | Built with | Live |
| --- | --- | --- | --- |
| [Skyline Weather](skyline%20weather) | Weather dashboard with current conditions, hourly and 7-day forecasts, air quality, and sun and moon times for any place | React, TypeScript, Vite | https://vukosir.github.io/Academic-Focused/ |
| [HP 12C Financial Calculator](HP12CFinancialCalculator) | A faithful web version of the HP 12C: RPN, time value of money, cash flows, bonds, depreciation, dates and statistics | React, TypeScript, Vite | https://hp12c-calculator.netlify.app/ |
| [SpendSmart](SpendSmart) | Android app for tracking expenses, setting budgets, and earning badges for good habits | Kotlin, Room, Material Design | Android app, build in Android Studio |
| [StudyTime](studytime) | Android study companion with a timetable, focus timer, tasks, exams, revision topics, challenges and progress stats | Kotlin, Jetpack Compose | Android app, build in Android Studio |
| [Indulge Cakes Bakery](indulge-bakery) | Website for a custom-cake bakery: cake ranges, collections and gallery, an order bag and quote form, customer accounts, and the owner's orders inbox | React, TanStack Start, Tailwind, Node.js | Self-hosted, run locally with npm |

## Skyline Weather

Live weather from the Open-Meteo API, with search by city, postal code or coordinates, saved places, light and dark themes, and Celsius/Fahrenheit and metric/imperial switches. It needs no API key. GitHub builds and publishes it to GitHub Pages automatically whenever its folder changes on main.

## HP 12C Financial Calculator

A working HP 12C in the browser, with the real RPN stack and the gold f and blue g shift keys. It includes a printed tape, a registers view, an in-app guide with worked examples, and full keyboard support. Everything runs in the browser, so nothing is sent anywhere. It is hosted on Netlify.

Programming keys (R/S, SST, GTO and similar) are not included.

## SpendSmart

An Android app (minimum Android 8.0) for personal expense tracking. It has a dashboard, expense and category management, budget goals, spending insights with charts, and achievement badges. Data is stored on the device with Room. A short demo video, SpendSmart.mp4, is in the project folder.

## StudyTime

A calm, pastel study companion for Android (minimum Android 8.0). Plan a weekly timetable, focus with a Pomodoro, custom or stopwatch timer, track tasks and exams, revise by topic with spaced reviews, and earn points, streaks and badges. Everything is stored on the phone as a JSON file, so it works without an account or an internet connection.

## Indulge Cakes Bakery

The website for Indulge Cakes Bakery, a custom-cake bakery in Johannesburg and Pretoria. It is a server-rendered TanStack Start app (React 19, plain JavaScript) with its own Node server, email/password sign-in with optional two-step verification, and a JSON-file database. Customers browse cake ranges and occasions, build an order bag and send a quote request with inspiration photos; the owner manages requests in a private inbox. No payment is taken online. See SECURITY.md in its folder for how sign-in, sessions and the server endpoints are protected.

## Running a project

Web projects (Skyline Weather and HP 12C) need Node.js 22.12 or newer. Open a terminal in the project folder and run:

    npm install
    npm run dev

On Windows PowerShell, if you see "running scripts is disabled", use npm.cmd instead of npm.

Indulge Cakes Bakery needs Node.js 20 or newer. Copy .env.example to .env before running npm run dev; its README covers production builds and making the owner an admin.

SpendSmart and StudyTime: open the project folder in Android Studio, let Gradle sync, then run it on an emulator or a phone.

Each folder has its own README with more detail.

## Repository layout

    Academic-Focused/
      .github/workflows/     Automatic publishing for Skyline Weather
      HP12CFinancialCalculator/
      indulge-bakery/
      skyline weather/
      SpendSmart/
      studytime/
