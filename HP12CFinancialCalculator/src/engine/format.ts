/** Turns numbers into the text the calculator display shows. */

export interface DisplayMode {
  kind: 'FIX' | 'SCI';
  digits: number;
}

export interface Entry {
  neg: boolean;
  mant: string;
  /** null while the mantissa is being typed; a string of 0-2 digits after EEX. */
  exp: string | null;
  expNeg: boolean;
}

function withCommas(int: string): string {
  return int.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

function sci(v: number, digits: number): string {
  const [mant = '0', exp = '0'] = v.toExponential(digits).split('e');
  const e = Number(exp);
  return `${mant} ${e < 0 ? '-' : ' '}${String(Math.abs(e)).padStart(2, '0')}`;
}

export function formatNumber(v: number, mode: DisplayMode): string {
  if (!Number.isFinite(v)) return 'Error 0';
  if (mode.kind === 'SCI') return sci(v, mode.digits);
  const rounded = Number(v.toFixed(mode.digits));
  if (Math.abs(rounded) >= 1e10) return sci(v, 6);
  const text = Math.abs(rounded).toFixed(mode.digits);
  const [int = '0', frac] = text.split('.');
  const sign = rounded < 0 || Object.is(rounded, -0) ? '-' : '';
  return `${sign}${withCommas(int)}.${frac ?? ''}`;
}

/** Text for a number that is still being typed. */
export function formatEntry(e: Entry): string {
  const [int = '', frac] = e.mant.split('.');
  const body = `${withCommas(int === '' ? '0' : int)}${frac !== undefined ? `.${frac}` : ''}`;
  const sign = e.neg ? '-' : '';
  if (e.exp === null) return `${sign}${body}`;
  return `${sign}${body} ${e.expNeg ? '-' : ' '}${e.exp.padStart(2, '0')}`;
}

export function entryValue(e: Entry): number {
  const mant = e.mant === '' ? '0' : e.mant;
  const exp = e.exp === null || e.exp === '' ? '0' : e.exp;
  return Number(`${e.neg ? '-' : ''}${mant}e${e.expNeg ? '-' : ''}${exp}`);
}
