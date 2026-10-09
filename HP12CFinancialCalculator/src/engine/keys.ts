/**
 * The physical keyboard. Every key has a main action, plus an optional action
 * when preceded by the gold f key and by the blue g key. Action ids are
 * handled in calc.ts.
 */

export interface KeyDef {
  id: string;
  /** Text on the key face. */
  label: string;
  main: string;
  /** Gold label above the key, and the action it runs. */
  f?: { label: string; action: string };
  /** Blue label on the lower edge of the key, and the action it runs. */
  g?: { label: string; action: string };
  kind: 'digit' | 'op' | 'fin' | 'enter' | 'f' | 'g' | 'plain';
}

const k = (
  id: string,
  label: string,
  main: string,
  kind: KeyDef['kind'],
  f?: KeyDef['f'],
  g?: KeyDef['g'],
): KeyDef => {
  const def: KeyDef = { id, label, main, kind };
  if (f) def.f = f;
  if (g) def.g = g;
  return def;
};

const fixed = (n: number) => ({ label: '', action: `fix:${n}` });

/** Row-major, 4 rows of 10. ENTER spans two rows, so the bottom row has 9 cells. */
export const KEYS: KeyDef[][] = [
  [
    k('n', 'n', 'n', 'fin', { label: 'AMORT', action: 'amort' }, { label: '12×', action: '12x' }),
    k('i', 'i', 'i', 'fin', { label: 'INT', action: 'int' }, { label: '12÷', action: '12div' }),
    k('pv', 'PV', 'pv', 'fin', { label: 'NPV', action: 'npv' }, { label: 'CFo', action: 'cf0' }),
    k('pmt', 'PMT', 'pmt', 'fin', { label: 'RND', action: 'rnd' }, { label: 'CFj', action: 'cfj' }),
    k('fv', 'FV', 'fv', 'fin', { label: 'IRR', action: 'irr' }, { label: 'Nj', action: 'nj' }),
    k('chs', 'CHS', 'chs', 'plain', { label: 'RPN', action: 'rpn' }, { label: 'DATE', action: 'date' }),
    k('7', '7', 'digit:7', 'digit', fixed(7), { label: 'BEG', action: 'beg' }),
    k('8', '8', 'digit:8', 'digit', fixed(8), { label: 'END', action: 'end' }),
    k('9', '9', 'digit:9', 'digit', fixed(9), { label: 'MEM', action: 'unsupported' }),
    k('div', '÷', 'div', 'op'),
  ],
  [
    k('pow', 'yˣ', 'pow', 'plain', { label: 'PRICE', action: 'price' }, { label: '√x', action: 'sqrt' }),
    k('inv', '1/x', 'inv', 'plain', { label: 'YTM', action: 'ytm' }, { label: 'eˣ', action: 'exp' }),
    k('pctT', '%T', 'pctT', 'plain', { label: 'SL', action: 'sl' }, { label: 'LN', action: 'ln' }),
    k('delta', 'Δ%', 'delta', 'plain', { label: 'SOYD', action: 'soyd' }, { label: 'FRAC', action: 'frac' }),
    k('pct', '%', 'pct', 'plain', { label: 'DB', action: 'db' }, { label: 'INTG', action: 'intg' }),
    k('eex', 'EEX', 'eex', 'plain', undefined, { label: 'ΔDYS', action: 'dys' }),
    k('4', '4', 'digit:4', 'digit', fixed(4), { label: 'D.MY', action: 'dmy' }),
    k('5', '5', 'digit:5', 'digit', fixed(5), { label: 'M.DY', action: 'mdy' }),
    k('6', '6', 'digit:6', 'digit', fixed(6), { label: 'x̄w', action: 'xbarw' }),
    k('mul', '×', 'mul', 'op', undefined, { label: 'x²', action: 'sq' }),
  ],
  [
    k('rs', 'R/S', 'unsupported', 'plain', { label: 'P/R', action: 'unsupported' }, { label: 'PSE', action: 'unsupported' }),
    k('sst', 'SST', 'unsupported', 'plain', { label: 'Σ', action: 'clearStats' }, { label: 'BST', action: 'unsupported' }),
    k('rdown', 'R↓', 'rdown', 'plain', { label: 'PRGM', action: 'unsupported' }, { label: 'GTO', action: 'unsupported' }),
    k('xy', 'x⇄y', 'xy', 'plain', { label: 'FIN', action: 'clearFin' }, { label: 'x≤y', action: 'unsupported' }),
    k('clx', 'CLx', 'clx', 'plain', { label: 'REG', action: 'clearReg' }, { label: 'x=0', action: 'unsupported' }),
    k('enter', 'ENTER', 'enter', 'enter', { label: 'PREFIX', action: 'clearPrefix' }, { label: 'LSTx', action: 'lstx' }),
    k('1', '1', 'digit:1', 'digit', fixed(1), { label: 'ŷ,r', action: 'estY' }),
    k('2', '2', 'digit:2', 'digit', fixed(2), { label: 'x̂,r', action: 'estX' }),
    k('3', '3', 'digit:3', 'digit', fixed(3), { label: 'n!', action: 'fact' }),
    k('sub', '−', 'sub', 'op'),
  ],
  [
    k('on', 'ON', 'on', 'plain'),
    k('f', 'f', 'f', 'f'),
    k('g', 'g', 'g', 'g'),
    k('sto', 'STO', 'sto', 'plain'),
    k('rcl', 'RCL', 'rcl', 'plain'),
    // ENTER occupies this column from the row above.
    k('0', '0', 'digit:0', 'digit', fixed(0), { label: 'x̄', action: 'mean' }),
    k('dot', '.', 'dot', 'digit', { label: '', action: 'sci' }, { label: 's', action: 'stdev' }),
    k('sigma', 'Σ+', 'sigma', 'plain', undefined, { label: 'Σ−', action: 'sigmaMinus' }),
    k('add', '+', 'add', 'op', undefined, { label: 'LSTx', action: 'lstx' }),
  ],
];

export const ALL_KEYS: KeyDef[] = KEYS.flat();

export const KEY_BY_ID = new Map(ALL_KEYS.map((key) => [key.id, key]));

/** Physical-keyboard shortcuts, shown in the help panel. */
export const KEYBOARD: Record<string, string> = {
  '0': '0',
  '1': '1',
  '2': '2',
  '3': '3',
  '4': '4',
  '5': '5',
  '6': '6',
  '7': '7',
  '8': '8',
  '9': '9',
  '.': 'dot',
  ',': 'dot',
  '+': 'add',
  '-': 'sub',
  '*': 'mul',
  x: 'mul',
  '/': 'div',
  Enter: 'enter',
  '=': 'enter',
  n: 'n',
  i: 'i',
  v: 'pv',
  p: 'pmt',
  t: 'fv',
  c: 'chs',
  e: 'eex',
  f: 'f',
  g: 'g',
  s: 'sto',
  r: 'rcl',
  w: 'xy',
  d: 'rdown',
  y: 'pow',
  '%': 'pct',
  q: 'sigma',
};

