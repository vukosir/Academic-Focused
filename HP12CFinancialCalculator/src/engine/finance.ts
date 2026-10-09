/**
 * Pure financial mathematics. Rates are passed as percentages (5 means 5 %),
 * the way the calculator's i register holds them.
 *
 * Time value of money uses the HP 12C equation
 *   PV + (1 + i*S) * PMT * (1 - (1+i)^-n) / i + FV * (1+i)^-n = 0
 * with S = 1 in BEGIN mode and 0 in END mode.
 */
import { actualDays, addMonths, compare, days360, type YMD } from './dates';

export interface Tvm {
  n: number;
  i: number;
  pv: number;
  pmt: number;
  fv: number;
  begin: boolean;
}

export class FinanceError extends Error {
  constructor(readonly code: number) {
    super(`Error ${code}`);
  }
}

const finite = (v: number, code: number): number => {
  if (!Number.isFinite(v)) throw new FinanceError(code);
  return v;
};

/** Residual of the TVM equation at rate r (a fraction). */
function residual(t: Tvm, r: number): number {
  if (Math.abs(r) < 1e-12) return t.pv + t.pmt * t.n + t.fv;
  const s = t.begin ? 1 : 0;
  const g = Math.pow(1 + r, t.n);
  return t.pv + ((1 + r * s) * t.pmt * (1 - 1 / g)) / r + t.fv / g;
}

export function solvePv(t: Tvm): number {
  const r = t.i / 100;
  if (Math.abs(r) < 1e-12) return finite(-(t.pmt * t.n + t.fv), 0);
  const g = Math.pow(1 + r, t.n);
  const a = ((1 + r * (t.begin ? 1 : 0)) * t.pmt) / r;
  return finite(-(a * (g - 1) + t.fv) / g, 0);
}

export function solveFv(t: Tvm): number {
  const r = t.i / 100;
  if (Math.abs(r) < 1e-12) return finite(-(t.pv + t.pmt * t.n), 0);
  const g = Math.pow(1 + r, t.n);
  const a = ((1 + r * (t.begin ? 1 : 0)) * t.pmt) / r;
  return finite(-(t.pv * g + a * (g - 1)), 0);
}

export function solvePmt(t: Tvm): number {
  const r = t.i / 100;
  if (t.n === 0) throw new FinanceError(0);
  if (Math.abs(r) < 1e-12) return finite(-(t.pv + t.fv) / t.n, 0);
  const g = Math.pow(1 + r, t.n);
  return finite((-(t.pv * g + t.fv) * r) / ((g - 1) * (1 + r * (t.begin ? 1 : 0))), 0);
}

/** Number of periods. Like the HP 12C, a fractional answer is rounded up. */
export function solveN(t: Tvm): number {
  const r = t.i / 100;
  let n: number;
  if (Math.abs(r) < 1e-12) {
    if (t.pmt === 0) throw new FinanceError(5);
    n = -(t.pv + t.fv) / t.pmt;
  } else {
    const a = ((1 + r * (t.begin ? 1 : 0)) * t.pmt) / r;
    const g = (a - t.fv) / (a + t.pv);
    if (!(g > 0)) throw new FinanceError(5);
    n = Math.log(g) / Math.log(1 + r);
  }
  if (!Number.isFinite(n) || n < 0) throw new FinanceError(5);
  return Math.ceil(n - 1e-9);
}

/** Finds a root of f between -99 % and a very large rate, preferring the root nearest zero. */
function findRate(f: (r: number) => number, code: number): number {
  const grid: number[] = [];
  for (let k = 0; k <= 400; k++) {
    const x = Math.sinh(k / 40) * 0.05; // dense near zero, reaching about 100x
    grid.push(x);
    if (k > 0 && x < 0.9999) grid.push(-x);
  }
  grid.push(-0.9999);
  grid.sort((a, b) => Math.abs(a) - Math.abs(b) || b - a);
  const vals = new Map<number, number>();
  const at = (x: number) => {
    let v = vals.get(x);
    if (v === undefined) vals.set(x, (v = f(x)));
    return v;
  };
  const ordered = [...new Set(grid)].filter((x) => x > -1).sort((a, b) => a - b);
  let best: [number, number] | null = null;
  for (let k = 0; k < ordered.length - 1; k++) {
    const a = ordered[k]!;
    const b = ordered[k + 1]!;
    const fa = at(a);
    const fb = at(b);
    if (!Number.isFinite(fa) || !Number.isFinite(fb)) continue;
    if (fa === 0) return a;
    if (fa * fb < 0) {
      const dist = Math.min(Math.abs(a), Math.abs(b));
      if (!best || dist < Math.min(Math.abs(best[0]), Math.abs(best[1]))) best = [a, b];
    }
  }
  if (!best) throw new FinanceError(code);
  let [lo, hi] = best;
  let flo = at(lo);
  for (let k = 0; k < 200; k++) {
    const mid = (lo + hi) / 2;
    const fm = f(mid);
    if (fm === 0) return mid;
    if (flo * fm < 0) hi = mid;
    else {
      lo = mid;
      flo = fm;
    }
  }
  return (lo + hi) / 2;
}

export function solveI(t: Tvm): number {
  if (t.n <= 0) throw new FinanceError(5);
  if (t.pv === 0 && t.pmt === 0 && t.fv === 0) throw new FinanceError(5);
  return findRate((r) => residual(t, r), 5) * 100;
}

// ---- Cash flows -----------------------------------------------------------

export interface CashFlows {
  /** Flow amounts: index 0 is CF0. */
  cf: number[];
  /** How many times each flow repeats (same length as cf). */
  nj: number[];
}

function expand({ cf, nj }: CashFlows): number[] {
  const flows: number[] = [];
  cf.forEach((amount, k) => {
    const times = k === 0 ? 1 : (nj[k] ?? 1);
    for (let j = 0; j < times; j++) flows.push(amount);
  });
  return flows;
}

function npvAt(flows: number[], r: number): number {
  let total = 0;
  for (let t = 0; t < flows.length; t++) total += flows[t]! / Math.pow(1 + r, t);
  return total;
}

export function npv(rate: number, cash: CashFlows): number {
  return finite(npvAt(expand(cash), rate / 100), 0);
}

export function irr(cash: CashFlows): number {
  const flows = expand(cash);
  const hasPos = flows.some((f) => f > 0);
  const hasNeg = flows.some((f) => f < 0);
  if (!hasPos || !hasNeg) throw new FinanceError(7);
  return findRate((r) => npvAt(flows, r), 7) * 100;
}

// ---- Depreciation ---------------------------------------------------------

export interface DepreciationResult {
  /** Depreciation for the requested year. */
  amount: number;
  /** Depreciable value left after that year (book value minus salvage). */
  remaining: number;
}

export function depreciation(
  method: 'SL' | 'SOYD' | 'DB',
  cost: number,
  salvage: number,
  life: number,
  year: number,
  factor: number,
): DepreciationResult {
  if (!(life > 0) || !(year >= 1) || !Number.isInteger(year) || !Number.isFinite(cost)) {
    throw new FinanceError(5);
  }
  const depreciable = cost - salvage;
  let cumulative = 0;
  let amount = 0;
  for (let y = 1; y <= year; y++) {
    if (y > life) {
      amount = 0;
    } else if (method === 'SL') {
      amount = depreciable / life;
    } else if (method === 'SOYD') {
      amount = (depreciable * (life - y + 1) * 2) / (life * (life + 1));
    } else {
      const book = cost - cumulative;
      amount = Math.max(0, Math.min((book * (factor / 100)) / life, book - salvage));
    }
    cumulative += amount;
  }
  return { amount, remaining: depreciable - cumulative };
}

// ---- Bonds ----------------------------------------------------------------

export interface BondInput {
  settle: YMD;
  maturity: YMD;
  /** Annual coupon rate in percent. */
  coupon: number;
}

interface Schedule {
  /** Coupons left to pay, including the one at maturity. */
  count: number;
  /** Fraction of a period from settlement to the next coupon. */
  w: number;
  /** Fraction of a period already accrued. */
  accruedFraction: number;
}

function schedule(settle: YMD, maturity: YMD): Schedule {
  if (compare(settle, maturity) >= 0) throw new FinanceError(8);
  let k = 0;
  while (compare(addMonths(maturity, -6 * (k + 1)), settle) > 0) k++;
  const next = addMonths(maturity, -6 * k);
  const prev = addMonths(maturity, -6 * (k + 1));
  const toNext = days360(settle, next);
  const sinceLast = days360(prev, settle);
  return { count: k + 1, w: toNext / 180, accruedFraction: sinceLast / 180 };
}

/** Clean price per 100 of par, and accrued interest, for a yield in percent. */
export function bondPrice(b: BondInput, yieldPct: number): { price: number; accrued: number } {
  const s = schedule(b.settle, b.maturity);
  const y = yieldPct / 100 / 2;
  const c = b.coupon / 2;
  let dirty = 0;
  for (let k = 0; k < s.count; k++) dirty += c / Math.pow(1 + y, k + s.w);
  dirty += 100 / Math.pow(1 + y, s.count - 1 + s.w);
  const accrued = c * s.accruedFraction;
  return { price: finite(dirty - accrued, 0), accrued };
}

/** Yield to maturity, in percent, for a clean price per 100 of par. */
export function bondYield(b: BondInput, price: number): number {
  const target = price;
  const f = (y: number) => bondPrice(b, y * 100).price - target;
  return findRate(f, 5) * 100;
}

export { actualDays };
