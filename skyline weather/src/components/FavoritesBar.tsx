import { placeLabel } from '../lib/place';
import { useAppDispatch, useAppState } from '../state/AppState';
import type { Place } from '../types';
import { CloseIcon, StarFilledIcon } from './Icons';

/** Saved places as a row of buttons, for switching with one click. */
export function FavoritesBar({ onSelect }: { onSelect: (place: Place) => void }) {
  const { favorites, place } = useAppState();
  const dispatch = useAppDispatch();

  if (favorites.length === 0) return null;

  return (
    <nav className="favorites" aria-label="Saved places">
      <span className="favorites__label" aria-hidden="true">
        <StarFilledIcon size={14} />
        Saved
      </span>
      <ul className="favorites__list">
        {favorites.map((favorite) => {
          const current = favorite.id === place?.id;
          return (
            <li key={favorite.id} className={`chip${current ? ' chip--current' : ''}`}>
              <button
                type="button"
                className="chip__main"
                onClick={() => onSelect(favorite)}
                aria-current={current ? 'true' : undefined}
                title={placeLabel(favorite)}
              >
                {favorite.name}
              </button>
              <button
                type="button"
                className="chip__remove"
                onClick={() => dispatch({ type: 'favorites/removed', id: favorite.id })}
                aria-label={`Remove ${favorite.name} from saved places`}
              >
                <CloseIcon size={13} />
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
