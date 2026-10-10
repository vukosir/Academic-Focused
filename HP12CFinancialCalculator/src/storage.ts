import { initialState, type CalcState } from './engine/calc';

const STATE_KEY = 'hp12c:state';
const THEME_KEY = 'hp12c:theme';

const isNumbers = (v: unknown, length?: number): v is number[] =>
  Array.isArray(v) &&
  (length === undefined || v.length === length) &&
  v.every((n) => typeof n === 'number' && Number.isFinite(n));

/** Reads the saved calculator, falling back to a fresh one if anything is off. */
export function loadState(): CalcState {
  const fresh = initialState();
  try {
    const raw = window.localStorage.getItem(STATE_KEY);
    if (!raw) return fresh;
    const saved = JSON.parse(raw) as Partial<CalcState> | null;
    if (!saved || typeof saved !== 'object') return fresh;
    if (!isNumbers(saved.stack, 4) || !isNumbers(saved.regs, fresh.regs.length)) return fresh;
    if (!isNumbers(saved.cf) || !isNumbers(saved.nj) || saved.cf.length !== saved.nj.length) return fresh;
    if (saved.cf.length < 1 || saved.cf.length > 21) return fresh;
    const nums = ['lastX', 'n', 'i', 'pv', 'pmt', 'fv', 'amortized'] as const;
    if (!nums.every((k) => typeof saved[k] === 'number' && Number.isFinite(saved[k]))) return fresh;
    const mode = saved.mode;
    if (!mode || (mode.kind !== 'FIX' && mode.kind !== 'SCI') || !Number.isInteger(mode.digits)) return fresh;
    if (mode.digits < 0 || mode.digits > 9) return fresh;
    return {
      ...fresh,
      stack: saved.stack as CalcState['stack'],
      lastX: saved.lastX as number,
      lift: saved.lift === true,
      regs: saved.regs,
      n: saved.n as number,
      i: saved.i as number,
      pv: saved.pv as number,
      pmt: saved.pmt as number,
      fv: saved.fv as number,
      begin: saved.begin === true,
      cf: saved.cf,
      nj: saved.nj,
      amortized: saved.amortized as number,
      mode,
      dmy: saved.dmy === true,
      tape: Array.isArray(saved.tape)
        ? saved.tape
            .filter((l) => l && typeof l.label === 'string' && typeof l.value === 'string')
            .slice(-200)
        : [],
    };
  } catch {
    return fresh;
  }
}

export function saveState(state: CalcState): void {
  try {
    window.localStorage.setItem(STATE_KEY, JSON.stringify(state));
  } catch {
    // Storage can be full or blocked. The calculator still works without it.
  }
}

export type Theme = 'light' | 'dark';

export function loadTheme(): Theme {
  try {
    const saved = JSON.parse(window.localStorage.getItem(THEME_KEY) ?? 'null');
    if (saved === 'light' || saved === 'dark') return saved;
  } catch {
    // fall through to the system setting
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function saveTheme(theme: Theme): void {
  try {
    window.localStorage.setItem(THEME_KEY, JSON.stringify(theme));
  } catch {
    // ignore
  }
}
