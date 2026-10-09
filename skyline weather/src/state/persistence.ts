/**
 * Loading and saving AppState in localStorage. Each slice has its own key and
 * its own guard, so one corrupted value only resets that slice.
 */
import { isPlace } from '../lib/place';
import { readJson, removeKey, writeJson } from '../lib/storage';
import type { MeasurementSystem, Place, TemperatureUnit, Theme } from '../types';
import type { AppState } from './reducer';

const isTheme = (v: unknown): v is Theme => v === 'light' || v === 'dark';
const isTemperatureUnit = (v: unknown): v is TemperatureUnit => v === 'celsius' || v === 'fahrenheit';
const isSystem = (v: unknown): v is MeasurementSystem => v === 'metric' || v === 'imperial';
const isPlaceOrNull = (v: unknown): v is Place | null => v === null || isPlace(v);
const isPlaceList = (v: unknown): v is Place[] => Array.isArray(v) && v.every(isPlace);
const isThemeOrNull = (v: unknown): v is Theme | null => v === null || isTheme(v);

export function systemTheme(): Theme {
  try {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

/** Regions where Fahrenheit and miles are the everyday units. */
function localeDefaults(): { temperatureUnit: TemperatureUnit; system: MeasurementSystem } {
  const region = (typeof navigator !== 'undefined' ? navigator.language : 'en').split('-')[1]?.toUpperCase();
  const imperial = region === 'US' || region === 'LR' || region === 'MM';
  return { temperatureUnit: imperial ? 'fahrenheit' : 'celsius', system: imperial ? 'imperial' : 'metric' };
}

export function loadState(): AppState {
  const defaults = localeDefaults();
  const savedTheme = readJson<Theme | null>('theme', isThemeOrNull, null);
  return {
    place: readJson<Place | null>('place', isPlaceOrNull, null),
    temperatureUnit: readJson('temperatureUnit', isTemperatureUnit, defaults.temperatureUnit),
    system: readJson('system', isSystem, defaults.system),
    theme: savedTheme ?? systemTheme(),
    themeChosenByUser: savedTheme !== null,
    favorites: readJson<Place[]>('favorites', isPlaceList, []),
    history: readJson<Place[]>('history', isPlaceList, []),
  };
}

export function saveState(state: AppState): void {
  writeJson('place', state.place);
  writeJson('temperatureUnit', state.temperatureUnit);
  writeJson('system', state.system);
  writeJson('favorites', state.favorites);
  writeJson('history', state.history);
  // The theme is stored only once the user picks one, so that until then the
  // app keeps following the operating system.
  if (state.themeChosenByUser) writeJson('theme', state.theme);
  else removeKey('theme');
}
