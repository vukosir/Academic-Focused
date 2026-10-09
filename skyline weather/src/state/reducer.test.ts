import { describe, expect, it } from 'vitest';
import { config } from '../config';
import type { Place } from '../types';
import { loadState, saveState } from './persistence';
import { isFavorite, reducer, type AppState } from './reducer';

const place = (n: number): Place => ({ id: `id-${n}`, name: `Place ${n}`, latitude: n, longitude: n });

const initial: AppState = {
  place: null,
  temperatureUnit: 'celsius',
  system: 'metric',
  theme: 'light',
  themeChosenByUser: false,
  favorites: [],
  history: [],
};

describe('reducer', () => {
  it('selects a place and records it in history, newest first, without duplicates', () => {
    let state = reducer(initial, { type: 'place/selected', place: place(1) });
    state = reducer(state, { type: 'place/selected', place: place(2) });
    state = reducer(state, { type: 'place/selected', place: place(1) });
    expect(state.place?.id).toBe('id-1');
    expect(state.history.map((p) => p.id)).toEqual(['id-1', 'id-2']);
  });

  it('caps the history length', () => {
    let state = initial;
    for (let n = 0; n < config.maxHistory + 4; n += 1) state = reducer(state, { type: 'place/selected', place: place(n) });
    expect(state.history).toHaveLength(config.maxHistory);
  });

  it('toggles favorites and respects the limit', () => {
    let state = reducer(initial, { type: 'favorites/toggled', place: place(1) });
    expect(isFavorite(state, place(1))).toBe(true);
    state = reducer(state, { type: 'favorites/toggled', place: place(1) });
    expect(isFavorite(state, place(1))).toBe(false);

    for (let n = 0; n < config.maxFavorites + 3; n += 1) state = reducer(state, { type: 'favorites/toggled', place: place(n) });
    expect(state.favorites).toHaveLength(config.maxFavorites);
  });

  it('follows the system theme only until the user chooses one', () => {
    let state = reducer(initial, { type: 'theme/system-changed', theme: 'dark' });
    expect(state.theme).toBe('dark');
    state = reducer(state, { type: 'theme/set', theme: 'light' });
    state = reducer(state, { type: 'theme/system-changed', theme: 'dark' });
    expect(state.theme).toBe('light');
  });

  it('changes units independently', () => {
    const state = reducer(reducer(initial, { type: 'units/temperature', unit: 'fahrenheit' }), { type: 'units/system', system: 'imperial' });
    expect(state).toMatchObject({ temperatureUnit: 'fahrenheit', system: 'imperial' });
  });
});

describe('persistence', () => {
  it('round-trips state through localStorage', () => {
    const state: AppState = { ...initial, place: place(1), favorites: [place(1), place(2)], temperatureUnit: 'fahrenheit', theme: 'dark', themeChosenByUser: true };
    saveState(state);
    expect(loadState()).toMatchObject({ place: place(1), favorites: [place(1), place(2)], temperatureUnit: 'fahrenheit', theme: 'dark', themeChosenByUser: true });
  });

  it('falls back to defaults when stored values are corrupted', () => {
    window.localStorage.setItem('skyline:place', '{"name": 42}');
    window.localStorage.setItem('skyline:favorites', 'not json at all');
    window.localStorage.setItem('skyline:temperatureUnit', '"kelvin"');
    const state = loadState();
    expect(state.place).toBeNull();
    expect(state.favorites).toEqual([]);
    expect(['celsius', 'fahrenheit']).toContain(state.temperatureUnit);
  });
});
