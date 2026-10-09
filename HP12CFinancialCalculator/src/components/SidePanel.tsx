import { useEffect, useRef, useState } from 'react';
import type { CalcState } from '../engine/calc';
import { formatNumber } from '../engine/format';

type Tab = 'tape' | 'registers' | 'guide';

const TABS: { id: Tab; label: string }[] = [
  { id: 'tape', label: 'Tape' },
  { id: 'registers', label: 'Registers' },
  { id: 'guide', label: 'Guide' },
];

export function SidePanel({ state, onClearTape }: { state: CalcState; onClearTape: () => void }) {
  const [tab, setTab] = useState<Tab>('tape');
  return (
    <section className="side" aria-label="Tape, registers and guide">
      <div className="tabs" role="tablist" aria-label="Panels">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            id={`tab-${t.id}`}
            aria-selected={tab === t.id}
            aria-controls={`panel-${t.id}`}
            className={tab === t.id ? 'is-active' : ''}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div role="tabpanel" id={`panel-${tab}`} aria-labelledby={`tab-${tab}`} className="tabpanel">
        {tab === 'tape' && <Tape state={state} onClear={onClearTape} />}
        {tab === 'registers' && <Registers state={state} />}
        {tab === 'guide' && <Guide />}
      </div>
    </section>
  );
}

function Tape({ state, onClear }: { state: CalcState; onClear: () => void }) {
  const end = useRef<HTMLDivElement>(null);
  useEffect(() => {
    end.current?.scrollIntoView?.({ block: 'nearest' });
  }, [state.tape.length]);
  return (
    <>
      <div className="panel-head">
        <p className="muted">A printed record of everything you key in and calculate.</p>
        <button type="button" className="link" onClick={onClear} disabled={state.tape.length === 0}>
          Clear tape
        </button>
      </div>
      {state.tape.length === 0 ? (
        <p className="empty">Nothing yet. Try 5 ENTER 3 +.</p>
      ) : (
        <ol className="tape" aria-label="Calculation tape">
          {state.tape.map((line, k) => (
            <li key={k}>
              <span className="tape-label">{line.label}</span>
              <span className="tape-value">{line.value}</span>
            </li>
          ))}
        </ol>
      )}
      <div ref={end} />
    </>
  );
}

function Registers({ state }: { state: CalcState }) {
  const fmt = (v: number) => formatNumber(v, state.mode);
  const tvm: [string, number][] = [
    ['n', state.n],
    ['i', state.i],
    ['PV', state.pv],
    ['PMT', state.pmt],
    ['FV', state.fv],
  ];
  const regs = state.regs.map((v, k) => [k < 10 ? `R${k}` : `R.${k - 10}`, v] as const);
  const stack: [string, number][] = [
    ['T', state.stack[3]],
    ['Z', state.stack[2]],
    ['Y', state.stack[1]],
    ['X', state.stack[0]],
    ['LSTx', state.lastX],
  ];
  return (
    <div className="regs">
      <h3>Stack</h3>
      <dl className="reg-grid">
        {stack.map(([k, v]) => (
          <Reg key={k} name={k} value={fmt(v)} />
        ))}
      </dl>
      <h3>Time value of money {state.begin ? '(BEGIN)' : '(END)'}</h3>
      <dl className="reg-grid">
        {tvm.map(([k, v]) => (
          <Reg key={k} name={k} value={fmt(v)} />
        ))}
      </dl>
      <h3>Cash flows</h3>
      <dl className="reg-grid">
        {state.cf.map((v, k) => (
          <Reg
            key={k}
            name={k === 0 ? 'CFo' : `CF${k}`}
            value={`${fmt(v)}${k > 0 && (state.nj[k] ?? 1) > 1 ? ` × ${state.nj[k]}` : ''}`}
          />
        ))}
      </dl>
      <h3>Storage</h3>
      <dl className="reg-grid">
        {regs.map(([k, v]) => (
          <Reg key={k} name={k} value={fmt(v)} />
        ))}
      </dl>
      <p className="muted">Statistics use R1 to R6: n, Σx, Σx², Σy, Σy², Σxy.</p>
    </div>
  );
}

function Reg({ name, value }: { name: string; value: string }) {
  return (
    <div className="reg">
      <dt>{name}</dt>
      <dd>{value}</dd>
    </div>
  );
}

const EXAMPLES: { title: string; steps: string; result: string }[] = [
  {
    title: 'Monthly payment on a 30-year loan',
    steps: '30 g n   7.5 g i   200000 PV   PMT',
    result: '−1,398.43',
  },
  {
    title: 'What does 100 become in 10 years at 5 %?',
    steps: '10 n   5 i   100 CHS PV   FV',
    result: '162.89',
  },
  {
    title: 'Rate of return on cash flows',
    steps: '100 CHS g CFo   60 g CFj   60 g CFj   f IRR',
    result: '13.07',
  },
  {
    title: 'Days between two dates',
    steps: '1.012024 ENTER 3.012024 g ΔDYS',
    result: '60.00',
  },
  {
    title: 'Amortize the first payment',
    steps: 'after PMT above: 1 f AMORT   (x⇄y shows principal)',
    result: '−1,250.00',
  },
];

const KEY_HINTS: [string, string][] = [
  ['0-9 .', 'digits'],
  ['+ - * /', 'arithmetic'],
  ['Enter', 'ENTER'],
  ['n i v p t', 'n, i, PV, PMT, FV'],
  ['c e', 'CHS, EEX'],
  ['f g', 'shift keys'],
  ['s r', 'STO, RCL'],
  ['w d', 'x⇄y, R↓'],
  ['y %', 'yˣ, %'],
  ['q', 'Σ+'],
];

function Guide() {
  return (
    <div className="guide">
      <h3>How to read the keys</h3>
      <p>
        Each key has up to three jobs. The white label is the key itself, the <b className="gold">gold</b> label above it is
        used after <b className="gold">f</b>, and the <b className="blue">blue</b> label on its lower edge is used after{' '}
        <b className="blue">g</b>. Press the shift key first, then the key.
      </p>
      <h3>RPN in one minute</h3>
      <p>
        Type a number, press <b>ENTER</b> to push it up, type the next, then press the operator. To work out 5 + 3, press 5
        ENTER 3 +. The four-level stack (X, Y, Z, T) is shown on the Registers tab.
      </p>
      <p>
        <b className="gold">f Σ</b>, <b className="gold">f FIN</b> and <b className="gold">f REG</b> are the CLEAR keys: statistics, the five money registers, and all stored values.
      </p>
      <h3>Money keys</h3>
      <ul>
        <li>
          After typing a value, <b>n i PV PMT FV</b> store it. Pressing one of them straight after another one
          <i> calculates</i> that value instead.
        </li>
        <li>
          <b>g 7</b> (BEG) and <b>g 8</b> (END) set when payments fall. <b>g n</b> and <b>g i</b> turn years and annual
          rates into months.
        </li>
        <li>
          Enter cash flows with <b>g CFo</b> and <b>g CFj</b>, repeat counts with <b>g Nj</b>, then <b>f NPV</b> or{' '}
          <b>f IRR</b>.
        </li>
        <li>
          Dates are typed as MM.DDYYYY (or DD.MMYYYY after <b>g 4</b>). Bonds use PMT for the coupon rate, i for the yield,
          PV for the price, and settlement and maturity dates in Y and X.
        </li>
        <li>
          Depreciation uses PV for cost, FV for salvage, n for life and i for the declining-balance factor, with the year
          in X.
        </li>
      </ul>
      <h3>Worked examples</h3>
      <ul className="examples">
        {EXAMPLES.map((e) => (
          <li key={e.title}>
            <b>{e.title}</b>
            <code>{e.steps}</code>
            <span>
              → <b>{e.result}</b>
            </span>
          </li>
        ))}
      </ul>
      <h3>Keyboard</h3>
      <ul className="keys-note">
        {KEY_HINTS.map(([k, what]) => (
          <li key={k}>
            <kbd>{k}</kbd> {what}
          </li>
        ))}
      </ul>
      <p className="muted">
        Backspace deletes a digit, Escape clears X. Not included: the programming keys (R/S, SST, BST, GTO, P/R, PSE,
        x≤y, x=0) and MEM.
      </p>
    </div>
  );
}


