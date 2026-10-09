/**
 * Application state and the pure reducer that changes it.
 *
 * This covers what the user has chosen: the selected place, units, theme,
 * favorites and search history. Loading and error states for remote data live
 * next to the requests themselves (see hooks/useResource.ts), because they
 * describe a request rather than a user choice.
 */
import { config } from '../config';
import type { MeasurementSystem, Place, TemperatureUnit, Theme } from '../types';

export interface AppState {
  /** null means nothing selected yet: the onboarding screen is shown. */
  place: Place | null;
  temperatureUnit: TemperatureUnit;
  system: MeasurementSystem;
  theme: Theme;
  /** False while the theme simply follows the operating system. */
  themeChosenByUser: boolean;
  favorites: Place[];
  history: Place[];
}

export type Action =
  | { type: 'place/selected'; place: Place; remember?: boolean }
  | { type: 'place/cleared' }
  | { type: 'favorites/toggled'; place: Place }
  | { type: 'favorites/removed'; id: string }
  | { type: 'history/removed'; id: string }
  | { type: 'history/cleared' }
  | { type: 'units/temperature'; unit: TemperatureUnit }
  | { type: 'units/system'; system: MeasurementSystem }
  | { type: 'theme/set'; theme: Theme }
  | { type: 'theme/system-changed'; theme: Theme };

export function isFavorite(state: Pick<AppState, 'favorites'>, place: Place | null): boolean {
  return place !== null && state.favorites.some((favorite) => favorite.id === place.id);
}

export function reducer(state: AppState, action: Action): AppState {
  switch (action.type) {
    case 'place/selected': {
      // Most recent first, no duplicates, capped.
      const history =
        action.remember === false
          ? state.history
          : [action.place, ...state.history.filter((item) => item.id !== action.place.id)].slice(0, config.maxHistory);
      return { ...state, place: action.place, history };
    }
    case 'place/cleared':
      return { ...state, place: null };
    case 'favorites/toggled': {
      const exists = state.favorites.some((item) => item.id === action.place.id);
      if (exists) return { ...state, favorites: state.favorites.filter((item) => item.id !== action.place.id) };
      if (state.favorites.length >= config.maxFavorites) return state;
      return { ...state, favorites: [...state.favorites, action.place] };
    }
    case 'favorites/removed':
      return { ...state, favorites: state.favorites.filter((item) => item.id !== action.id) };
    case 'history/removed':
      return { ...state, history: state.history.filter((item) => item.id !== action.id) };
    case 'history/cleared':
      return { ...state, history: [] };
    case 'units/temperature':
      return { ...state, temperatureUnit: action.unit };
    case 'units/system':
      return { ...state, system: action.system };
    case 'theme/set':
      return { ...state, theme: action.theme, themeChosenByUser: true };
    case 'theme/system-changed':
      // Only follow the OS while the user has not made their own choice.
      return state.themeChosenByUser ? state : { ...state, theme: action.theme };
    default:
      return state;
  }
}
