/**
 * Location search with autocomplete.
 *
 * Follows the ARIA combobox pattern: the text field owns a listbox, arrow
 * keys move a virtual highlight (focus stays in the field), Enter chooses,
 * Escape closes. With an empty field the list shows recent searches instead
 * of suggestions.
 */
import { useEffect, useId, useImperativeHandle, useRef, useState, type KeyboardEvent, type Ref } from 'react';
import { AppError, describeError, isAbortError, toAppError } from '../api/errors';
import { MIN_QUERY_LENGTH, resolveQuery, reverseGeocode } from '../api/geocoding';
import { useLocationSuggestions } from '../hooks/useLocationSuggestions';
import { formatCoordinates, placeContext } from '../lib/place';
import { useAppDispatch, useAppState } from '../state/AppState';
import type { Place } from '../types';
import { CloseIcon, HistoryIcon, PinIcon, SearchIcon, Spinner } from './Icons';

export interface SearchBoxHandle {
  focus: () => void;
  /** Run a search as if the user had typed and submitted `query`. */
  search: (query: string) => void;
}

interface Props {
  onSelect: (place: Place) => void;
  ref?: Ref<SearchBoxHandle>;
}

export function SearchBox({ onSelect, ref }: Props) {
  const { history } = useAppState();
  const dispatch = useAppDispatch();

  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<{ error: AppError; query: string } | null>(null);

  const inputRef = useRef<HTMLInputElement>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const submitController = useRef<AbortController | null>(null);

  const listId = useId();
  const errorId = useId();
  const hintId = useId();

  const term = query.trim();
  const suggestionState = useLocationSuggestions(query);
  const showingHistory = term.length < MIN_QUERY_LENGTH;
  const options: Place[] = showingHistory ? history : suggestionState.suggestions;
  const isCoordinateSuggestion = suggestionState.status === 'ready' && suggestionState.coordinates;

  // The highlight must never point past the end of a list that just changed.
  useEffect(() => {
    setActiveIndex(-1);
  }, [term, showingHistory]);

  // Cancel a pending submit if the component goes away.
  useEffect(() => () => submitController.current?.abort(), []);

  const choose = (place: Place) => {
    setQuery('');
    setOpen(false);
    setSubmitError(null);
    setActiveIndex(-1);
    onSelect(place);
    inputRef.current?.blur();
  };

  /** Resolve free text (or coordinates) to a place and select it. */
  const submit = async (text: string) => {
    const value = text.trim();
    if (value === '') return;
    submitController.current?.abort();
    const controller = new AbortController();
    submitController.current = controller;
    setSubmitting(true);
    setSubmitError(null);
    try {
      choose(await resolveQuery(value, { signal: controller.signal }));
    } catch (error) {
      if (isAbortError(error)) return;
      setSubmitError({ error: toAppError(error), query: value });
      setOpen(false);
    } finally {
      if (submitController.current === controller) setSubmitting(false);
    }
  };

  /** Coordinates typed by hand get a proper place name before being selected. */
  const chooseOption = async (place: Place) => {
    if (!isCoordinateSuggestion) {
      choose(place);
      return;
    }
    setSubmitting(true);
    try {
      choose(await reverseGeocode(place));
    } catch {
      choose(place);
    } finally {
      setSubmitting(false);
    }
  };

  useImperativeHandle(ref, () => ({
    focus: () => inputRef.current?.focus(),
    search: (text: string) => {
      setQuery(text);
      void submit(text);
    },
  }));

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowUp': {
        event.preventDefault();
        if (!open) {
          setOpen(true);
          return;
        }
        if (options.length === 0) return;
        const step = event.key === 'ArrowDown' ? 1 : -1;
        // Moving past either end returns to the text field (index -1).
        setActiveIndex((index) => {
          const next = index + step;
          if (next >= options.length) return -1;
          if (next < -1) return options.length - 1;
          return next;
        });
        break;
      }
      case 'Enter': {
        event.preventDefault();
        const highlighted = open && activeIndex >= 0 ? options[activeIndex] : undefined;
        if (highlighted) {
          void chooseOption(highlighted);
        } else if (!showingHistory && suggestionState.status === 'ready' && suggestionState.suggestions[0]) {
          // Suggestions for exactly this text are already loaded: take the top one.
          void chooseOption(suggestionState.suggestions[0]);
        } else {
          void submit(query);
        }
        break;
      }
      case 'Escape':
        if (open) {
          event.preventDefault();
          setOpen(false);
          setActiveIndex(-1);
        } else if (query !== '') {
          event.preventDefault();
          setQuery('');
          setSubmitError(null);
        }
        break;
      default:
    }
  };

  const clear = () => {
    setQuery('');
    setSubmitError(null);
    setActiveIndex(-1);
    inputRef.current?.focus();
  };

  // What the popup has to say when there are no options to list.
  let message: string | null = null;
  if (!showingHistory) {
    if (suggestionState.status === 'error') message = describeError(suggestionState.error, 'suggestions').title;
    else if (suggestionState.status === 'ready' && options.length === 0)
      message = 'No matching places. Check the spelling or try a larger town nearby.';
    else if (suggestionState.status === 'loading' && options.length === 0) message = 'Searching';
  }

  const popupVisible = open && (options.length > 0 || message !== null);
  const busy = submitting || (!showingHistory && suggestionState.status === 'loading');
  const activeId = popupVisible && activeIndex >= 0 ? `${listId}-option-${activeIndex}` : undefined;

  // Announced to screen readers when the list changes.
  const liveText = !open
    ? ''
    : message
      ? message
      : options.length > 0
        ? `${options.length} ${showingHistory ? 'recent' : 'suggested'} ${options.length === 1 ? 'place' : 'places'}. Use the arrow keys to choose.`
        : '';

  return (
    <div
      className="search"
      ref={rootRef}
      onBlur={(event) => {
        // Close only when focus leaves the whole search component.
        if (!rootRef.current?.contains(event.relatedTarget as Node | null)) setOpen(false);
      }}
    >
      <form
        role="search"
        className="search__field"
        onSubmit={(event) => {
          event.preventDefault();
          void submit(query);
        }}
      >
        <span className="search__icon">{busy ? <Spinner size={18} /> : <SearchIcon size={18} />}</span>
        <label htmlFor={`${listId}-input`} className="visually-hidden">
          Search for a place
        </label>
        <input
          ref={inputRef}
          id={`${listId}-input`}
          className="search__input"
          type="text"
          inputMode="search"
          enterKeyHint="search"
          placeholder="City, postal code or coordinates"
          autoComplete="off"
          autoCorrect="off"
          spellCheck={false}
          role="combobox"
          aria-autocomplete="list"
          aria-expanded={popupVisible}
          aria-controls={listId}
          aria-activedescendant={activeId}
          aria-describedby={submitError ? errorId : hintId}
          aria-invalid={submitError ? true : undefined}
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            setOpen(true);
            setSubmitError(null);
          }}
          onFocus={() => setOpen(true)}
          onClick={() => setOpen(true)}
          onKeyDown={onKeyDown}
        />
        {query !== '' ? (
          <button type="button" className="search__clear" onClick={clear} aria-label="Clear search">
            <CloseIcon size={16} />
          </button>
        ) : null}
        <button type="submit" className="visually-hidden" tabIndex={-1}>
          Search
        </button>
      </form>

      <p id={hintId} className="visually-hidden">
        Type a city, a postal code, or latitude and longitude. Suggestions appear as you type.
      </p>
      <p className="visually-hidden" role="status" aria-live="polite">
        {liveText}
      </p>

      <div className="search__popup" hidden={!popupVisible}>
        {showingHistory && options.length > 0 ? (
          <div className="search__popup-head">
            <span id={`${listId}-label`}>Recent searches</span>
            <button
              type="button"
              className="link-button"
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => dispatch({ type: 'history/cleared' })}
            >
              Clear history
            </button>
          </div>
        ) : null}

        <ul
          id={listId}
          role="listbox"
          className="search__list"
          aria-label={showingHistory ? 'Recent searches' : 'Suggested places'}
        >
          {options.map((place, index) => {
            const context = placeContext(place);
            return (
              <li
                key={place.id}
                id={`${listId}-option-${index}`}
                role="option"
                aria-selected={index === activeIndex}
                className={`search__option${index === activeIndex ? ' is-active' : ''}`}
                // Keep focus in the text field so the popup does not close before the click lands.
                onMouseDown={(event) => event.preventDefault()}
                onMouseEnter={() => setActiveIndex(index)}
                onClick={() => void chooseOption(place)}
              >
                <span className="search__option-icon">{showingHistory ? <HistoryIcon size={16} /> : <PinIcon size={16} />}</span>
                <span className="search__option-text">
                  <span className="search__option-name">
                    {isCoordinateSuggestion ? `Go to ${formatCoordinates(place.latitude, place.longitude)}` : place.name}
                  </span>
                  {context ? <span className="search__option-context">{context}</span> : null}
                </span>
              </li>
            );
          })}
        </ul>

        {message ? <p className="search__message">{message}</p> : null}
      </div>

      {submitError ? (
        <div id={errorId} className="search__error" role="alert">
          <p className="search__error-title">
            {submitError.error.kind === 'not-found'
              ? `No place matches "${submitError.query}"`
              : describeError(submitError.error, 'that place').title}
          </p>
          <p>{describeError(submitError.error, 'that place').message}</p>
          {submitError.error.retryable ? (
            <button type="button" className="link-button" onClick={() => void submit(submitError.query)}>
              Try again
            </button>
          ) : null}
          <button type="button" className="search__error-close" onClick={() => setSubmitError(null)} aria-label="Dismiss this message">
            <CloseIcon size={16} />
          </button>
        </div>
      ) : null}
    </div>
  );
}
