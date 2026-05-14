# SpendSmart — Personal Budget Tracker

**OPSC6311 — Final POE Submission**
A full-featured, gamified Android budget tracking app built with Kotlin.

---

## Video Demo

> **[Add your unlisted YouTube link here after recording the mobile demo]**

The demonstration video shows the full app running on a physical Android device, including all required features: login, expense logging with receipt photos, categories, budget goals, the category spending chart with min/max goal lines, the budget progress dashboard, and the gamification achievements system.

---

## Purpose of the App

SpendSmart is a personal budget tracker designed to make managing finances engaging rather than stressful. Users can log daily expenses, organise spending into colour-coded categories, set monthly minimum and maximum budget goals, and visualise their spending habits through interactive charts. A gamification layer rewards consistent budgeting behaviour with unlockable badges, encouraging users to stay on track.

The app targets South African users (amounts displayed in ZAR — R) and stores all data locally on the device using Room DB, requiring no internet connection.

---

## Features

### Core (Part 2 + Final POE)
| Feature | Description |
|---|---|
| Register / Login | Username + password authentication; SHA-256 hashing; session persisted in SharedPreferences |
| Expense Logging | Amount, date, start/end time, description, category — all required fields validated |
| Receipt Photos | Attach a photo from the camera or gallery to any expense; viewable from the expenses list |
| Custom Categories | Create colour-coded spending categories; delete with orphaned-expense handling |
| Budget Goals | Set an overall monthly min/max goal; set per-category goals individually |
| Expenses List | Filter all expenses by any custom date range; tap to view full detail + receipt |
| Category Totals | See total spent per category for a user-selected period |
| Room Database | All user data persisted locally — works entirely offline |

### Final POE — New Features
| Feature | Description |
|---|---|
| **Category Spending Chart** | Horizontal bar chart in the Insights tab showing spending per category for a **user-selectable date range**. Dashed **LimitLines** mark the overall monthly min (amber) and max (red) budget goals directly on the chart. |
| **Budget Progress Dashboard** | The Budget tab displays overall and per-category progress bars. Bars turn **red** when over budget, **amber** when under the minimum goal, and **green** when on track. Overspending categories are visually highlighted. |
| **Gamification — 8 Badges** | Badges are awarded automatically based on user actions (see Achievements section below). |

### Own Features (for lecturer)
1. **Demo Mode** — Toggle from the ⋮ menu. Loads a fully pre-populated dataset (37 transactions, 13 categories, 6 months of history) across every tab including charts, insights, and achievements. Demo data is held entirely in `DemoData.kt` and is **never stored in Room DB**. Toggling off returns the user to their real, untouched data.

2. **Dark Mode** — Toggle from the ⋮ menu. Applies a system-wide dark theme via `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES/NO)`, persisted in SharedPreferences so the preference survives app restarts.

---

## Gamification — Achievements

Eight badges are evaluated automatically every time the user saves an expense:

| Badge | ID | Condition |
|---|---|---|
| First Step | `first_expense` | Log your first expense |
| Getting Started | `five_expenses` | Log 5 or more expenses |
| Consistent Logger | `consistent_logger` | Log 10 or more expenses |
| Organiser | `category_creator` | Create 3 or more categories |
| Budget Setter | `budget_set` | Set at least one budget goal |
| Within Budget | `within_budget` | Current month spending ≤ overall max goal |
| Super Saver | `saver_badge` | Current month spending ≤ 80 % of overall max goal |
| Receipt Keeper | `photo_attached` | Attach a photo to an expense |

Earned badges are highlighted in green on the Achievements tab. A progress bar shows how many badges have been earned out of 8.

---

## Category Spending Chart (Insights Tab)

The chart satisfies the Final POE requirement: *"The user must be able to view a graph showing the amount spent per category over a user-selectable period. The graph must also display the minimum and maximum goals."*

- Tap **From** or **To** to open a date picker and change the date range.
- Each horizontal bar represents the total spending for one category in that period.
- A dashed **amber line** marks the overall monthly **minimum** budget goal.
- A dashed **red line** marks the overall monthly **maximum** budget goal.
- If no expenses exist in the selected period, a friendly empty-state message is shown.
- In Demo Mode the chart is populated with the built-in sample data automatically.

---

## Budget Progress Dashboard (Budget Tab)

- Overall monthly spending is shown against the min and max goals with a colour-coded progress bar.
- Each category has its own progress bar showing actual spending vs its individual goal.
- Status labels: **OVER BUDGET** (red), **UNDER MIN** (amber), **ON TRACK** (green), **NO GOAL SET** (grey).

---

## Tech Stack

- **Language:** Kotlin
- **UI:** XML layouts, Material Design 3 Components, ViewBinding
- **Database:** Room (SQLite) — 5 entities, 5 DAOs
- **Charts:** MPAndroidChart (HorizontalBarChart, PieChart, BarChart, LineChart)
- **Concurrency:** Kotlin Coroutines + lifecycleScope
- **CI/CD:** GitHub Actions

---

## Project Structure

```
app/src/main/java/com/spendsmart/
├── data/
│   ├── dao/          (UserDao, CategoryDao, ExpenseDao, BudgetGoalDao, EarnedBadgeDao)
│   ├── entities/     (User, Category, Expense, BudgetGoal, EarnedBadge)
│   └── SpendSmartDatabase.kt
├── demo/
│   └── DemoData.kt             ← isolated sample data, never touches Room DB
├── ui/
│   ├── login/                  (LoginActivity)
│   ├── dashboard/              (DashboardFragment, RecentAdapter)
│   ├── expenses/               (ExpensesFragment, AddExpenseActivity, ExpenseDetailActivity, ExpenseAdapter)
│   ├── categories/             (CategoriesFragment, ManageCategoriesActivity, CategoryAdapter)
│   ├── budget/                 (BudgetFragment, BudgetGoalsActivity, BudgetCatAdapter)
│   ├── insights/               (InsightsFragment ← category chart + date pickers)
│   ├── achievements/           (AchievementsFragment, BadgeAdapter)
│   └── MainActivity.kt
└── utils/
    ├── SessionManager.kt       (SharedPreferences wrapper for login, demo, dark mode)
    ├── HashUtils.kt            (SHA-256 password hashing)
    ├── FormatUtils.kt          (currency, date, colour helpers)
    └── BadgeEvaluator.kt       (evaluates all 8 badge conditions)
```

---

## Design Considerations

**Colour language** — green (#1a6b3c) for healthy spending, amber (#b45309) for caution (under minimum), red (#c0392b) for overspending. Consistent across progress bars, chart limit lines, status labels, and badge states.

**Offline-first** — all data is stored locally in Room DB. No network permissions are required. The app works without any internet connection.

**Input validation** — all screens validate required fields and show user-friendly error messages rather than crashing. Photos are stored as file paths using a FileProvider; paths are validated before display.

**Demo isolation** — `DemoData.kt` is a plain Kotlin object with hardcoded data. It has zero Room DB dependencies, so toggling Demo Mode can never corrupt or alter real user data.

**Badge evaluation timing** — `BadgeEvaluator.evaluate()` is called inside a coroutine immediately after every expense save. This ensures budget-adherence badges (WITHIN_BUDGET, SAVER_BADGE) are always up-to-date based on the latest month total.

---

## GitHub Actions (CI/CD)

The pipeline is defined in `.github/workflows/build.yml` and runs automatically on every push and pull request to `main` or `master`.

### What it does

**Job 1 — Unit Tests (`test`)**
- Runs on `ubuntu-latest`
- Sets up JDK 17 (Temurin distribution)
- Caches Gradle packages to speed up subsequent runs
- Executes `./gradlew test --stacktrace`
- Uploads the HTML test report as a build artifact for review

**Job 2 — Build APK (`build`)**
- Runs after the test job succeeds (dependency enforced with `needs: test`)
- Assembles the debug APK: `./gradlew assembleDebug --stacktrace`
- Uploads `app-debug.apk` as a downloadable build artifact named **SpendSmart-debug**

### Why GitHub Actions

GitHub Actions ensures the app compiles and all unit tests pass on a clean Linux environment — not just the developer's local machine. This catches environment-specific build failures early and provides a downloadable APK for every successful build without needing Android Studio installed on the reviewer's machine.

---

## Unit Tests

**File:** `app/src/test/java/com/spendsmart/SpendSmartTest.kt`

| Test Class | Tests |
|---|---|
| `FormatUtilsTest` | Currency formatting, compact notation, date parsing, invalid inputs |
| `HashUtilsTest` | SHA-256 output non-empty, deterministic, distinct inputs, correct length |
| `BudgetLogicTest` | Over-budget detection, under-min detection, on-track detection, progress % clamping |

Run locally with: `./gradlew test`

---

## How to Run

1. Clone the repository
2. Open in **Android Studio Hedgehog** (or newer)
3. Sync Gradle (File → Sync Project with Gradle Files)
4. Run on a physical Android device (API 26 / Android 8.0+) or an emulator
5. Register a new account and start adding expenses
6. Use the ⋮ overflow menu → **Toggle Demo Mode** to preview all features with sample data
7. Use the ⋮ menu → **Toggle Dark Mode** to switch themes

---

## Submission Checklist (Final POE)

- [x] Kotlin source code on GitHub (no zip files)
- [x] Code comments and logging (`Log.d` throughout)
- [x] README with purpose, design considerations, GitHub Actions documentation
- [x] Category spending chart with user-selectable date range and min/max goal lines
- [x] Budget progress dashboard with visual overspending highlights
- [x] Gamification — 8 badges, all evaluated automatically
- [x] Two own features documented: Demo Mode + Dark Mode
- [x] App icon and image assets (`ic_launcher`, `ic_launcher_round`)
- [x] GitHub Actions CI/CD pipeline (tests + APK build)
- [x] Unit tests (FormatUtils, HashUtils, BudgetLogic)
- [ ] Video demo link (add after recording on physical device)
- [ ] Built APK (downloaded from GitHub Actions artifacts)
