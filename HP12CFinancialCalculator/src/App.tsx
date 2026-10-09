import { useCallback, useEffect, useReducer, useState } from 'react';
import { Display } from './components/Display';
import { Keypad } from './components/Keypad';
import { SidePanel } from './components/SidePanel';
import { backspace, initialState, press, type CalcState } from './engine/calc';
import { KEYBOARD } from './engine/keys';
import { loadState, loadTheme, saveState, saveTheme, type Theme } from './storage';

type Action = { type: 'key'; id: string } | { type: 'back' } | { type: 'reset' } | { type: 'clearTape' };

function reducer(state: CalcState, action: Action): CalcState {
  switch (action.type) {
    case 'key':
      return press(state, action.id);
    case 'back':
      return backspace(state);
    case 'reset':
      return initialState();
    case 'clearTape':
      return { ...state, tape: [] };
  }
}

function isTyping(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  return target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName);
}

export default function App() {
  const [state, dispatch] = useReducer(reducer, undefined, loadState);
  const [theme, setTheme] = useState<Theme>(loadTheme);
  const [copied, setCopied] = useState(false);

  useEffect(() => saveState(state), [state]);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    saveTheme(theme);
  }, [theme]);

  const onKey = useCallback((id: string) => dispatch({ type: 'key', id }), []);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.ctrlKey || e.metaKey || e.altKey || isTyping(e.target)) return;
      if (e.key === 'Backspace') {
        e.preventDefault();
        dispatch({ type: 'back' });
        return;
      }
      if (e.key === 'Escape') {
        dispatch({ type: 'key', id: 'clx' });
        return;
      }
      const id = KEYBOARD[e.key] ?? KEYBOARD[e.key.toLowerCase()];
      if (!id) return;
      // Let Enter and Space activate a focused button normally.
      if (e.key === 'Enter' && e.target instanceof HTMLButtonElement) return;
      e.preventDefault();
      dispatch({ type: 'key', id });
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(String(state.stack[0]));
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      setCopied(false);
    }
  };

  return (
    <div className="page">
      <header className="top">
        <div>
          <h1>HP 12C</h1>
          <p className="sub">Financial calculator</p>
        </div>
        <div className="top-actions">
          <button type="button" className="chip" onClick={copy} aria-label="Copy the displayed value">
            {copied ? 'Copied' : 'Copy X'}
          </button>
          <button
            type="button"
            className="chip"
            onClick={() => {
              if (window.confirm('Reset the calculator? This clears the stack, registers and tape.')) {
                dispatch({ type: 'reset' });
              }
            }}
          >
            Reset
          </button>
          <button
            type="button"
            className="chip"
            onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
            aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
          >
            {theme === 'dark' ? 'Light' : 'Dark'}
          </button>
        </div>
      </header>

      <main className="layout">
        <section className="calc" aria-label="Calculator">
          <div className="calc-badge" aria-hidden="true">
            <span>HEWLETT·PACKARD</span>
            <span className="model">12C</span>
          </div>
          <Display state={state} />
          <p className="notice" role="status">
            {state.notice ?? (state.error !== null ? 'Press any key to clear the error.' : '')}
          </p>
          <Keypad shift={state.shift} onPress={onKey} />
        </section>
        <SidePanel state={state} onClearTape={() => dispatch({ type: 'clearTape' })} />
      </main>

      <footer className="foot">
        An independent web recreation for learning and everyday finance. Not affiliated with HP. Everything runs in your
        browser and is saved on this device only.
      </footer>
    </div>
  );
}
