/**
 * App: wires state, data and layout together.
 *
 * Page structure
 *   topbar      brand, search, units, theme
 *   favorites   saved places
 *   notices     offline, location problems
 *   main
 *     hero      current conditions over the sky scene, or onboarding / error
 *     sheet     forecast panels, each inside its own error boundary
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import { isAbortError } from './api/errors';
import { reverseGeocode } from './api/geocoding';
import { AirQualityPanel } from './components/AirQualityPanel';
import { ConditionsGrid } from './components/ConditionsGrid';
import { CurrentConditions } from './components/CurrentConditions';
import { DailyForecast } from './components/DailyForecast';
import { ErrorState } from './components/ErrorState';
import { FavoritesBar } from './components/FavoritesBar';
import { Header } from './components/Header';
import { HourlyForecast } from './components/HourlyForecast';
import { CloseIcon, OfflineIcon, PinIcon } from './components/Icons';
import { Onboarding } from './components/Onboarding';
import { Panel } from './components/Panel';
import { PrecipitationPanel } from './components/PrecipitationPanel';
import type { SearchBoxHandle } from './components/SearchBox';
import { HeroSkeleton, SheetSkeleton } from './components/Skeletons';
import { SunMoonPanel } from './components/SunMoonPanel';
import { WidgetBoundary } from './components/WidgetBoundary';
import { config } from './config';
import { useGeolocation } from './hooks/useGeolocation';
import { useOnlineStatus } from './hooks/useOnlineStatus';
import { useAirQuality, useForecast } from './hooks/useWeather';
import { pickScene } from './lib/scene';
import { useAppDispatch, useAppState } from './state/AppState';
import type { Place } from './types';

export default function App() {
  const { place } = useAppState();
  const dispatch = useAppDispatch();
  const online = useOnlineStatus();
  const geolocation = useGeolocation();
  const searchRef = useRef<SearchBoxHandle>(null);

  const forecast = useForecast(place);
  const airQuality = useAirQuality(place);
  const [namingLocation, setNamingLocation] = useState(false);

  const selectPlace = useCallback(
    (next: Place) => {
      geolocation.clearError();
      dispatch({ type: 'place/selected', place: next });
    },
    [dispatch, geolocation],
  );

  /** Ask the browser where we are, then put a name on the coordinates. */
  const locate = useCallback(async () => {
    const coords = await geolocation.locate();
    if (!coords) {
      // Denied or unavailable: the notice explains why, and search is the fallback.
      searchRef.current?.focus();
      return;
    }
    setNamingLocation(true);
    try {
      selectPlace(await reverseGeocode(coords));
    } catch (error) {
      if (!isAbortError(error)) throw error;
    } finally {
      setNamingLocation(false);
    }
  }, [geolocation, selectPlace]);

  const reset = useCallback(() => selectPlace(config.defaultPlace), [selectPlace]);

  const data = forecast.state.status === 'success' ? forecast.state.data : null;
  const scene = pickScene(data);

  // Keep the browser tab title useful.
  useEffect(() => {
    document.title = place && data ? `${Math.round(data.current.temperature)}° ${place.name} | Skyline Weather` : 'Skyline Weather';
  }, [place, data]);

  // One sentence for screen readers whenever the page changes what it shows.
  const announcement =
    forecast.state.status === 'loading' && place
      ? `Loading weather for ${place.name}`
      : forecast.state.status === 'success' && place
        ? `Showing weather for ${place.name}`
        : '';

  const locating = geolocation.locating || namingLocation;

  return (
    <div className="app" data-scene={scene}>
      <a className="skip-link" href="#main">
        Skip to the weather
      </a>
      <div className="sky" aria-hidden="true">
        <div className="sky__fx" />
      </div>

      <Header searchRef={searchRef} onSelectPlace={selectPlace} onLocate={() => void locate()} locating={locating} onReset={reset} />
      <FavoritesBar onSelect={selectPlace} />

      <div className="notices">
        {!online ? (
          <p className="notice" role="status">
            <OfflineIcon size={18} />
            <span>You are offline. Anything on screen is from the last successful update.</span>
          </p>
        ) : null}
        {geolocation.error ? (
          <p className="notice" role="alert">
            <PinIcon size={18} />
            <span>{geolocation.error}</span>
            <button type="button" className="notice__close" onClick={geolocation.clearError} aria-label="Dismiss this message">
              <CloseIcon size={16} />
            </button>
          </p>
        ) : null}
      </div>

      <p className="visually-hidden" role="status" aria-live="polite">
        {announcement}
      </p>

      <main id="main" tabIndex={-1}>
        {place === null ? (
          <Onboarding
            onLocate={() => void locate()}
            locating={locating}
            onFocusSearch={() => searchRef.current?.focus()}
            onExample={(query) => searchRef.current?.search(query)}
            onSelect={selectPlace}
          />
        ) : forecast.state.status === 'error' ? (
          <div className="hero hero--message">
            <h1 className="hero__name">{place.name}</h1>
            <ErrorState error={forecast.state.error} subject={`the forecast for ${place.name}`} onRetry={forecast.refresh}>
              <button type="button" className="button button--ghost" onClick={() => searchRef.current?.focus()}>
                Search for another place
              </button>
            </ErrorState>
          </div>
        ) : forecast.state.status === 'success' ? (
          <>
            <WidgetBoundary name="the current conditions" resetKey={place.id}>
              <CurrentConditions
                place={place}
                forecast={forecast.state.data}
                updatedAt={forecast.state.updatedAt}
                staleError={forecast.state.staleError}
                isRefreshing={forecast.isRefreshing}
                onRefresh={() => {
                  forecast.refresh();
                  airQuality.refresh();
                }}
              />
            </WidgetBoundary>

            <div className="sheet">
              <div className="sheet__grid">
                <Panel title="Next 24 hours" name="the hourly forecast" className="panel--wide" resetKey={place.id}>
                  <HourlyForecast hours={forecast.state.data.hourly} />
                </Panel>

                <Panel title="7-day forecast" name="the daily forecast" className="panel--half" resetKey={place.id}>
                  <DailyForecast days={forecast.state.data.daily} />
                </Panel>

                <Panel title="Right now" name="the current readings" className="panel--half" resetKey={place.id}>
                  <ConditionsGrid current={forecast.state.data.current} />
                </Panel>

                <Panel title="Precipitation" name="the precipitation chart" resetKey={place.id}>
                  <PrecipitationPanel hours={forecast.state.data.hourly} today={forecast.state.data.daily[0]} />
                </Panel>

                <Panel title="Air quality" name="air quality" resetKey={place.id}>
                  <AirQualityPanel resource={airQuality} />
                </Panel>

                <Panel title="Sun and moon" name="the sun and moon times" resetKey={place.id}>
                  <SunMoonPanel today={forecast.state.data.daily[0]} latitude={place.latitude} />
                </Panel>
              </div>
            </div>
          </>
        ) : (
          <>
            <HeroSkeleton />
            <SheetSkeleton />
          </>
        )}
      </main>

      <footer className="footer">
        <p>
          Weather and air quality data by{' '}
          <a href="https://open-meteo.com/" target="_blank" rel="noreferrer">
            Open-Meteo
          </a>
          , licensed CC BY 4.0. Moon phase is calculated on your device.
        </p>
      </footer>
    </div>
  );
}
