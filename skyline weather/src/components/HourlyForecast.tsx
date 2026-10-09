/**
 * The next 24 hours as a horizontally scrolling strip. Each hour is a column,
 * and a line drawn across the columns shows how the temperature moves.
 */
import { useEffect, useRef, useState, type CSSProperties } from 'react';
import { formatHour } from '../lib/time';
import { convertTemperature, shortTemperature } from '../lib/units';
import { describeWeather } from '../lib/weatherCodes';
import { useAppState } from '../state/AppState';
import type { HourlyPoint } from '../types';
import { ChevronLeftIcon, ChevronRightIcon } from './Icons';
import { WeatherIcon } from './WeatherIcon';

/** Must match --hour-w and --plot-h in styles/components.css. */
const COLUMN_WIDTH = 68;
const PLOT_HEIGHT = 64;
/** Vertical room kept free at the top of the plot for the temperature label. */
const PLOT_PADDING = 26;

export function HourlyForecast({ hours }: { hours: HourlyPoint[] }) {
  const { temperatureUnit } = useAppState();
  const scroller = useRef<HTMLDivElement>(null);
  const [edges, setEdges] = useState({ start: true, end: false });

  // Enable or disable the scroll buttons as the strip moves.
  useEffect(() => {
    const element = scroller.current;
    if (!element) return;
    const update = () =>
      setEdges({
        start: element.scrollLeft <= 2,
        end: element.scrollLeft + element.clientWidth >= element.scrollWidth - 2,
      });
    update();
    element.addEventListener('scroll', update, { passive: true });
    window.addEventListener('resize', update);
    return () => {
      element.removeEventListener('scroll', update);
      window.removeEventListener('resize', update);
    };
  }, [hours.length]);

  if (hours.length === 0) {
    return <p className="panel__empty">The hourly forecast is not available for this place right now.</p>;
  }

  const temps = hours.map((hour) => (hour.temperature === null ? null : convertTemperature(hour.temperature, temperatureUnit)));
  const known = temps.filter((t): t is number => t !== null);
  const min = known.length ? Math.min(...known) : 0;
  const max = known.length ? Math.max(...known) : 1;
  const span = max - min || 1;

  /** Vertical position of a temperature inside the plot row, in px from its top. */
  const yFor = (t: number) => PLOT_PADDING + (1 - (t - min) / span) * (PLOT_HEIGHT - PLOT_PADDING - 6);

  // Points for the connecting line. Gaps in the data break the line.
  const segments: string[] = [];
  let currentSegment: string[] = [];
  temps.forEach((t, i) => {
    if (t === null) {
      if (currentSegment.length > 1) segments.push(currentSegment.join(' '));
      currentSegment = [];
    } else {
      currentSegment.push(`${i * COLUMN_WIDTH + COLUMN_WIDTH / 2},${yFor(t).toFixed(1)}`);
    }
  });
  if (currentSegment.length > 1) segments.push(currentSegment.join(' '));

  const scrollBy = (direction: 1 | -1) => {
    const element = scroller.current;
    if (!element) return;
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    element.scrollBy({ left: direction * COLUMN_WIDTH * 4, behavior: reduce ? 'auto' : 'smooth' });
  };

  return (
    <div className="hourly">
      <button
        type="button"
        className="hourly__nav hourly__nav--prev"
        onClick={() => scrollBy(-1)}
        disabled={edges.start}
        aria-label="Show earlier hours"
      >
        <ChevronLeftIcon size={18} />
      </button>

      {/* Focusable so keyboard users can scroll it with the arrow keys. */}
      <div className="hourly__scroller" ref={scroller} tabIndex={0} role="group" aria-label="Hourly forecast, scrollable">
        <div className="hourly__track" style={{ width: hours.length * COLUMN_WIDTH }}>
          <svg
            className="hourly__line"
            width={hours.length * COLUMN_WIDTH}
            height={PLOT_HEIGHT}
            aria-hidden="true"
            focusable="false"
          >
            {segments.map((points) => (
              <polyline key={points} points={points} />
            ))}
          </svg>
          <ol className="hourly__list">
            {hours.map((hour, index) => {
              const condition = describeWeather(hour.weatherCode, hour.isDay);
              const t = temps[index] ?? null;
              const chance = hour.precipitationProbability;
              const label = index === 0 ? 'Now' : formatHour(hour.time);
              return (
                <li key={hour.time} className="hourly__hour">
                  <span className="hourly__time">{label}</span>
                  <WeatherIcon name={condition.icon} size={30} label={condition.label} />
                  <span className="hourly__plot" style={{ '--y': `${t === null ? PLOT_HEIGHT / 2 : yFor(t)}px` } as CSSProperties}>
                    <span className="hourly__temp">{shortTemperature(hour.temperature, temperatureUnit)}</span>
                    {t === null ? null : <span className="hourly__dot" aria-hidden="true" />}
                  </span>
                  <span className={`hourly__rain${chance !== null && chance >= 20 ? ' is-likely' : ''}`}>
                    <span className="visually-hidden">Chance of rain </span>
                    {chance === null ? '--' : `${Math.round(chance)}%`}
                  </span>
                </li>
              );
            })}
          </ol>
        </div>
      </div>

      <button
        type="button"
        className="hourly__nav hourly__nav--next"
        onClick={() => scrollBy(1)}
        disabled={edges.end}
        aria-label="Show later hours"
      >
        <ChevronRightIcon size={18} />
      </button>
    </div>
  );
}
