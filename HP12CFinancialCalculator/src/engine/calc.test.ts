import { describe, expect, it } from 'vitest';
import { entryValue } from './format';
import { displayText, initialState, keysFor, press, pressAll, backspace, type CalcState } from './calc';

/** Types a number and presses ENTER. */
const enter = (v: string) => [...keysFor(v), 'enter'];
const type = (v: string) => keysFor(v);
const f = (key: string) => ['f', key];
const g = (key: string) => ['g', key];

type Steps = string | Steps[];
function run(...steps: Steps[]): CalcState {
  const keys: string[] = [];
  const walk = (s: Steps): void => (typeof s === 'string' ? void keys.push(s) : s.forEach(walk));
  steps.forEach(walk);
  return pressAll(initialState(), keys);
}

const x = (s: CalcState) => s.stack[0];
/** The stack as the display sees it, including a number still being typed. */
const live = (s: CalcState) => (s.entry ? [entryValue(s.entry), ...s.stack.slice(1)] : s.stack);

describe('RPN stack', () => {
  it('adds with ENTER', () => {
    const s = run(enter('5'), type('3'), 'add');
    expect(x(s)).toBe(8);
    expect(displayText(s)).toBe('8.00');
  });

  it('does not lift the stack after ENTER, so the next number overwrites X', () => {
    const s = run(enter('5'), type('7'));
    expect(live(s).slice(0, 2)).toEqual([7, 5]);
  });

  it('lifts the stack after an operation result', () => {
    const s = run(enter('2'), type('3'), 'add', type('4'));
    expect(live(s).slice(0, 3)).toEqual([4, 5, 0]);
  });

  it('keeps T when the stack drops', () => {
    const s = run(enter('1'), enter('2'), enter('3'), type('4'), 'add', 'add', 'add');
    expect(x(s)).toBe(10);
  });

  it('rolls down and swaps', () => {
    const s = run(enter('1'), enter('2'), enter('3'), type('4'));
    expect(pressAll(s, ['rdown']).stack).toEqual([3, 2, 1, 4]);
    expect(pressAll(s, ['xy']).stack.slice(0, 2)).toEqual([3, 4]);
  });

  it('recalls the last X', () => {
    const s = run(enter('6'), type('4'), 'div', g('enter'));
    expect(s.stack.slice(0, 2)).toEqual([4, 1.5]);
  });

  it('reports divide by zero as Error 0 and clears it with the next key', () => {
    const s = run(enter('5'), type('0'), 'div');
    expect(displayText(s)).toBe('Error 0');
    const cleared = press(s, '1');
    expect(cleared.error).toBeNull();
    expect(displayText(cleared)).toBe('0.00');
  });

  it('CHS toggles the number being typed and the number in X', () => {
    expect(displayText(run(type('12'), 'chs'))).toBe('-12');
    expect(x(run(enter('9'), 'chs'))).toBe(-9);
  });

  it('formats thousands and decimals while typing', () => {
    expect(displayText(run(type('1234567.5')))).toBe('1,234,567.5');
  });

  it('supports EEX', () => {
    const s = run('eex', '3', 'enter');
    expect(x(s)).toBe(1000);
    expect(x(run('2', 'eex', '3', 'chs', 'enter'))).toBeCloseTo(0.002, 10);
  });

  it('backspace deletes a digit, then clears X', () => {
    const typed = run(type('123'));
    expect(displayText(backspace(typed))).toBe('12');
    expect(x(backspace(run(enter('5'))))).toBe(0);
  });
});

describe('percentages and math', () => {
  it('% keeps the base in Y', () => {
    const s = run(enter('200'), type('15'), 'pct');
    expect(x(s)).toBe(30);
    expect(s.stack[1]).toBe(200);
  });

  it('Δ% and %T', () => {
    expect(x(run(enter('80'), type('100'), 'delta'))).toBeCloseTo(25, 10);
    expect(x(run(enter('200'), type('50'), 'pctT'))).toBeCloseTo(25, 10);
  });

  it('powers, roots, logs', () => {
    expect(x(run(enter('2'), type('10'), 'pow'))).toBe(1024);
    expect(x(run(type('16'), g('pow')))).toBe(4);
    expect(x(run(type('1'), g('inv'), g('pctT')))).toBeCloseTo(1, 10);
    expect(x(run(type('4'), 'inv'))).toBe(0.25);
    expect(x(run(type('5'), g('3')))).toBe(120);
  });

  it('rejects impossible operations', () => {
    expect(run(type('1'), 'chs', g('pow')).error).toBe(0);
    expect(run(type('0'), 'inv').error).toBe(0);
    expect(run(type('0'), g('pctT')).error).toBe(0);
  });

  it('INTG, FRAC and RND', () => {
    expect(x(run(type('3.75'), g('pct')))).toBe(3);
    expect(x(run(type('3.75'), g('delta')))).toBeCloseTo(0.75, 10);
    expect(x(run(type('2.34567'), f('pmt')))).toBe(2.35);
  });
});

describe('display modes', () => {
  it('FIX n and SCI', () => {
    const s = run(enter('1234.56789'), f('4'));
    expect(displayText(s)).toBe('1,234.5679');
    expect(displayText(run(type('1234.5'), 'enter', f('dot')))).toBe('1.23  03');
  });

  it('falls back to scientific for huge values', () => {
    expect(displayText(run(type('1'), 'eex', '1', '2', 'enter'))).toContain(' 12');
  });
});

describe('registers', () => {
  it('stores, recalls and does register arithmetic', () => {
    let s = run(enter('10'), 'sto', '4');
    s = pressAll(s, [...type('5'), 'sto', 'add', '4', 'clx', 'rcl', '4']);
    expect(x(s)).toBe(15);
  });

  it('uses the dot registers', () => {
    const s = run(type('7'), 'sto', 'dot', '3', 'clx', 'rcl', 'dot', '3');
    expect(x(s)).toBe(7);
    expect(s.regs[13]).toBe(7);
  });

  it('stores into the financial registers', () => {
    const s = run(type('9'), 'sto', 'pv', 'clx', 'rcl', 'pv');
    expect(x(s)).toBe(9);
  });
});

describe('time value of money', () => {
  it('computes a 30 year mortgage payment', () => {
    const s = run(type('360'), 'n', type('0.625'), 'i', type('200000'), 'pv', 'pmt');
    expect(x(s)).toBeCloseTo(-1398.43, 2);
  });

  it('computes future value of a lump sum', () => {
    const s = run(type('10'), 'n', type('5'), 'i', type('100'), 'chs', 'pv', 'fv');
    expect(x(s)).toBeCloseTo(162.89, 2);
  });

  it('computes present value of an annuity, in END and BEGIN mode', () => {
    const base = [type('5'), 'n', type('10'), 'i', type('100'), 'pmt', 'fv'];
    const end = run(base.slice(0, 6), type('0'), 'fv', 'pv');
    expect(x(end)).toBeCloseTo(-379.08, 2);
    const begin = run(g('7'), base.slice(0, 6), type('0'), 'fv', 'pv');
    expect(x(begin)).toBeCloseTo(-416.99, 2);
  });

  it('solves for the interest rate', () => {
    const s = run(type('10'), 'n', type('100'), 'chs', 'pv', type('162.889'), 'fv', 'i');
    expect(x(s)).toBeCloseTo(5, 2);
  });

  it('solves for n and rounds up', () => {
    const s = run(type('5'), 'i', type('100'), 'chs', 'pv', type('200'), 'fv', 'n');
    expect(x(s)).toBe(15);
  });

  it('works at 0 % interest', () => {
    const s = run(type('12'), 'n', type('0'), 'i', type('1200'), 'pv', 'pmt');
    expect(x(s)).toBe(-100);
  });

  it('reports Error 5 when there is no solution', () => {
    const s = run(type('5'), 'n', type('100'), 'pv', type('100'), 'fv', 'i');
    expect(s.error).toBe(5);
  });

  it('12× and 12÷ fill n and i', () => {
    const s = run(type('30'), g('n'));
    expect(s.n).toBe(360);
    expect(run(type('7.5'), g('i')).i).toBeCloseTo(0.625, 10);
  });

  it('amortizes a loan', () => {
    const s = run(
      type('360'), 'n', type('0.625'), 'i', type('200000'), 'pv', 'pmt',
      '1', f('n'),
    );
    // First payment: interest 1,250.00, principal 148.43.
    expect(x(s)).toBeCloseTo(-1250, 2);
    expect(s.stack[1]).toBeCloseTo(-148.43, 2);
    expect(s.pv).toBeCloseTo(199851.57, 2);
  });
});

describe('cash flows', () => {
  const flows = (...cfs: string[]) =>
    cfs.flatMap((c, k) => [...type(c.replace('-', '')), ...(c.startsWith('-') ? ['chs'] : []), ...(k === 0 ? g('pv') : g('pmt'))]);

  it('computes NPV and IRR', () => {
    const s = run(flows('-100', '60', '60'), type('10'), 'i', f('pv'));
    expect(x(s)).toBeCloseTo(4.13, 2);
    expect(x(run(flows('-100', '60', '60'), f('fv')))).toBeCloseTo(13.07, 2);
  });

  it('honours Nj repeat counts', () => {
    // -100 then 30 for 4 periods
    const s = run(flows('-100', '30'), type('4'), g('fv'), f('fv'));
    expect(x(s)).toBeCloseTo(7.71, 2);
  });

  it('IRR needs both signs', () => {
    expect(run(flows('100', '60'), f('fv')).error).toBe(7);
  });
});

describe('depreciation', () => {
  const setup = [type('10000'), 'pv', type('1000'), 'fv', type('5'), 'n'];
  it('straight line', () => {
    const s = run(setup, type('2'), f('pctT'));
    expect(x(s)).toBeCloseTo(1800, 6);
    expect(s.stack[1]).toBeCloseTo(5400, 6);
  });
  it('sum of the years digits', () => {
    const s = run(setup, type('1'), f('delta'));
    expect(x(s)).toBeCloseTo(3000, 6);
  });
  it('declining balance with a 200 % factor', () => {
    const s = run(setup, type('200'), 'i', type('1'), f('pct'));
    expect(x(s)).toBeCloseTo(4000, 6);
  });
});

describe('dates', () => {
  it('counts days between dates (M.DY)', () => {
    const s = run(enter('1.012024'), type('3.012024'), g('eex'));
    expect(x(s)).toBe(60);
    expect(s.stack[1]).toBe(60);
  });

  it('adds days and shows the weekday', () => {
    const s = run(enter('1.012024'), type('45'), g('chs'));
    expect(x(s)).toBeCloseTo(2.152024, 6);
    expect(s.dateShown).toBe(4); // Thursday, 15 Feb 2024
  });

  it('supports D.MY entry', () => {
    const s = run(g('4'), enter('1.012024'), type('1.032024'), g('eex'));
    expect(x(s)).toBe(60);
  });

  it('rejects impossible dates with Error 8', () => {
    expect(run(enter('2.302024'), type('1'), g('eex')).error).toBe(8);
  });
});

describe('bonds', () => {
  const setup = [type('6.5'), 'pmt', enter('4.282024'), type('6.152030')];

  it('prices at par when yield equals coupon on a coupon date', () => {
    const s = run(type('6.5'), 'pmt', type('6.5'), 'i', enter('6.152024'), type('6.152030'), f('pow'));
    expect(x(s)).toBeCloseTo(100, 6);
    expect(s.stack[1]).toBeCloseTo(0, 6);
  });

  it('round trips price and yield', () => {
    const priced = run(setup.slice(0, 2), type('7.25'), 'i', setup.slice(2), f('pow'));
    const price = x(priced);
    expect(price).toBeLessThan(100);
    const back = run(
      setup.slice(0, 2), type(String(price)), 'pv', setup.slice(2), f('inv'),
    );
    expect(x(back)).toBeCloseTo(7.25, 4);
  });
});

describe('statistics', () => {
  const data = [
    [1, 2],
    [2, 4],
    [3, 5],
    [4, 4],
    [5, 5],
  ];
  const load = data.flatMap(([a, b]) => [...type(String(b)), 'enter', ...type(String(a)), 'sigma']);

  it('accumulates n', () => {
    expect(x(run(load))).toBe(5);
  });

  it('means and standard deviations', () => {
    const m = run(load, g('0'));
    expect(m.stack[0]).toBeCloseTo(3, 10);
    expect(m.stack[1]).toBeCloseTo(4, 10);
    const sd = run(load, g('dot'));
    expect(sd.stack[0]).toBeCloseTo(1.5811388, 6);
    expect(sd.stack[1]).toBeCloseTo(1.2247449, 6);
  });

  it('linear regression estimate and r', () => {
    const s = run(load, type('6'), g('1'));
    expect(s.stack[0]).toBeCloseTo(5.8, 6);
    expect(s.stack[1]).toBeCloseTo(0.7745967, 6);
  });

  it('reverses with Σ−', () => {
    const s = run(load, type('5'), 'enter', type('5'), g('sigma'));
    expect(s.regs[1]).toBe(4);
  });

  it('raises Error 2 with no data', () => {
    expect(run(g('0')).error).toBe(2);
  });
});

describe('clearing and shifting', () => {
  it('f twice cancels the shift', () => {
    expect(run('f', 'f').shift).toBeNull();
  });
  it('f CLEAR FIN clears the TVM registers only', () => {
    const s = run(type('5'), 'n', type('3'), 'sto', '1', f('xy'));
    expect(s.n).toBe(0);
    expect(s.regs[1]).toBe(3);
  });
  it('f CLEAR REG clears everything stored', () => {
    const s = run(type('5'), 'n', type('3'), 'sto', '1', f('clx'));
    expect(s.n).toBe(0);
    expect(s.regs[1]).toBe(0);
  });
  it('unsupported programming keys leave a notice', () => {
    expect(run('rs').notice).toMatch(/Programming/);
  });
});

describe('tape', () => {
  it('records typed numbers and results', () => {
    const s = run(enter('5'), type('3'), 'add');
    expect(s.tape.map((l) => `${l.label}${l.value}`)).toEqual(['5', '3', '+8.00']);
  });
});




