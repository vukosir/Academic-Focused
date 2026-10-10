/**
 * The calculator itself: a pure state machine. `press(state, keyId)` returns the
 * next state. Nothing here touches the DOM, so every behaviour is unit tested.
 */
import {
  DAY_NAMES,
  actualDays,
  addDays,
  dateToNumber,
  dayOfWeek,
  days360,
  parseDate,
} from './dates';
import {
  FinanceError,
  bondPrice,
  bondYield,
  depreciation,
  irr,
  npv,
  solveFv,
  solveI,
  solveN,
  solvePmt,
  solvePv,
  type Tvm,
} from './finance';
import { entryValue, formatEntry, formatNumber, type DisplayMode, type Entry } from './format';
import { KEY_BY_ID } from './keys';

export interface TapeLine {
  label: string;
  value: string;
}

export interface Pending {
  kind: 'STO' | 'RCL';
  op: '+' | '-' | '*' | '/' | null;
  dot: boolean;
}

export interface CalcState {
  /** X, Y, Z, T. */
  stack: [number, number, number, number];
  lastX: number;
  entry: Entry | null;
  /** True when the next number typed pushes the stack up. */
  lift: boolean;
  shift: 'f' | 'g' | null;
  pending: Pending | null;
  /** R0-R9, then R.0-R.9. Statistics use R1-R6. */
  regs: number[];
  n: number;
  i: number;
  pv: number;
  pmt: number;
  fv: number;
  begin: boolean;
  /** True when the previous key was a time-value key, so the next one computes. */
  lastTvm: boolean;
  /** CF0 first, then CF1...; nj holds the repeat counts. */
  cf: number[];
  nj: number[];
  /** Periods amortized so far in the current schedule. */
  amortized: number;
  mode: DisplayMode;
  dmy: boolean;
  /** Set right after DATE so the display shows the date and weekday. */
  dateShown: number | null;
  error: number | null;
  notice: string | null;
  tape: TapeLine[];
}

export const MAX_FLOWS = 20;
const TAPE_LIMIT = 200;
const REGISTER_COUNT = 20;

export function initialState(): CalcState {
  return {
    stack: [0, 0, 0, 0],
    lastX: 0,
    entry: null,
    lift: false,
    shift: null,
    pending: null,
    regs: new Array<number>(REGISTER_COUNT).fill(0),
    n: 0,
    i: 0,
    pv: 0,
    pmt: 0,
    fv: 0,
    begin: false,
    lastTvm: false,
    cf: [0],
    nj: [1],
    amortized: 0,
    mode: { kind: 'FIX', digits: 2 },
    dmy: false,
    dateShown: null,
    error: null,
    notice: null,
    tape: [],
  };
}

// ---- Display --------------------------------------------------------------

export function displayText(s: CalcState): string {
  if (s.error !== null) return `Error ${s.error}`;
  if (s.entry) return formatEntry(s.entry);
  if (s.dateShown !== null) return formatNumber(s.stack[0], { kind: 'FIX', digits: 6 });
  return formatNumber(s.stack[0], s.mode);
}

export function weekdayLabel(s: CalcState): string {
  return s.dateShown !== null ? (DAY_NAMES[s.dateShown] ?? '') : '';
}

// ---- Small helpers --------------------------------------------------------

class CalcError extends Error {
  constructor(readonly code: number) {
    super(`Error ${code}`);
  }
}

function fail(code: number): never {
  throw new CalcError(code);
}

const num = (v: number): number => {
  if (!Number.isFinite(v)) fail(0);
  return v;
};

function tvmOf(s: CalcState): Tvm {
  return { n: s.n, i: s.i, pv: s.pv, pmt: s.pmt, fv: s.fv, begin: s.begin };
}

function log(s: CalcState, label: string, value?: string): void {
  s.tape.push({ label, value: value ?? formatNumber(s.stack[0], s.mode) });
  if (s.tape.length > TAPE_LIMIT) s.tape.splice(0, s.tape.length - TAPE_LIMIT);
}

/** Moves the stack up one level, discarding T. */
function liftStack(s: CalcState): void {
  const [x, y, z] = s.stack;
  s.stack = [x, x, y, z];
}

/** Puts a result in X, lifting the stack first if it is enabled. */
function pushResult(s: CalcState, value: number): void {
  if (s.lift) liftStack(s);
  s.stack[0] = num(value);
  s.lift = true;
}

/** Replaces X without lifting (unary functions). */
function replaceX(s: CalcState, value: number): void {
  s.lastX = s.stack[0];
  s.stack[0] = num(value);
  s.lift = true;
}

/** Replaces X and Y with a binary result, dropping the stack. */
function binary(s: CalcState, result: number): void {
  const [x, , z, t] = s.stack;
  s.lastX = x;
  s.stack = [num(result), z, t, t];
  s.lift = true;
}

function commit(s: CalcState): void {
  if (!s.entry) return;
  const text = formatEntry(s.entry);
  s.stack[0] = entryValue(s.entry);
  s.entry = null;
  s.lift = true;
  s.tape.push({ label: '', value: text });
}

function roundToDisplay(s: CalcState, v: number): number {
  const digits = s.mode.digits;
  return s.mode.kind === 'FIX'
    ? Number(v.toFixed(Math.min(digits, 20)))
    : Number(v.toExponential(digits));
}

function toInt(v: number, min: number, max: number): number {
  const k = Math.trunc(v);
  if (k < min || k > max) fail(0);
  return k;
}

// ---- Registers ------------------------------------------------------------

const TVM_KEYS = ['n', 'i', 'pv', 'pmt', 'fv'] as const;
type TvmKey = (typeof TVM_KEYS)[number];

function registerIndex(digit: number, dot: boolean): number {
  return dot ? 10 + digit : digit;
}

function applyOp(op: Pending['op'], current: number, x: number): number {
  switch (op) {
    case '+':
      return current + x;
    case '-':
      return current - x;
    case '*':
      return current * x;
    case '/':
      if (x === 0) fail(0);
      return current / x;
    default:
      return x;
  }
}

function handlePending(s: CalcState, p: Pending, keyId: string): void {
  s.pending = null;
  const label = `${p.kind}${p.op ? ` ${p.op === '*' ? '×' : p.op === '/' ? '÷' : p.op}` : ''}`;

  if (!p.op && p.kind === 'STO' && ['add', 'sub', 'mul', 'div'].includes(keyId)) {
    const map = { add: '+', sub: '-', mul: '*', div: '/' } as const;
    s.pending = { ...p, op: map[keyId as keyof typeof map] };
    return;
  }
  if (keyId === 'dot' && !p.dot) {
    s.pending = { ...p, dot: true };
    return;
  }

  commit(s);
  let slot: { get(): number; set(v: number): void; name: string } | null = null;

  if (/^\d$/.test(keyId)) {
    const idx = registerIndex(Number(keyId), p.dot);
    slot = {
      get: () => s.regs[idx] ?? 0,
      set: (v) => void (s.regs[idx] = v),
      name: `${p.dot ? '.' : ''}${keyId}`,
    };
  } else if ((TVM_KEYS as readonly string[]).includes(keyId) && !p.dot) {
    const key = keyId as TvmKey;
    slot = { get: () => s[key], set: (v) => void (s[key] = v), name: key.toUpperCase() };
  }
  if (!slot) return; // any other key cancels

  if (p.kind === 'STO') {
    slot.set(num(applyOp(p.op, slot.get(), s.stack[0])));
    s.lift = true;
    log(s, `${label} ${slot.name}`);
  } else {
    pushResult(s, slot.get());
    log(s, `RCL ${slot.name}`);
  }
}

// ---- Statistics -----------------------------------------------------------

function statsN(s: CalcState): number {
  const n = s.regs[1] ?? 0;
  if (n < 1) fail(2);
  return n;
}

function regression(s: CalcState) {
  const n = statsN(s);
  if (n < 2) fail(2);
  const [, sx = 0, sxx = 0, sy = 0, syy = 0, sxy = 0] = s.regs.slice(1, 7);
  const cxx = sxx - (sx * sx) / n;
  const cyy = syy - (sy * sy) / n;
  const cxy = sxy - (sx * sy) / n;
  if (cxx === 0) fail(2);
  const b = cxy / cxx;
  const a = sy / n - (b * sx) / n;
  const r = cyy === 0 ? 0 : cxy / Math.sqrt(cxx * cyy);
  return { a, b, r };
}

function addStats(s: CalcState, sign: 1 | -1): void {
  const [x, y] = s.stack;
  const r = s.regs;
  r[1] = (r[1] ?? 0) + sign;
  r[2] = (r[2] ?? 0) + sign * x;
  r[3] = (r[3] ?? 0) + sign * x * x;
  r[4] = (r[4] ?? 0) + sign * y;
  r[5] = (r[5] ?? 0) + sign * y * y;
  r[6] = (r[6] ?? 0) + sign * x * y;
  s.lastX = x;
  s.stack[0] = r[1];
  s.lift = false;
}

function factorial(x: number): number {
  const k = toInt(x, 0, 69);
  if (k !== x) fail(0);
  let f = 1;
  for (let j = 2; j <= k; j++) f *= j;
  return f;
}

// ---- Dates ----------------------------------------------------------------

function dateAt(s: CalcState, value: number) {
  const d = parseDate(value, s.dmy);
  if (!d) fail(8);
  return d;
}

// ---- Actions --------------------------------------------------------------

function tvmKey(s: CalcState, key: TvmKey, wasTvm: boolean): void {
  commit(s);
  s.lastTvm = true;
  if (!wasTvm) {
    s[key] = s.stack[0];
    s.amortized = 0;
    s.lift = true;
    log(s, key.toUpperCase());
    return;
  }
  const t = tvmOf(s);
  try {
    let value: number;
    switch (key) {
      case 'n':
        value = solveN(t);
        break;
      case 'i':
        value = solveI(t);
        break;
      case 'pv':
        value = solvePv(t);
        break;
      case 'pmt':
        value = solvePmt(t);
        break;
      default:
        value = solveFv(t);
    }
    s[key] = value;
    s.amortized = 0;
    pushResult(s, value);
    log(s, key.toUpperCase());
  } catch (e) {
    if (e instanceof FinanceError) fail(e.code);
    throw e;
  }
}

function amortize(s: CalcState): void {
  const periods = toInt(s.stack[0], 0, 1200);
  const r = s.i / 100;
  const digits = s.mode.kind === 'FIX' ? s.mode.digits : 2;
  const round = (v: number) => Number(v.toFixed(digits));
  let balance = s.pv;
  let totalInterest = 0;
  let totalPrincipal = 0;
  for (let k = 0; k < periods; k++) {
    const first = s.amortized + k === 0;
    const interest = s.begin && first ? 0 : round(-balance * r);
    const principal = s.pmt - interest;
    totalInterest += interest;
    totalPrincipal += principal;
    balance += principal;
  }
  s.amortized += periods;
  s.pv = balance;
  s.n = periods;
  s.lastX = s.stack[0];
  s.stack = [totalInterest, totalPrincipal, periods, s.stack[2]];
  s.lift = true;
  log(s, 'AMORT');
  log(s, 'PRIN', formatNumber(totalPrincipal, s.mode));
}

function simpleInterest(s: CalcState): void {
  const base = -s.pv * (s.i / 100) * s.n;
  const i360 = base / 360;
  const i365 = base / 365;
  if (s.lift) liftStack(s);
  s.stack = [num(i360), num(i365), s.stack[1], s.stack[2]];
  s.lift = true;
  log(s, 'INT 360');
  log(s, 'INT 365', formatNumber(i365, s.mode));
}

function cashFlowKey(s: CalcState, action: 'cf0' | 'cfj' | 'nj'): void {
  const x = s.stack[0];
  if (action === 'cf0') {
    s.cf = [x];
    s.nj = [1];
    log(s, 'CFo');
  } else if (action === 'cfj') {
    if (s.cf.length > MAX_FLOWS) fail(4);
    s.cf.push(x);
    s.nj.push(1);
    log(s, `CF${s.cf.length - 1}`);
  } else {
    if (s.cf.length < 2) fail(4);
    const k = toInt(x, 1, 99);
    s.nj[s.nj.length - 1] = k;
    log(s, `N${s.cf.length - 1}`);
  }
  s.lift = true;
}

function depreciationKey(s: CalcState, method: 'SL' | 'SOYD' | 'DB'): void {
  const year = s.stack[0];
  try {
    const r = depreciation(method, s.pv, s.fv, s.n, year, s.i);
    s.lastX = year;
    s.stack = [r.amount, r.remaining, s.stack[1], s.stack[2]];
    s.lift = true;
    log(s, method);
    log(s, 'REMAIN', formatNumber(r.remaining, s.mode));
  } catch (e) {
    if (e instanceof FinanceError) fail(e.code);
    throw e;
  }
}

function bondKey(s: CalcState, kind: 'price' | 'ytm'): void {
  const settle = dateAt(s, s.stack[1]);
  const maturity = dateAt(s, s.stack[0]);
  const b = { settle, maturity, coupon: s.pmt };
  try {
    if (kind === 'price') {
      const { price, accrued } = bondPrice(b, s.i);
      s.lastX = s.stack[0];
      s.stack = [price, accrued, s.stack[2], s.stack[3]];
      s.pv = price;
      log(s, 'PRICE');
      log(s, 'ACCRD', formatNumber(accrued, s.mode));
    } else {
      const y = bondYield(b, s.pv);
      s.lastX = s.stack[0];
      s.stack = [y, s.stack[1], s.stack[2], s.stack[3]];
      s.i = y;
      log(s, 'YTM');
    }
  } catch (e) {
    if (e instanceof FinanceError) fail(e.code);
    throw e;
  }
  s.lift = true;
}

function dateKey(s: CalcState): void {
  const start = dateAt(s, s.stack[1]);
  const days = Math.trunc(s.stack[0]);
  const end = addDays(start, days);
  if (!end) fail(8);
  s.lastX = s.stack[0];
  s.stack = [dateToNumber(end, s.dmy), s.stack[2], s.stack[3], s.stack[3]];
  s.dateShown = dayOfWeek(end);
  s.lift = true;
  log(s, 'DATE', `${formatNumber(s.stack[0], { kind: 'FIX', digits: 6 })} ${DAY_NAMES[s.dateShown]}`);
}

function daysKey(s: CalcState): void {
  const from = dateAt(s, s.stack[1]);
  const to = dateAt(s, s.stack[0]);
  s.lastX = s.stack[0];
  s.stack = [actualDays(from, to), days360(from, to), s.stack[2], s.stack[3]];
  s.lift = true;
  log(s, 'ΔDYS');
  log(s, '30/360', formatNumber(s.stack[1], { kind: 'FIX', digits: 0 }));
}

function unary(s: CalcState, label: string, f: (x: number) => number): void {
  replaceX(s, f(s.stack[0]));
  log(s, label);
}

const ACTIONS: Record<string, (s: CalcState) => void> = {
  enter: (s) => {
    commit(s);
    liftStack(s);
    s.lift = false;
  },
  clx: (s) => {
    s.entry = null;
    s.stack[0] = 0;
    s.lift = false;
  },
  xy: (s) => {
    commit(s);
    const [x, y, z, t] = s.stack;
    s.stack = [y, x, z, t];
    s.lift = true;
  },
  rdown: (s) => {
    commit(s);
    const [x, y, z, t] = s.stack;
    s.stack = [y, z, t, x];
    s.lift = true;
  },
  lstx: (s) => {
    commit(s);
    pushResult(s, s.lastX);
  },
  chs: (s) => {
    if (s.entry) {
      if (s.entry.exp !== null) s.entry.expNeg = !s.entry.expNeg;
      else s.entry.neg = !s.entry.neg;
      return;
    }
    s.stack[0] = -s.stack[0];
  },
  eex: (s) => {
    if (!s.entry) {
      if (s.lift) liftStack(s);
      s.entry = { neg: false, mant: '1', exp: '', expNeg: false };
    } else if (s.entry.exp === null) {
      if (s.entry.mant === '') s.entry.mant = '1';
      s.entry.exp = '';
    }
  },
  dot: (s) => {
    if (!s.entry) {
      if (s.lift) liftStack(s);
      s.entry = { neg: false, mant: '0.', exp: null, expNeg: false };
    } else if (s.entry.exp === null && !s.entry.mant.includes('.')) {
      s.entry.mant = (s.entry.mant === '' ? '0' : s.entry.mant) + '.';
    }
  },
  add: (s) => {
    commit(s);
    binary(s, s.stack[1] + s.stack[0]);
    log(s, '+');
  },
  sub: (s) => {
    commit(s);
    binary(s, s.stack[1] - s.stack[0]);
    log(s, '−');
  },
  mul: (s) => {
    commit(s);
    binary(s, s.stack[1] * s.stack[0]);
    log(s, '×');
  },
  div: (s) => {
    commit(s);
    if (s.stack[0] === 0) fail(0);
    binary(s, s.stack[1] / s.stack[0]);
    log(s, '÷');
  },
  pow: (s) => {
    commit(s);
    const [x, y] = s.stack;
    if (y === 0 && x <= 0) fail(0);
    if (y < 0 && !Number.isInteger(x)) fail(0);
    binary(s, Math.pow(y, x));
    log(s, 'yˣ');
  },
  inv: (s) => {
    commit(s);
    unary(s, '1/x', (x) => (x === 0 ? fail(0) : 1 / x));
  },
  sqrt: (s) => {
    commit(s);
    unary(s, '√x', (x) => (x < 0 ? fail(0) : Math.sqrt(x)));
  },
  sq: (s) => {
    commit(s);
    unary(s, 'x²', (x) => x * x);
  },
  exp: (s) => {
    commit(s);
    unary(s, 'eˣ', Math.exp);
  },
  ln: (s) => {
    commit(s);
    unary(s, 'LN', (x) => (x <= 0 ? fail(0) : Math.log(x)));
  },
  frac: (s) => {
    commit(s);
    unary(s, 'FRAC', (x) => x - Math.trunc(x));
  },
  intg: (s) => {
    commit(s);
    unary(s, 'INTG', Math.trunc);
  },
  fact: (s) => {
    commit(s);
    unary(s, 'n!', factorial);
  },
  rnd: (s) => {
    commit(s);
    unary(s, 'RND', (x) => roundToDisplay(s, x));
  },
  pct: (s) => {
    commit(s);
    const [x, y] = s.stack;
    s.lastX = x;
    s.stack[0] = num((y * x) / 100);
    s.lift = true;
    log(s, '%');
  },
  delta: (s) => {
    commit(s);
    const [x, y] = s.stack;
    if (y === 0) fail(0);
    s.lastX = x;
    s.stack[0] = num(((x - y) / y) * 100);
    s.lift = true;
    log(s, 'Δ%');
  },
  pctT: (s) => {
    commit(s);
    const [x, y] = s.stack;
    if (y === 0) fail(0);
    s.lastX = x;
    s.stack[0] = num((x / y) * 100);
    s.lift = true;
    log(s, '%T');
  },
  '12x': (s) => {
    commit(s);
    const v = s.stack[0] * 12;
    s.n = v;
    replaceX(s, v);
    log(s, '12×');
  },
  '12div': (s) => {
    commit(s);
    const v = s.stack[0] / 12;
    s.i = v;
    replaceX(s, v);
    log(s, '12÷');
  },
  beg: (s) => {
    commit(s);
    s.begin = true;
  },
  end: (s) => {
    commit(s);
    s.begin = false;
  },
  amort: (s) => {
    commit(s);
    amortize(s);
  },
  int: (s) => {
    commit(s);
    simpleInterest(s);
  },
  npv: (s) => {
    commit(s);
    try {
      pushResult(s, npv(s.i, { cf: s.cf, nj: s.nj }));
    } catch (e) {
      if (e instanceof FinanceError) fail(e.code);
      throw e;
    }
    log(s, 'NPV');
  },
  irr: (s) => {
    commit(s);
    try {
      const v = irr({ cf: s.cf, nj: s.nj });
      s.i = v;
      pushResult(s, v);
    } catch (e) {
      if (e instanceof FinanceError) fail(e.code);
      throw e;
    }
    log(s, 'IRR');
  },
  cf0: (s) => {
    commit(s);
    cashFlowKey(s, 'cf0');
  },
  cfj: (s) => {
    commit(s);
    cashFlowKey(s, 'cfj');
  },
  nj: (s) => {
    commit(s);
    cashFlowKey(s, 'nj');
  },
  sl: (s) => {
    commit(s);
    depreciationKey(s, 'SL');
  },
  soyd: (s) => {
    commit(s);
    depreciationKey(s, 'SOYD');
  },
  db: (s) => {
    commit(s);
    depreciationKey(s, 'DB');
  },
  price: (s) => {
    commit(s);
    bondKey(s, 'price');
  },
  ytm: (s) => {
    commit(s);
    bondKey(s, 'ytm');
  },
  date: (s) => {
    commit(s);
    dateKey(s);
  },
  dys: (s) => {
    commit(s);
    daysKey(s);
  },
  dmy: (s) => {
    commit(s);
    s.dmy = true;
  },
  mdy: (s) => {
    commit(s);
    s.dmy = false;
  },
  sigma: (s) => {
    commit(s);
    addStats(s, 1);
    log(s, 'Σ+');
  },
  sigmaMinus: (s) => {
    commit(s);
    addStats(s, -1);
    log(s, 'Σ−');
  },
  mean: (s) => {
    commit(s);
    const n = statsN(s);
    const [x, y] = s.stack;
    s.stack = [num((s.regs[2] ?? 0) / n), num((s.regs[4] ?? 0) / n), x, y];
    s.lift = true;
    log(s, 'x̄');
  },
  stdev: (s) => {
    commit(s);
    const n = statsN(s);
    if (n < 2) fail(2);
    const [x, y] = s.stack;
    const sd = (sum: number, sumSq: number) =>
      Math.sqrt(Math.max(0, (sumSq - (sum * sum) / n) / (n - 1)));
    s.stack = [num(sd(s.regs[2] ?? 0, s.regs[3] ?? 0)), num(sd(s.regs[4] ?? 0, s.regs[5] ?? 0)), x, y];
    s.lift = true;
    log(s, 's');
  },
  xbarw: (s) => {
    commit(s);
    statsN(s);
    const sumY = s.regs[4] ?? 0;
    if (sumY === 0) fail(0);
    replaceX(s, (s.regs[6] ?? 0) / sumY);
    log(s, 'x̄w');
  },
  estY: (s) => {
    commit(s);
    const { a, b, r } = regression(s);
    const x = s.stack[0];
    s.lastX = x;
    s.stack = [num(a + b * x), num(r), s.stack[1], s.stack[2]];
    s.lift = true;
    log(s, 'ŷ,r');
  },
  estX: (s) => {
    commit(s);
    const { a, b, r } = regression(s);
    if (b === 0) fail(2);
    const y = s.stack[0];
    s.lastX = y;
    s.stack = [num((y - a) / b), num(r), s.stack[1], s.stack[2]];
    s.lift = true;
    log(s, 'x̂,r');
  },
  sci: (s) => {
    commit(s);
    s.mode = { kind: 'SCI', digits: s.mode.digits };
  },
  clearStats: (s) => {
    commit(s);
    for (let k = 1; k <= 6; k++) s.regs[k] = 0;
    s.stack = [0, 0, 0, 0];
    s.lift = false;
  },
  clearFin: (s) => {
    commit(s);
    s.n = s.i = s.pv = s.pmt = s.fv = 0;
    s.cf = [0];
    s.nj = [1];
    s.amortized = 0;
  },
  clearReg: (s) => {
    commit(s);
    s.regs.fill(0);
    s.n = s.i = s.pv = s.pmt = s.fv = 0;
    s.cf = [0];
    s.nj = [1];
    s.amortized = 0;
  },
  clearPrefix: (s) => {
    commit(s);
  },
  rpn: () => {
    // The 12C is always in RPN here; the key exists for completeness.
  },
  sto: (s) => {
    commit(s);
    s.pending = { kind: 'STO', op: null, dot: false };
  },
  rcl: (s) => {
    commit(s);
    s.pending = { kind: 'RCL', op: null, dot: false };
  },
  on: () => {},
  unsupported: (s) => {
    s.notice = 'Programming keys are not part of this version.';
  },
};

for (const key of TVM_KEYS) {
  // Registered in press() because they need the previous-key flag.
  ACTIONS[key] = () => {};
}

function digit(s: CalcState, d: string): void {
  if (!s.entry) {
    if (s.lift) liftStack(s);
    s.entry = { neg: false, mant: '', exp: null, expNeg: false };
  }
  const e = s.entry;
  if (e.exp !== null) {
    e.exp = (e.exp + d).slice(-2);
    return;
  }
  if (e.mant.replace('.', '').length >= 10) return;
  e.mant = e.mant === '0' ? d : e.mant + d;
}

function setFix(s: CalcState, digits: number): void {
  commit(s);
  s.mode = { kind: 'FIX', digits };
}

/** Deletes the last typed digit, or clears X when no number is being typed. */
export function backspace(state: CalcState): CalcState {
  if (state.error !== null) return press(state, 'clx');
  const s = structuredClone(state);
  s.shift = null;
  s.notice = null;
  s.dateShown = null;
  if (!s.entry) return press(state, 'clx');
  const e = s.entry;
  if (e.exp !== null) {
    if (e.exp === '') e.exp = null;
    else e.exp = e.exp.slice(0, -1);
  } else if (e.mant.length <= 1) {
    s.entry = { neg: false, mant: '', exp: null, expNeg: false };
  } else {
    e.mant = e.mant.slice(0, -1);
  }
  return s;
}

// ---- Entry point ----------------------------------------------------------

export function press(state: CalcState, keyId: string): CalcState {
  if (state.error !== null) {
    // The first key after an error only clears it, like the real calculator.
    return { ...state, error: null, shift: null, pending: null, notice: null, dateShown: null };
  }

  const s = structuredClone(state);
  const wasTvm = s.lastTvm;
  s.lastTvm = false;
  s.notice = null;

  try {
    if (keyId === 'f' || keyId === 'g') {
      s.shift = s.shift === keyId ? null : keyId;
      s.lastTvm = wasTvm;
      return s;
    }

    const def = KEY_BY_ID.get(keyId);
    if (!def) return s;
    const shift = s.shift;
    s.shift = null;
    s.dateShown = null;

    if (s.pending) {
      handlePending(s, s.pending, keyId);
      return s;
    }

    const action = shift === 'f' ? def.f?.action : shift === 'g' ? def.g?.action : def.main;
    if (!action) return s;

    if (action.startsWith('digit:')) {
      digit(s, action.slice(6));
    } else if (action.startsWith('fix:')) {
      setFix(s, Number(action.slice(4)));
    } else if ((TVM_KEYS as readonly string[]).includes(action)) {
      tvmKey(s, action as TvmKey, wasTvm);
    } else {
      ACTIONS[action]?.(s);
    }
    return s;
  } catch (e) {
    if (e instanceof CalcError) {
      s.error = e.code;
      s.entry = null;
      s.pending = null;
      s.shift = null;
      return s;
    }
    throw e;
  }
}

/** Replays a list of key ids; handy in tests. */
export function pressAll(state: CalcState, keys: string[]): CalcState {
  return keys.reduce(press, state);
}

/** Keys that type a number, e.g. keysFor('1,250.5') -> ['1','2','5','0','dot','5']. */
export function keysFor(value: string): string[] {
  const out: string[] = [];
  for (const ch of value) {
    if (ch === '.') out.push('dot');
    else if (ch === '-') out.push('chs');
    else if (/\d/.test(ch)) out.push(ch);
  }
  // CHS applies to the number typed so far, so it goes last.
  return value.startsWith('-') ? [...out.filter((k) => k !== 'chs'), 'chs'] : out;
}
