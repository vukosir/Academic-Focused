/**
 * Autocomplete for the search box.
 *
 * The typed text is debounced, then looked up. Each new lookup aborts the
 * previous one, so a slow reply for "Pre" can never overwrite the results for
 * "Pretoria". Coordinates are recognised locally and offered as a suggestion
 * without any request.
 */
import { useEffect, useState } from 'react';
import { AppError, isAbortError, toAppError } from '../api/errors';
import { MIN_QUERY_LENGTH, parseCoordinates, placeFromCoordinates, searchPlaces } from '../api/geocoding';
import { config } from '../config';
import type { Place } from '../types';
import { useDebouncedValue } from './useDebouncedValue';

export type SuggestionState =
  | { status: 'idle'; suggestions: Place[] }
  | { status: 'loading'; suggestions: Place[] }
  | { status: 'ready'; suggestions: Place[]; coordinates: boolean }
  | { status: 'error'; suggestions: Place[]; error: AppError };

const IDLE: SuggestionState = { status: 'idle', suggestions: [] };

export function useLocationSuggestions(query: string): SuggestionState {
  const term = query.trim();
  const debounced = useDebouncedValue(term, config.searchDebounceMs);
  const [state, setState] = useState<SuggestionState>(IDLE);

  useEffect(() => {
    if (debounced.length < MIN_QUERY_LENGTH) {
      setState(IDLE);
      return;
    }

    const coords = parseCoordinates(debounced);
    if (coords) {
      setState({ status: 'ready', suggestions: [placeFromCoordinates(coords)], coordinates: true });
      return;
    }

    const controller = new AbortController();
    // Keep the previous list visible while the next one loads, to avoid flicker.
    setState((previous) => ({ status: 'loading', suggestions: previous.suggestions }));

    searchPlaces(debounced, { signal: controller.signal })
      .then((suggestions) => {
        if (!controller.signal.aborted) setState({ status: 'ready', suggestions, coordinates: false });
      })
      .catch((error: unknown) => {
        if (isAbortError(error) || controller.signal.aborted) return;
        setState({ status: 'error', suggestions: [], error: toAppError(error) });
      });

    return () => controller.abort();
  }, [debounced]);

  // While the user is still typing, the debounced value lags behind. Report
  // that as loading so the list never shows results for outdated text.
  if (term.length < MIN_QUERY_LENGTH) return IDLE;
  if (term !== debounced && state.status !== 'loading') return { status: 'loading', suggestions: state.suggestions };
  return state;
}
