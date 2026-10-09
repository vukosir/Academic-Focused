/**
 * The detailed readings for right now: wind, humidity, dew point, pressure,
 * visibility, UV index and cloud cover. Every tile copes with a missing value.
 */
import type { CSSProperties, ReactNode } from 'react';
import { compassPoint, compassWords, describeDewPoint, describePressure, describeUv, describeVisibility } from '../lib/scales';
import { formatDistance, formatPercent, formatPressure, formatSpeed, formatTemperature, PLACEHOLDER, type Measurement } from '../lib/units';
import { useAppState } from '../state/AppState';
import type { CurrentWeather } from '../types';
import { ArrowIcon } from './Icons';

interface TileProps {
  label: string;
  measurement: Measurement;
  /** One short line of interpretation under the number. */
  note?: string;
  children?: ReactNode;
}

function Tile({ label, measurement, note, children }: TileProps) {
  const missing = measurement.value === PLACEHOLDER;
  return (
    <div className="tile">
      <dt className="tile__label">{label}</dt>
      <dd className="tile__body">
        <span className="tile__value">
          <span className="visually-hidden">{measurement.spoken}</span>
          <span aria-hidden="true">
            {measurement.value}
            {missing ? null : <span className="tile__unit">{measurement.unit}</span>}
          </span>
        </span>
        <span className="tile__note">{missing ? 'Not reported for this place' : note}</span>
        {children}
      </dd>
    </div>
  );
}

/** A thin track with a marker, used for values on a bounded scale. */
function Meter({ fraction, level }: { fraction: number; level?: string }) {
  const clamped = Math.min(1, Math.max(0, fraction));
  return (
    <span className="meter" data-level={level} aria-hidden="true">
      <span className="meter__fill" style={{ '--fraction': clamped } as CSSProperties} />
    </span>
  );
}

export function ConditionsGrid({ current }: { current: CurrentWeather }) {
  const { system, temperatureUnit } = useAppState();

  const wind = formatSpeed(current.windSpeed, system);
  const gusts = formatSpeed(current.windGusts, system);
  const point = current.windDirection === null ? null : compassPoint(current.windDirection);
  const uv = current.uvIndex === null ? null : describeUv(current.uvIndex);

  const windNote = [
    point ? `From the ${compassWords(point)}` : null,
    gusts.value !== PLACEHOLDER ? `gusts ${gusts.value} ${gusts.unit}` : null,
  ]
    .filter(Boolean)
    .join(', ');

  return (
    <dl className="tiles">
      <Tile label="Wind" measurement={wind} note={windNote || undefined}>
        {current.windDirection !== null && point ? (
          <span className="compass" aria-hidden="true">
            <span className="compass__n">N</span>
            {/* Meteorological direction is where the wind comes from, so the arrow points the opposite way. */}
            <span className="compass__arrow" style={{ '--bearing': `${current.windDirection + 180}deg` } as CSSProperties}>
              <ArrowIcon size={18} />
            </span>
          </span>
        ) : null}
      </Tile>

      <Tile
        label="Humidity"
        measurement={formatPercent(current.humidity)}
        note={current.humidity === null ? undefined : current.humidity < 30 ? 'Dry' : current.humidity <= 60 ? 'Comfortable' : 'Humid'}
      >
        {current.humidity === null ? null : <Meter fraction={current.humidity / 100} />}
      </Tile>

      <Tile
        label="Dew point"
        measurement={formatTemperature(current.dewPoint, temperatureUnit)}
        note={current.dewPoint === null ? undefined : describeDewPoint(current.dewPoint)}
      />

      <Tile
        label="Pressure"
        measurement={formatPressure(current.pressure, system)}
        note={current.pressure === null ? undefined : describePressure(current.pressure)}
      />

      <Tile
        label="Visibility"
        measurement={formatDistance(current.visibility, system)}
        note={current.visibility === null ? undefined : describeVisibility(current.visibility)}
      />

      <Tile
        label="UV index"
        measurement={
          current.uvIndex === null
            ? { value: PLACEHOLDER, unit: '', spoken: 'not available' }
            : { value: current.uvIndex.toFixed(current.uvIndex < 10 ? 1 : 0), unit: '', spoken: `${current.uvIndex.toFixed(1)}` }
        }
        note={uv ? `${uv.label}. ${uv.advice}` : undefined}
      >
        {current.uvIndex === null || !uv ? null : <Meter fraction={current.uvIndex / 11} level={uv.level} />}
      </Tile>

      <Tile
        label="Cloud cover"
        measurement={formatPercent(current.cloudCover)}
        note={
          current.cloudCover === null
            ? undefined
            : current.cloudCover < 20
              ? 'Mostly open sky'
              : current.cloudCover < 70
                ? 'Broken cloud'
                : 'Mostly covered'
        }
      >
        {current.cloudCover === null ? null : <Meter fraction={current.cloudCover / 100} />}
      </Tile>

      <Tile
        label="Feels like"
        measurement={formatTemperature(current.apparentTemperature, temperatureUnit)}
        note={
          current.apparentTemperature === null
            ? undefined
            : Math.abs(current.apparentTemperature - current.temperature) < 1
              ? 'Close to the measured temperature'
              : current.apparentTemperature > current.temperature
                ? 'Warmer than measured, from humidity'
                : 'Cooler than measured, from wind or dry air'
        }
      />
    </dl>
  );
}
