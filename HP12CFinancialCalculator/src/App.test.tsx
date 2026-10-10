import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import App from './App';
import { initialState } from './engine/calc';
import { loadState, saveState } from './storage';

const display = () => screen.getByTestId('display').textContent;

async function click(user: ReturnType<typeof userEvent.setup>, ...names: string[]) {
  for (const name of names) {
    await user.click(screen.getByRole('button', { name: new RegExp(`^${name}(,|$)`) }));
  }
}

describe('App', () => {
  it('starts at zero', () => {
    render(<App />);
    expect(display()).toBe('0.00');
  });

  it('adds with the on-screen keys', async () => {
    const user = userEvent.setup();
    render(<App />);
    await click(user, '5', 'ENTER', '3', '\\+');
    expect(display()).toBe('8.00');
    expect(screen.getByRole('tabpanel')).toHaveTextContent('8.00');
  });

  it('shows the f shift state and uses the gold function', async () => {
    const user = userEvent.setup();
    render(<App />);
    await click(user, 'f');
    expect(screen.getByRole('button', { name: /^f$/ })).toHaveAttribute('aria-pressed', 'true');
    await click(user, '4');
    expect(display()).toBe('0.0000');
  });

  it('solves a loan payment from the keyboard', () => {
    render(<App />);
    for (const key of '360n0.625i200000vp') fireEvent.keyDown(window, { key });
    expect(display()).toBe('-1,398.43');
  });

  it('shows a readable error and clears it with the next key', async () => {
    const user = userEvent.setup();
    render(<App />);
    await click(user, '5', 'ENTER', '0', '÷');
    expect(display()).toBe('Error 0');
    // The first key only clears the error, as on the real calculator.
    await click(user, '1');
    expect(display()).toBe('0.00');
    await click(user, '1');
    expect(display()).toBe('1');
  });

  it('keeps its state across a reload', async () => {
    const user = userEvent.setup();
    const first = render(<App />);
    await click(user, '4', '2', 'ENTER');
    first.unmount();
    render(<App />);
    expect(display()).toBe('42.00');
  });

  it('switches panels', async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.click(screen.getByRole('tab', { name: 'Registers' }));
    expect(screen.getByText('Time value of money (END)')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Guide' }));
    expect(screen.getByText('RPN in one minute')).toBeInTheDocument();
  });
});

describe('storage', () => {
  it('falls back to a fresh calculator for corrupted data', () => {
    window.localStorage.setItem('hp12c:state', '{"stack":[1,2]}');
    expect(loadState()).toEqual(initialState());
    window.localStorage.setItem('hp12c:state', 'not json');
    expect(loadState()).toEqual(initialState());
  });

  it('round trips a saved state', () => {
    const s = { ...initialState(), n: 360, regs: initialState().regs.map((_, k) => k) };
    saveState(s);
    const back = loadState();
    expect(back.n).toBe(360);
    expect(back.regs[7]).toBe(7);
  });
});
