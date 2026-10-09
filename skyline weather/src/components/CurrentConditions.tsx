/**
 * The top of the page: where, when, how warm, and what the sky is doing.
 */
import { describeError, type AppError } from '../api/errors';
import { formatCoordinates, placeContext } from '../lib/place';
import { formatAge, formatClock, formatLongDateTime, wallNow } from '../lib/time';
import { formatTemperature, shortTemperature } from '../lib/units';
import { describeWeather } from '../lib/weatherCodes';
import { isFavorite } from '../state/reducer';
import { useAppDispatch, useAppState } from '../state/AppState';
import { config } from '../config';
import type { Forecast, Place } from '../types';
import { RefreshIcon, Spinner, StarFilledIcon, StarIcon } from './Icons';
import { SunArc } from './SunArc';
import { WeatherIcon } from './WeatherIcon';
import { WidgetBoundary } from './WidgetBoundary';

interface Props {
  place: Place;
  forecast: Forecast;
  updatedAt: number;
  /** Set when a refresh failed and the data on screen is from earlier. */
  staleError: AppError | null;
  isRefreshing: boolean;
  onRefresh: () => void;
}

export function CurrentConditions({ place, forecast, updatedAt, staleError, isRefreshing, onRefresh }: Props) {
  const state = useAppState();
  const dispatch = useAppDispatch();
  const { current, daily } = forecast;

  const condition = describeWeather(current.weatherCode, current.isDay);
  const temperature = formatTemperature(current.temperature, state.temperatureUnit);
  const feelsLike = formatTemperature(current.apparentTemperature, state.temperatureUnit);
  const today = daily[0];
  const now = wallNow(forecast.utcOffsetSeconds);
  const context = placeContext(place);
  const saved = isFavorite(state, place);
  const favoritesFull = !saved && state.favorites.length >= config.maxFavorites;

  return (
    <section className="hero" aria-labelledby="place-title">
      <div className="hero__main">
        <div className="hero__place">
          <h1 id="place-title" className="hero__name">
            {place.name}
          </h1>
          <button
            type="button"
            className={`icon-button hero__save${saved ? ' is-on' : ''}`}
            onClick={() => dispatch({ type: 'favorites/toggled', place })}
            aria-pressed={saved}
            disabled={favoritesFull}
            aria-label={saved ? `Remove ${place.name} from saved places` : `Save ${place.name}`}
            title={
              favoritesFull
                ? `You can save up to ${config.maxFavorites} places. Remove one to add another.`
                : saved
                  ? 'Saved. Select to remove.'
                  : 'Save this place'
            }
          >
            {saved ? <StarFilledIcon size={18} /> : <StarIcon size={18} />}
          </button>
        </div>
        <p className="hero__meta">
          {context || formatCoordinates(place.latitude, place.longitude, 2)}
          <span className="hero__meta-sep" aria-hidden="true" />
          <time dateTime={now.toISOString().slice(0, 16)}>{formatLongDateTime(now)}</time>
        </p>

        <div className="hero__reading">
          <p className="hero__temp">
            <span className="visually-hidden">Temperature: {temperature.spoken}</span>
            <span aria-hidden="true">
              {temperature.value}
              <span className="hero__temp-unit">{temperature.unit}</span>
            </span>
          </p>
          <div className="hero__condition">
            <WeatherIcon name={condition.icon} size={56} />
            <div>
              <p className="hero__condition-label">{condition.label}</p>
              <p className="hero__feels">
                <span className="visually-hidden">Feels like {feelsLike.spoken}</span>
                <span aria-hidden="true">
                  Feels like {feelsLike.value}
                  {feelsLike.value === '--' ? '' : '°'}
                </span>
              </p>
            </div>
          </div>
        </div>

        {today ? (
          <dl className="hero__range">
            <div>
              <dt>High</dt>
              <dd>{shortTemperature(today.tempMax, state.temperatureUnit)}</dd>
            </div>
            <div>
              <dt>Low</dt>
              <dd>{shortTemperature(today.tempMin, state.temperatureUnit)}</dd>
            </div>
            <div>
              <dt>Rain chance</dt>
              <dd>{today.precipitationProbabilityMax === null ? '--' : `${Math.round(today.precipitationProbabilityMax)}%`}</dd>
            </div>
          </dl>
        ) : null}

        <div className="hero__status">
          <button type="button" className="button button--ghost" onClick={onRefresh} disabled={isRefreshing}>
            {isRefreshing ? <Spinner size={16} /> : <RefreshIcon size={16} />}
            {isRefreshing ? 'Refreshing' : 'Refresh'}
          </button>
          <p className="hero__updated">
            Observed at {formatClock(current.time)} local time, loaded {formatAge(updatedAt)}
          </p>
        </div>

        {staleError ? (
          <p className="hero__stale" role="status">
            <strong>Showing the last forecast we could load.</strong> {describeError(staleError).title}. It will refresh on its own
            once the connection is back.
          </p>
        ) : null}
      </div>

      <div className="hero__arc">
        <WidgetBoundary name="the sun path" resetKey={place.id}>
          <SunArc today={today} tomorrow={daily[1]} now={now} />
        </WidgetBoundary>
      </div>
    </section>
  );
}
