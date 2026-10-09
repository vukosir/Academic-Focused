# HP 12C Financial Calculator (web)

A web version of the HP 12C financial calculator. It keeps the real calculator's RPN logic, the four-level stack, and the gold `f` and blue `g` shift keys, and runs entirely in the browser. Built with React, TypeScript and Vite, in the same style as Skyline Weather.

**Live app:** https://hp12c-calculator.netlify.app/

This replaces the earlier Windows (WPF) version of this project.

## How it is published

The site is hosted on Netlify as plain static files. There is no server and nothing to configure.

To publish a change:

1. Run `npm run build` in this folder. It writes the site to `dist/`.
2. In Netlify, open the site, go to **Deploys**, and drag the `dist` folder onto the page.

The built site uses relative paths, so the same `dist/` also works on GitHub Pages or any other static host.

## Run it

You need Node.js 22.12 or newer. In this folder:

    npm install
    npm run dev

Then open http://localhost:5174.

On Windows PowerShell, if you see "running scripts is disabled", use `npm.cmd` instead of `npm`.

| Command | What it does |
| --- | --- |
| `npm run dev` | Development server with hot reload on port 5174. |
| `npm run build` | Type-checks, then writes the production site to `dist/`. |
| `npm run preview` | Serves the production build on port 4174. |
| `npm test` | Runs the test suite once. |
| `npm run typecheck` | Type-checks without building. |

## What it does

- **RPN stack** with X, Y, Z, T, stack lift rules, LSTx, x⇄y, R↓, CLx, and the HP's "first key after an error only clears it" behaviour.
- **Time value of money**: n, i, PV, PMT, FV, BEGIN and END, `12×`, `12÷`. Pressing a money key right after another one calculates it; otherwise it stores. A calculated `n` is rounded up, as on the real machine.
- **Amortization** (`f AMORT`) with per-period rounding, and simple interest (`f INT`, 360 and 365 day basis).
- **Cash flows**: CFo, CFj, Nj, NPV and IRR (up to 20 flows plus CFo).
- **Bonds**: PRICE and YTM on a 30/360 semiannual basis, with accrued interest.
- **Depreciation**: SL, SOYD, DB.
- **Dates**: M.DY and D.MY entry, ΔDYS (actual and 30/360), DATE with the weekday.
- **Statistics**: Σ+, Σ−, mean, standard deviation, weighted mean, linear regression estimates and correlation, n!.
- **Math**: √x, x², eˣ, LN, yˣ, 1/x, %, Δ%, %T, INTG, FRAC, RND.
- **Storage**: STO and RCL for R0 to R9 and R.0 to R.9, including STO with `+ − × ÷`.
- **Display**: FIX 0 to 9 (`f 0` to `f 9`) and scientific (`f .`).

Extras that the original did not have: a printed **tape** of everything you do, a **registers** view, an in-app **guide** with worked examples, full **keyboard** support, light and dark themes, a copy button, saved state, and a layout that works from 320 px phones to wide desktops.

## Not included

The programming keys: R/S, SST, BST, GTO, P/R, PSE, x≤y, x=0, and MEM. They are on the keypad for layout fidelity and show a short notice when pressed. Odd-period (fractional `n`) compound interest is not modelled either.

## Keyboard

| Key | Does |
| --- | --- |
| `0-9` `.` | Digits |
| `+ - * /` | Arithmetic |
| `Enter` | ENTER |
| `n` `i` `v` `p` `t` | n, i, PV, PMT, FV |
| `c` `e` | CHS, EEX |
| `f` `g` | Shift keys |
| `s` `r` | STO, RCL |
| `w` `d` | x⇄y, R↓ |
| `y` `%` `q` | yˣ, %, Σ+ |
| `Backspace` | Delete a digit, or clear X |
| `Escape` | CLx |

## Project structure

    src/
      engine/       Pure logic with no DOM, fully unit tested
        calc.ts       The state machine: press(state, key) -> state
        keys.ts       The keypad: main, gold and blue action for every key
        finance.ts    TVM solvers, cash flows, depreciation, bonds
        dates.ts      Date parsing, day counts, weekdays
        format.ts     Number formatting and the typing buffer
      components/   Display, Keypad, side panel (tape, registers, guide)
      storage.ts    Saved state and theme, validated on load
      App.tsx       Layout, keyboard handling

State is saved in `localStorage` on this device only. Corrupted saved data is ignored and the calculator starts fresh.

## Checking it against a real HP 12C

The tests use values worked out independently: a 30-year, 7.5 % loan of 200,000 has a payment of −1,398.43; 100 at 5 % for 10 years is 162.89; the cash flows −100, 60, 60 have an IRR of 13.07 %; and 1 Jan to 1 Mar 2024 is 60 days. Bond prices are checked for consistency (price at a yield equal to the coupon is par, and YTM reverses PRICE) rather than against HP's published figures, so compare a few of your own bond examples with a physical calculator before relying on it.

## Data and privacy

Nothing is sent anywhere. There are no network requests, accounts, or analytics.
