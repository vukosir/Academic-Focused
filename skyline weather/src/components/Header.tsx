import type { Ref } from 'react';
import { config } from '../config';
import { useAppDispatch, useAppState } from '../state/AppState';
import type { Place } from '../types';
import { HomeIcon, LocateIcon, MoonIcon, Spinner, SunIcon } from './Icons';
import { SearchBox, type SearchBoxHandle } from './SearchBox';
import { SegmentedControl } from './SegmentedControl';

interface Props {
  searchRef: Ref<SearchBoxHandle>;
  onSelectPlace: (place: Place) => void;
  onLocate: () => void;
  locating: boolean;
  onReset: () => void;
}

/** Brand, search, location shortcuts, and the unit and theme switches. */
export function Header({ searchRef, onSelectPlace, onLocate, locating, onReset }: Props) {
  const { temperatureUnit, system, theme } = useAppState();
  const dispatch = useAppDispatch();
  const nextTheme = theme === 'dark' ? 'light' : 'dark';

  return (
    <header className="topbar">
      <a className="brand" href="./" aria-label="Skyline Weather, home">
        <svg className="brand__mark" width="30" height="30" viewBox="0 0 64 64" aria-hidden="true" focusable="false">
          <path d="M8 46a24 24 0 0 1 48 0" fill="none" stroke="currentColor" strokeOpacity=".5" strokeWidth="4" strokeDasharray="2 7" strokeLinecap="round" />
          <circle cx="43" cy="27" r="8" fill="var(--sun)" />
          <path d="M6 46h52" stroke="currentColor" strokeWidth="4" strokeLinecap="round" />
        </svg>
        <span className="brand__name">Skyline</span>
      </a>

      <div className="topbar__search">
        <SearchBox ref={searchRef} onSelect={onSelectPlace} />
        <button
          type="button"
          className="icon-button"
          onClick={onLocate}
          disabled={locating}
          aria-label={locating ? 'Finding your location' : 'Use my location'}
          title="Use my location"
        >
          {locating ? <Spinner size={18} /> : <LocateIcon size={18} />}
        </button>
        <button
          type="button"
          className="icon-button"
          onClick={onReset}
          aria-label={`Reset to the default location, ${config.defaultPlace.name}`}
          title={`Reset to ${config.defaultPlace.name}`}
        >
          <HomeIcon size={18} />
        </button>
      </div>

      <div className="topbar__settings">
        <SegmentedControl
          legend="Temperature unit"
          value={temperatureUnit}
          onChange={(unit) => dispatch({ type: 'units/temperature', unit })}
          options={[
            { value: 'celsius', label: '°C', spoken: 'Celsius' },
            { value: 'fahrenheit', label: '°F', spoken: 'Fahrenheit' },
          ]}
        />
        <SegmentedControl
          legend="Units for wind, distance, pressure and rainfall"
          value={system}
          onChange={(next) => dispatch({ type: 'units/system', system: next })}
          options={[
            { value: 'metric', label: 'km/h', spoken: 'Metric units' },
            { value: 'imperial', label: 'mph', spoken: 'Imperial units' },
          ]}
        />
        <button
          type="button"
          className="icon-button"
          onClick={() => dispatch({ type: 'theme/set', theme: nextTheme })}
          aria-label={`Switch to ${nextTheme} theme`}
          title={`Switch to ${nextTheme} theme`}
        >
          {theme === 'dark' ? <SunIcon size={18} /> : <MoonIcon size={18} />}
        </button>
      </div>
    </header>
  );
}
