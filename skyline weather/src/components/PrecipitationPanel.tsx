/**
 * Chance of precipitation for each of the next 24 hours, as a bar chart with a
 * readout line. Hovering or touching a bar, or arrowing through the chart with
 * the keyboard, updates the readout. The same numbers are also available as
 * text in the hourly strip.
 */
import { useState, type CSSProperties, type KeyboardEvent } from 'react';
import { formatHour } from '../lib/time';
import { formatPrecipitation } from '../lib/units';
import { useAppState } from '../state/AppState';
import type { DailyPoint, HourlyPoint } from '../types';

interface Props {
  hours: HourlyPoint[];
  today: DailyPoint | undefined;
}

export function PrecipitationPanel({ hours, today }: Props) {
  const { system } = useAppState();
  const [active, setActive] = useState<number | null>(null);

  const usable = hours.filter((hour) => hour.precipitationProbability !== null);
  if (usable.length === 0) {
    return <p className="panel__empty">Precipitation chances are not available for this place right now.</p>;
  }

  // The wettest hour is the default readout.
  let peakIndex = 0;
  hours.forEach((hour, index) => {
    if ((hour.precipitationProbability ?? -1) > (hours[peakIndex]?.precipitationProbability ?? -1)) peakIndex = index;
  });
  const peak = hours[peakIndex];
  const peakChance = Math.round(peak?.precipitationProbability ?? 0);
  const expected = hours.reduce((sum, hour) => sum + (hour.precipitation ?? 0), 0);
  const total = formatPrecipitation(expected, system);

  const shownIndex = active ?? peakIndex;
  const shown = hours[shownIndex];
  const shownAmount = formatPrecipitation(shown?.precipitation ?? null, system);

  const onKeyDown = (event: KeyboardEvent<HTMLOListElement>) => {
    const last = hours.length - 1;
    const from = active ?? peakIndex;
    let next: number | null = null;
    if (event.key === 'ArrowRight') next = Math.min(last, from + 1);
    else if (event.key === 'ArrowLeft') next = Math.max(0, from - 1);
    else if (event.key === 'Home') next = 0;
    else if (event.key === 'End') next = last;
    if (next === null) return;
    event.preventDefault();
    setActive(next);
  };

  const headline =
    peakChance < 10
      ? 'Dry for the next 24 hours'
      : peakChance < 40
        ? `A small chance of rain, highest around ${formatHour(peak?.time ?? '')}`
        : `Rain is likely around ${formatHour(peak?.time ?? '')}`;

  return (
    <div className="precip">
      <p className="precip__headline">{headline}</p>

      {/* Announced politely, so keyboard users hear each hour as they arrow through the chart. */}
      <p className="precip__readout" aria-live="polite">
        <span className="precip__readout-time">
          {shownIndex === 0 ? 'Now' : formatHour(shown?.time ?? '')}
          {active === null ? ' (peak)' : ''}
        </span>
        <span className="precip__readout-value">
          {shown?.precipitationProbability === null || shown === undefined ? '--' : `${Math.round(shown.precipitationProbability)}%`}
        </span>
        <span className="precip__readout-amount">
          {shownAmount.value} {shownAmount.unit}
        </span>
      </p>

      <div className="precip__plot">
        <span className="precip__scale precip__scale--100" aria-hidden="true">
          100%
        </span>
        <span className="precip__scale precip__scale--50" aria-hidden="true">
          50%
        </span>
      {/* One tab stop for the whole chart. Arrow keys move between hours. */}
      <ol
        className="precip__chart"
        tabIndex={0}
        aria-label="Chance of precipitation by hour. Use the left and right arrow keys to read each hour."
        onKeyDown={onKeyDown}
        onBlur={() => setActive(null)}
        onMouseLeave={() => setActive(null)}
      >
        {hours.map((hour, index) => {
          const chance = hour.precipitationProbability;
          return (
            <li
              key={hour.time}
              className={`precip__col${index === shownIndex ? ' is-active' : ''}`}
              onMouseEnter={() => setActive(index)}
              onTouchStart={() => setActive(index)}
            >
              <span className="precip__bar" aria-hidden="true" style={{ '--h': `${Math.max(0, Math.min(100, chance ?? 0))}%` } as CSSProperties} />
              {index % 6 === 0 ? (
                <span className="precip__tick" aria-hidden="true">
                  {index === 0 ? 'Now' : formatHour(hour.time)}
                </span>
              ) : null}
            </li>
          );
        })}
      </ol>
      </div>

      <dl className="precip__stats">
        <div>
          <dt>Expected in 24 h</dt>
          <dd>
            {total.value} {total.unit}
          </dd>
        </div>
        <div>
          <dt>Highest chance today</dt>
          <dd>{today?.precipitationProbabilityMax == null ? '--' : `${Math.round(today.precipitationProbabilityMax)}%`}</dd>
        </div>
      </dl>
    </div>
  );
}
