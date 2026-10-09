/**
 * React bindings for the app state: a provider that owns the reducer, keeps
 * localStorage and the <html> theme attribute in sync, and two hooks to read
 * the state and dispatch actions.
 */
import { createContext, useContext, useEffect, useMemo, useReducer, type Dispatch, type ReactNode } from 'react';
import { loadState, saveState } from './persistence';
import { reducer, type Action, type AppState } from './reducer';

const StateContext = createContext<AppState | null>(null);
const DispatchContext = createContext<Dispatch<Action> | null>(null);

export function AppStateProvider({ children, initialState }: { children: ReactNode; initialState?: AppState }) {
  const [state, dispatch] = useReducer(reducer, initialState, (preset) => preset ?? loadState());

  // Persist every change. The writes are tiny, so no batching is needed.
  useEffect(() => {
    saveState(state);
  }, [state]);

  // The theme is applied on <html> so CSS variables switch for the whole page.
  useEffect(() => {
    document.documentElement.dataset.theme = state.theme;
  }, [state.theme]);

  // Follow the operating system until the user chooses a theme themselves.
  useEffect(() => {
    if (typeof window.matchMedia !== 'function') return;
    const query = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (event: MediaQueryListEvent) =>
      dispatch({ type: 'theme/system-changed', theme: event.matches ? 'dark' : 'light' });
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  const stableState = useMemo(() => state, [state]);

  return (
    <StateContext.Provider value={stableState}>
      <DispatchContext.Provider value={dispatch}>{children}</DispatchContext.Provider>
    </StateContext.Provider>
  );
}

export function useAppState(): AppState {
  const state = useContext(StateContext);
  if (!state) throw new Error('useAppState must be used inside AppStateProvider');
  return state;
}

export function useAppDispatch(): Dispatch<Action> {
  const dispatch = useContext(DispatchContext);
  if (!dispatch) throw new Error('useAppDispatch must be used inside AppStateProvider');
  return dispatch;
}
