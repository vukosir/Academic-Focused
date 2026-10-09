import { config } from '../config';
import { useAppState } from '../state/AppState';
import type { Place } from '../types';
import { LocateIcon, SearchIcon, Spinner } from './Icons';

interface Props {
  onLocate: () => void;
  locating: boolean;
  onFocusSearch: () => void;
  onExample: (query: string) => void;
  onSelect: (place: Place) => void;
}

/** One example for each kind of input the search understands. */
const EXAMPLES = [
  { query: 'Cape Town', hint: 'a city' },
  { query: '90210', hint: 'a postal code' },
  { query: '35.68, 139.69', hint: 'coordinates' },
];

/** First-run screen, shown until a place has been chosen. */
export function Onboarding({ onLocate, locating, onFocusSearch, onExample, onSelect }: Props) {
  const { history } = useAppState();

  return (
    <section className="onboarding" aria-labelledby="onboarding-title">
      <h1 id="onboarding-title" className="onboarding__title">
        Where should we look at the sky?
      </h1>
      <p className="onboarding__lead">
        Share your location or search for a place to see what it is doing now, hour by hour, and over the next seven days.
      </p>

      <div className="onboarding__actions">
        <button type="button" className="button button--primary" onClick={onLocate} disabled={locating}>
          {locating ? <Spinner size={18} /> : <LocateIcon size={18} />}
          {locating ? 'Finding your location' : 'Use my location'}
        </button>
        <button type="button" className="button" onClick={onFocusSearch}>
          <SearchIcon size={18} />
          Search for a place
        </button>
        <button type="button" className="button" onClick={() => onSelect(config.defaultPlace)}>
          Show {config.defaultPlace.name}
        </button>
      </div>

      <div className="onboarding__examples">
        <p id="examples-label">You can search by</p>
        <ul aria-labelledby="examples-label">
          {EXAMPLES.map((example) => (
            <li key={example.query}>
              <button type="button" className="example" onClick={() => onExample(example.query)}>
                <span className="example__hint">{example.hint}</span>
                <span className="example__query">{example.query}</span>
              </button>
            </li>
          ))}
        </ul>
      </div>

      {history.length > 0 ? (
        <div className="onboarding__examples">
          <p id="recent-label">Recently viewed</p>
          <ul aria-labelledby="recent-label">
            {history.map((place) => (
              <li key={place.id}>
                <button type="button" className="example" onClick={() => onSelect(place)}>
                  <span className="example__query">{place.name}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </section>
  );
}
