/**
 * Seven days: icon, chance of rain, and a bar that places each day's low and
 * high on a shared scale, so warmer and cooler days are comparable at a glance.
 */
import type { CSSProperties } from 'react';
import { formatDayMonth, formatWeekday } from '../lib/time';
import { shortTemperature } from '../lib/units';
import { describeWeather } from '../lib/weatherCodes';
import { useAppState } from '../state/AppState';
import type { DailyPoint } from '../types';
import { DropIcon } from './Icons';
import { WeatherIcon } from './WeatherIcon';

export function DailyForecast({ days }: { days: DailyPoint[] }) {
  const { temperatureUnit } = useAppState();

  if (days.length === 0) {
    return <p className="panel__empty">The daily forecast is not available for this place right now.</p>;
  }

  // The scale runs from the coldest low to the warmest high of the week.
  const lows = days.map((d) => d.tempMin).filter((v): v is number => v !== null);
  const highs = days.map((d) => d.tempMax).filter((v): v is number => v !== null);
  const weekMin = lows.length ? Math.min(...lows) : 0;
  const weekMax = highs.length ? Math.max(...highs) : 1;
  const span = weekMax - weekMin || 1;

  return (
    <ol className="daily">
      {days.map((day, index) => {
        const condition = describeWeather(day.weatherCode, true);
        const hasRange = day.tempMin !== null && day.tempMax !== null;
        const start = hasRange ? ((day.tempMin as number) - weekMin) / span : 0;
        const end = hasRange ? ((day.tempMax as number) - weekMin) / span : 0;
        const chance = day.precipitationProbabilityMax;
        const low = shortTemperature(day.tempMin, temperatureUnit);
        const high = shortTemperature(day.tempMax, temperatureUnit);

        return (
          <li key={day.date} className="daily__row">
            <span className="daily__day">
              <span className="daily__weekday">{index === 0 ? 'Today' : formatWeekday(day.date, 'short')}</span>
              <span className="daily__date">{formatDayMonth(day.date)}</span>
            </span>
            <span className="daily__icon">
              <WeatherIcon name={condition.icon} size={30} />
              <span className="daily__label">{condition.label}</span>
            </span>
            <span className={`daily__rain${chance !== null && chance >= 20 ? ' is-likely' : ''}`}>
              <DropIcon size={13} />
              <span className="visually-hidden">Chance of rain </span>
              {chance === null ? '--' : `${Math.round(chance)}%`}
            </span>
            <span className="daily__temps">
              <span className="daily__low">
                <span className="visually-hidden">Low </span>
                {low}
              </span>
              <span className="daily__bar" aria-hidden="true">
                {hasRange ? (
                  <span
                    className="daily__bar-fill"
                    style={{ '--start': `${(start * 100).toFixed(1)}%`, '--end': `${(end * 100).toFixed(1)}%` } as CSSProperties}
                  />
                ) : null}
              </span>
              <span className="daily__high">
                <span className="visually-hidden">High </span>
                {high}
              </span>
            </span>
          </li>
        );
      })}
    </ol>
  );
}
