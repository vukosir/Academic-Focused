import { displayText, weekdayLabel, type CalcState } from '../engine/calc';

export function Display({ state }: { state: CalcState }) {
  const text = displayText(state);
  const error = state.error !== null;
  return (
    <div className="lcd" role="group" aria-label="Calculator display">
      <div className="lcd-flags" aria-hidden="true">
        <span className={state.shift === 'f' ? 'on' : ''}>f</span>
        <span className={state.shift === 'g' ? 'on' : ''}>g</span>
        <span className={state.begin ? 'on' : ''}>BEGIN</span>
        <span className={state.dmy ? 'on' : ''}>D.MY</span>
        <span className={state.pending ? 'on' : ''}>
          {state.pending ? `${state.pending.kind}${state.pending.op ? ` ${state.pending.op === '*' ? '×' : state.pending.op === '/' ? '÷' : state.pending.op}` : ''}${state.pending.dot ? ' .' : ''}` : 'STO'}
        </span>
        <span className="lcd-spacer" />
        <span className="on">RPN</span>
      </div>
      <output className={`lcd-value${error ? ' is-error' : ''}`} aria-live="polite" data-testid="display">
        {text}
      </output>
      <div className="lcd-foot" aria-hidden="true">
        <span>{weekdayLabel(state)}</span>
        <span>{state.mode.kind === 'SCI' ? `SCI ${state.mode.digits}` : `FIX ${state.mode.digits}`}</span>
      </div>
    </div>
  );
}
