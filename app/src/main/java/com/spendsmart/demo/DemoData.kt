package com.spendsmart.demo

object DemoData {

    data class DemoCategory(val name: String, val amount: Double, val colorHex: String)
    data class DemoTransaction(val date: String, val description: String, val category: String, val amount: Double)
    data class DemoMonthly(val month: String, val spending: Double, val income: Double)
    data class DemoBadge(val id: String, val label: String, val desc: String, val earned: Boolean)

    val TRANSACTIONS = listOf(
        // ── APRIL 2026 ──
        DemoTransaction("2026-04-28", "Supermarket — Groceries",        "Groceries",        850.00),
        DemoTransaction("2026-04-27", "Monthly Rent",                   "Rent",           12500.00),
        DemoTransaction("2026-04-26", "Electricity — Prepaid",          "Electricity",      750.00),
        DemoTransaction("2026-04-25", "Water & Rates",                  "Water & Rates",    620.00),
        DemoTransaction("2026-04-24", "Fuel — Petrol Station",          "Fuel",             900.00),
        DemoTransaction("2026-04-22", "Supermarket — Weekly Shop",      "Groceries",        975.00),
        DemoTransaction("2026-04-21", "Mobile Data & Airtime",          "Cellphone",        350.00),
        DemoTransaction("2026-04-19", "Streaming Subscription",         "Subscriptions",    299.00),
        DemoTransaction("2026-04-18", "Pharmacy — Toiletries",          "Personal Care",    410.00),
        DemoTransaction("2026-04-17", "Bus & Taxi Fare",                "Transport",        480.00),
        DemoTransaction("2026-04-15", "Supermarket — Groceries",        "Groceries",        660.00),
        DemoTransaction("2026-04-14", "Takeaway Meal",                  "Takeaways",        320.00),
        DemoTransaction("2026-04-12", "Fuel — Petrol Station",          "Fuel",             850.00),
        DemoTransaction("2026-04-10", "Clothing Store",                 "Clothing",        1200.00),
        DemoTransaction("2026-04-08", "Supermarket — Monthly Stock",    "Groceries",       1850.00),
        DemoTransaction("2026-04-07", "Internet Service Provider",      "Subscriptions",    699.00),
        DemoTransaction("2026-04-05", "Household Cleaning Supplies",    "Home",             390.00),
        DemoTransaction("2026-04-03", "Medical Aid Contribution",       "Medical",         1450.00),
        DemoTransaction("2026-04-02", "Bus & Taxi Fare",                "Transport",        420.00),
        DemoTransaction("2026-04-01", "Takeaway Meal",                  "Takeaways",        275.00),
        // ── MARCH 2026 ──
        DemoTransaction("2026-03-31", "Monthly Rent",                   "Rent",           12500.00),
        DemoTransaction("2026-03-30", "Supermarket — Monthly Stock",    "Groceries",       2100.00),
        DemoTransaction("2026-03-29", "Electricity — Prepaid",          "Electricity",      800.00),
        DemoTransaction("2026-03-28", "Water & Rates",                  "Water & Rates",    620.00),
        DemoTransaction("2026-03-26", "Fuel — Petrol Station",          "Fuel",             950.00),
        DemoTransaction("2026-03-25", "Mobile Data & Airtime",          "Cellphone",        350.00),
        DemoTransaction("2026-03-24", "Streaming Subscription",         "Subscriptions",    299.00),
        DemoTransaction("2026-03-22", "Supermarket — Weekly Shop",      "Groceries",        880.00),
        DemoTransaction("2026-03-20", "Bus & Taxi Fare",                "Transport",        460.00),
        DemoTransaction("2026-03-19", "Pharmacy — Medication",          "Medical",          680.00),
        DemoTransaction("2026-03-17", "Takeaway Meal",                  "Takeaways",        390.00),
        DemoTransaction("2026-03-15", "Supermarket — Groceries",        "Groceries",        730.00),
        DemoTransaction("2026-03-14", "Fuel — Petrol Station",          "Fuel",             875.00),
        DemoTransaction("2026-03-12", "Internet Service Provider",      "Subscriptions",    699.00),
        DemoTransaction("2026-03-10", "Household Repairs",              "Home",             950.00),
        DemoTransaction("2026-03-08", "Clothing Store",                 "Clothing",         890.00),
        DemoTransaction("2026-03-06", "Medical Aid Contribution",       "Medical",         1450.00),
        DemoTransaction("2026-03-05", "Personal Care — Pharmacy",       "Personal Care",    360.00),
        DemoTransaction("2026-03-03", "Bus & Taxi Fare",                "Transport",        440.00),
        DemoTransaction("2026-03-01", "Takeaway Meal",                  "Takeaways",        310.00),
        // ── FEBRUARY 2026 ──
        DemoTransaction("2026-02-28", "Monthly Rent",                   "Rent",           12500.00),
        DemoTransaction("2026-02-27", "Supermarket — Monthly Stock",    "Groceries",       2250.00),
        DemoTransaction("2026-02-26", "Electricity — Prepaid",          "Electricity",      900.00),
        DemoTransaction("2026-02-25", "Water & Rates",                  "Water & Rates",    620.00),
        DemoTransaction("2026-02-24", "Fuel — Petrol Station",          "Fuel",             875.00),
        DemoTransaction("2026-02-22", "Mobile Data & Airtime",          "Cellphone",        350.00),
        DemoTransaction("2026-02-20", "Streaming Subscription",         "Subscriptions",    299.00),
        DemoTransaction("2026-02-19", "Supermarket — Weekly Shop",      "Groceries",        920.00),
        DemoTransaction("2026-02-18", "Bus & Taxi Fare",                "Transport",        490.00),
        DemoTransaction("2026-02-16", "Takeaway Meal",                  "Takeaways",        350.00),
        DemoTransaction("2026-02-15", "Clothing Store",                 "Clothing",        1750.00),
        DemoTransaction("2026-02-14", "Supermarket — Groceries",        "Groceries",        780.00),
        DemoTransaction("2026-02-12", "Internet Service Provider",      "Subscriptions",    699.00),
        DemoTransaction("2026-02-11", "Fuel — Petrol Station",          "Fuel",             900.00),
        DemoTransaction("2026-02-10", "Medical Aid Contribution",       "Medical",         1450.00),
        DemoTransaction("2026-02-08", "Household Cleaning Supplies",    "Home",             410.00),
        DemoTransaction("2026-02-06", "Pharmacy — Toiletries",          "Personal Care",    480.00),
        DemoTransaction("2026-02-04", "Bus & Taxi Fare",                "Transport",        510.00),
        DemoTransaction("2026-02-02", "Takeaway Meal",                  "Takeaways",        295.00),
        DemoTransaction("2026-02-01", "Pharmacy — Medication",          "Medical",          550.00),
        // ── JANUARY 2026 ──
        DemoTransaction("2026-01-31", "Monthly Rent",                   "Rent",           12500.00),
        DemoTransaction("2026-01-30", "Supermarket — Monthly Stock",    "Groceries",       2400.00),
        DemoTransaction("2026-01-29", "Electricity — Prepaid",          "Electricity",      950.00),
        DemoTransaction("2026-01-28", "Water & Rates",                  "Water & Rates",    620.00),
        DemoTransaction("2026-01-27", "Fuel — Petrol Station",          "Fuel",             925.00),
        DemoTransaction("2026-01-25", "Mobile Data & Airtime",          "Cellphone",        350.00),
        DemoTransaction("2026-01-24", "Streaming Subscription",         "Subscriptions",    299.00),
        DemoTransaction("2026-01-22", "Supermarket — Weekly Shop",      "Groceries",       1050.00),
        DemoTransaction("2026-01-20", "Bus & Taxi Fare",                "Transport",        470.00),
        DemoTransaction("2026-01-18", "Takeaway Meal",                  "Takeaways",        420.00),
        DemoTransaction("2026-01-17", "Clothing Store",                 "Clothing",         980.00),
        DemoTransaction("2026-01-15", "Supermarket — Groceries",        "Groceries",        870.00),
        DemoTransaction("2026-01-14", "Internet Service Provider",      "Subscriptions",    699.00),
        DemoTransaction("2026-01-12", "Fuel — Petrol Station",          "Fuel",             875.00),
        DemoTransaction("2026-01-10", "Medical Aid Contribution",       "Medical",         1450.00),
        DemoTransaction("2026-01-09", "Home Maintenance",               "Home",             750.00),
        DemoTransaction("2026-01-07", "Personal Care — Pharmacy",       "Personal Care",    390.00),
        DemoTransaction("2026-01-05", "Bus & Taxi Fare",                "Transport",        450.00),
        DemoTransaction("2026-01-03", "Takeaway Meal",                  "Takeaways",        340.00),
        DemoTransaction("2026-01-01", "Pharmacy — Medication",          "Medical",          480.00),
        // ── DECEMBER 2025 ──
        DemoTransaction("2025-12-31", "Monthly Rent",                   "Rent",           12500.00),
        DemoTransaction("2025-12-30", "Supermarket — Monthly Stock",    "Groceries",       3100.00),
        DemoTransaction("2025-12-29", "Electricity — Prepaid",          "Electricity",     1100.00),
        DemoTransaction("2025-12-28", "Water & Rates",                  "Water & Rates",    620.00),
        DemoTransaction("2025-12-27", "Fuel — Petrol Station",          "Fuel",            1050.00),
        DemoTransaction("2025-12-26", "Clothing Store — Seasonal Sale", "Clothing",        2500.00),
        DemoTransaction("2025-12-24", "Supermarket — Festive Stock",    "Groceries",       2800.00),
        DemoTransaction("2025-12-22", "Mobile Data & Airtime",          "Cellphone",        350.00),
        DemoTransaction("2025-12-21", "Streaming Subscription",         "Subscriptions",    299.00),
        DemoTransaction("2025-12-19", "Bus & Taxi Fare",                "Transport",        550.00),
        DemoTransaction("2025-12-18", "Takeaway Meal",                  "Takeaways",        650.00),
        DemoTransaction("2025-12-16", "Supermarket — Weekly Shop",      "Groceries",       1200.00),
        DemoTransaction("2025-12-14", "Internet Service Provider",      "Subscriptions",    699.00),
        DemoTransaction("2025-12-12", "Fuel — Petrol Station",          "Fuel",             975.00),
        DemoTransaction("2025-12-10", "Medical Aid Contribution",       "Medical",         1450.00),
        DemoTransaction("2025-12-08", "Household — Gift Supplies",      "Home",            1200.00),
        DemoTransaction("2025-12-06", "Personal Care — Pharmacy",       "Personal Care",    520.00),
        DemoTransaction("2025-12-04", "Bus & Taxi Fare",                "Transport",        480.00),
        DemoTransaction("2025-12-02", "Takeaway Meal",                  "Takeaways",        490.00),
        DemoTransaction("2025-12-01", "Pharmacy — Medication",          "Medical",          600.00),
        // ── NOVEMBER 2025 ──
        DemoTransaction("2025-11-30", "Monthly Rent",                   "Rent",           12500.00),
        DemoTransaction("2025-11-29", "Supermarket — Monthly Stock",    "Groceries",       2000.00),
        DemoTransaction("2025-11-28", "Electricity — Prepaid",          "Electricity",      750.00),
        DemoTransaction("2025-11-27", "Water & Rates",                  "Water & Rates",    620.00),
        DemoTransaction("2025-11-26", "Fuel — Petrol Station",          "Fuel",             875.00),
        DemoTransaction("2025-11-24", "Mobile Data & Airtime",          "Cellphone",        350.00),
        DemoTransaction("2025-11-23", "Streaming Subscription",         "Subscriptions",    299.00),
        DemoTransaction("2025-11-22", "Supermarket — Weekly Shop",      "Groceries",        890.00),
        DemoTransaction("2025-11-20", "Bus & Taxi Fare",                "Transport",        460.00),
        DemoTransaction("2025-11-18", "Takeaway Meal",                  "Takeaways",        310.00),
        DemoTransaction("2025-11-16", "Clothing Store",                 "Clothing",        1100.00),
        DemoTransaction("2025-11-14", "Supermarket — Groceries",        "Groceries",        760.00),
        DemoTransaction("2025-11-12", "Internet Service Provider",      "Subscriptions",    699.00),
        DemoTransaction("2025-11-10", "Fuel — Petrol Station",          "Fuel",             850.00),
        DemoTransaction("2025-11-08", "Medical Aid Contribution",       "Medical",         1450.00),
        DemoTransaction("2025-11-07", "Household Cleaning Supplies",    "Home",             380.00),
        DemoTransaction("2025-11-05", "Personal Care — Pharmacy",       "Personal Care",    350.00),
        DemoTransaction("2025-11-03", "Bus & Taxi Fare",                "Transport",        430.00),
        DemoTransaction("2025-11-01", "Takeaway Meal",                  "Takeaways",        280.00),
        DemoTransaction("2025-11-01", "Pharmacy — Medication",          "Medical",          520.00)
    )

    // Category totals — exact sum of transactions above (verified: R 166,633.00)
    val CATEGORIES = listOf(
        DemoCategory("Rent",          75000.00, "#111110"),
        DemoCategory("Groceries",     27065.00, "#1a6b3c"),
        DemoCategory("Medical",       11530.00, "#0369a1"),
        DemoCategory("Fuel",          10900.00, "#b45309"),
        DemoCategory("Clothing",       8420.00, "#065f46"),
        DemoCategory("Subscriptions",  5988.00, "#4338ca"),
        DemoCategory("Transport",      5640.00, "#c2410c"),
        DemoCategory("Electricity",    5250.00, "#6d28d9"),
        DemoCategory("Takeaways",      4430.00, "#0e7490"),
        DemoCategory("Home",           4080.00, "#0f766e"),
        DemoCategory("Water & Rates",  3720.00, "#7e22ce"),
        DemoCategory("Personal Care",  2510.00, "#5b21b6"),
        DemoCategory("Cellphone",      2100.00, "#78350f")
    )

    // Verified: sum(CATEGORIES) = 166,633  ==  sum(TRANSACTIONS)
    // Monthly salary R 35,000 × 6 = R 210,000
    // ACCOUNT_CHANGE = 210,000 − 166,633 = +43,367  (surplus — user is saving)
    // AVG_MONTHLY    = 166,633 / 6       = 27,772.17

    val TOTAL_SPENT    = 166633.00
    val TOTAL_INCOME   = 210000.00
    val ACCOUNT_CHANGE =  43367.00
    val AVG_MONTHLY    =  27772.17

    // Monthly chart data — exact per-month sums (verified against transactions)
    val MONTHLY = listOf(
        DemoMonthly("Nov 25", 25873.00, 35000.00),
        DemoMonthly("Dec 25", 33133.00, 35000.00),
        DemoMonthly("Jan 26", 27268.00, 35000.00),
        DemoMonthly("Feb 26", 27378.00, 35000.00),
        DemoMonthly("Mar 26", 26733.00, 35000.00),
        DemoMonthly("Apr 26", 26248.00, 35000.00)
    )
    // Cross-check: 25873+33133+27268+27378+26733+26248 = 166,633 ✓

    val BUDGET_MIN = 22000.00
    val BUDGET_MAX = 32000.00

    val BADGES = listOf(
        DemoBadge("saver",      "Super Saver",        "Stay under max budget for a full month",            true),
        DemoBadge("tracker",    "Consistent Logger",  "Expenses recorded every month for 6 months",        true),
        DemoBadge("budget_set", "Budget Setter",      "Monthly budget goal has been set",                  true),
        DemoBadge("on_track",   "On Track",           "Finish a month within the min–max budget range",    true),
        DemoBadge("big_saver",  "Big Saver",          "Save over R 5,000 in a single month vs budget",     false),
        DemoBadge("streak",     "5-Day Streak",       "Log an expense every day for 5 days in a row",      false)
    )

    val INSIGHT_SUMMARY = "Over the past 6 months (November 2025 – April 2026) a monthly salary of R 35,000 — totalling R 210,000 — was received. Total spending came to R 166,633, leaving a healthy net surplus of R 43,367. Average monthly spending is R 27,772 against a R 35,000 income, meaning roughly R 7,228 per month is being retained."

    val INSIGHT_MONEY_GOES = "Rent is the single largest expense at R 75,000 — 45% of all spending over the period. Groceries are second at R 27,065, averaging R 4,511 per month. Medical costs (medical aid plus pharmacy) total R 11,530. December spending spiked to R 33,133 due to festive groceries and a R 2,500 seasonal clothing purchase."

    val INSIGHT_STANDOUT = "Subscriptions cost R 5,988 over 6 months — R 998 per month for streaming and internet combined. Fuel averaged R 1,817 per month across the period. Clothing reached R 8,420 total — if the December festive spike is excluded, the monthly average drops to just R 1,184."

    val TOP_SAVING_MOVE = "You are consistently spending R 7,000 – R 9,000 less than your monthly income. Redirecting R 4,000 per month into a dedicated savings account would build a R 24,000 emergency fund in just 6 months without changing your lifestyle at all."

    val OVERSPENDING_FLAGS = listOf(
        Pair("December Groceries",  "R 6,100 spent — nearly double a normal month due to the festive season"),
        Pair("December Clothing",   "R 2,500 seasonal spend in one month — plan this purchase in advance"),
        Pair("Subscriptions",       "R 998/month on streaming + internet — confirm both are actively used")
    )
}
