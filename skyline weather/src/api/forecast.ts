/**
 * Forecast: current conditions, hourly and daily data from Open-Meteo.
 *
 * getForecast() is what the UI calls. buildForecastUrl() and
 * normaliseForecast() are exported separately so they can be tested without a
 * network.
 */
import { config } from '../config';
import type { CurrentWeather, DailyPoint, Forecast, HourlyPoint, Place } from '../types';
import { TtlCache, type CacheResult } from './cache';
import { AppError } from './errors';
import { buildUrl, fetchJson } from './http';
import { isObject, list, num, str } from './parse';

const CURRENT_FIELDS = [
  'temperature_2m',
  'relative_humidity_2m',
  'apparent_temperature',
  'is_day',
  'precipitation',
  'weather_code',
  'cloud_cover',
  'pressure_msl',
  'wind_speed_10m',
  'wind_direction_10m',
  'wind_gusts_10m',
];

const HOURLY_FIELDS = [
  'temperature_2m',
  'apparent_temperature',
  'precipitation_probability',
  'precipitation',
  'weather_code',
  'visibility',
  'dew_point_2m',
  'uv_index',
  'is_day',
  'wind_speed_10m',
];

const DAILY_FIELDS = [
  'weather_code',
  'temperature_2m_max',
  'temperature_2m_min',
  'sunrise',
  'sunset',
  'uv_index_max',
  'precipitation_sum',
  'precipitation_probability_max',
  'wind_speed_10m_max',
];

export const HOURS_SHOWN = 24;
export const DAYS_SHOWN = 7;

const forecastCache = new TtlCache<Forecast>({ maxEntries: 12, storageKey: 'cache:forecast' });

export function buildForecastUrl(place: Pick<Place, 'latitude' | 'longitude'>): string {
  // Units are left at the API defaults (Celsius, km/h, mm, hPa). Conversion to
  // imperial happens in the UI so a unit switch costs no request.
  return buildUrl(config.endpoints.forecast, {
    latitude: place.latitude.toFixed(4),
    longitude: place.longitude.toFixed(4),
    current: CURRENT_FIELDS.join(','),
    hourly: HOURLY_FIELDS.join(','),
    daily: DAILY_FIELDS.join(','),
    timezone: 'auto',
    forecast_days: DAYS_SHOWN,
  });
}

/** Index of the hourly slot that contains `currentTime` (both are local ISO strings). */
function currentHourIndex(times: unknown[], currentTime: string): number {
  const hour = `${currentTime.slice(0, 13)}:00`;
  const exact = times.indexOf(hour);
  if (exact !== -1) return exact;
  // ISO strings in the same format sort chronologically, so plain comparison works.
  const next = times.findIndex((time) => typeof time === 'string' && time >= hour);
  return next === -1 ? 0 : next;
}

/**
 * Convert a raw Open-Meteo forecast into the app's Forecast shape.
 *
 * Only the current temperature is treated as essential. Any other field that
 * is missing becomes null, and missing hourly or daily blocks become empty
 * arrays, so a partial response still renders what it can.
 */
export function normaliseForecast(raw: unknown, fetchedAt: number = Date.now()): Forecast {
  if (!isObject(raw)) throw new AppError('malformed', 'Forecast response was not an object');

  const cur = raw.current;
  if (!isObject(cur)) throw new AppError('malformed', 'Forecast response has no current conditions');
  const temperature = num(cur.temperature_2m);
  if (temperature === null) throw new AppError('malformed', 'Forecast response has no current temperature');

  const currentTime = str(cur.time) ?? new Date(fetchedAt + (num(raw.utc_offset_seconds) ?? 0) * 1000).toISOString().slice(0, 16);

  const hourlyRaw = raw.hourly;
  const times = list(hourlyRaw, 'time');
  const start = times.length > 0 ? currentHourIndex(times, currentTime) : 0;
  const column = (key: string) => list(hourlyRaw, key);
  const h = {
    temperature: column('temperature_2m'),
    apparent: column('apparent_temperature'),
    probability: column('precipitation_probability'),
    precipitation: column('precipitation'),
    code: column('weather_code'),
    visibility: column('visibility'),
    dewPoint: column('dew_point_2m'),
    uv: column('uv_index'),
    isDay: column('is_day'),
    wind: column('wind_speed_10m'),
  };

  const hourly: HourlyPoint[] = [];
  for (let i = start; i < times.length && hourly.length < HOURS_SHOWN; i += 1) {
    const time = str(times[i]);
    if (time === null) continue;
    hourly.push({
      time,
      temperature: num(h.temperature[i]),
      apparentTemperature: num(h.apparent[i]),
      precipitationProbability: num(h.probability[i]),
      precipitation: num(h.precipitation[i]),
      weatherCode: num(h.code[i]),
      isDay: h.isDay[i] !== 0,
      windSpeed: num(h.wind[i]),
    });
  }

  const dailyRaw = raw.daily;
  const dates = list(dailyRaw, 'time');
  const dcol = (key: string) => list(dailyRaw, key);
  const d = {
    code: dcol('weather_code'),
    max: dcol('temperature_2m_max'),
    min: dcol('temperature_2m_min'),
    sunrise: dcol('sunrise'),
    sunset: dcol('sunset'),
    uv: dcol('uv_index_max'),
    sum: dcol('precipitation_sum'),
    probability: dcol('precipitation_probability_max'),
    wind: dcol('wind_speed_10m_max'),
  };

  const daily: DailyPoint[] = [];
  for (let i = 0; i < dates.length && daily.length < DAYS_SHOWN; i += 1) {
    const date = str(dates[i]);
    if (date === null) continue;
    daily.push({
      date,
      weatherCode: num(d.code[i]),
      tempMax: num(d.max[i]),
      tempMin: num(d.min[i]),
      sunrise: str(d.sunrise[i]),
      sunset: str(d.sunset[i]),
      uvIndexMax: num(d.uv[i]),
      precipitationSum: num(d.sum[i]),
      precipitationProbabilityMax: num(d.probability[i]),
      windSpeedMax: num(d.wind[i]),
    });
  }

  const current: CurrentWeather = {
    time: currentTime,
    temperature,
    apparentTemperature: num(cur.apparent_temperature),
    isDay: cur.is_day !== 0,
    weatherCode: num(cur.weather_code),
    humidity: num(cur.relative_humidity_2m),
    windSpeed: num(cur.wind_speed_10m),
    windDirection: num(cur.wind_direction_10m),
    windGusts: num(cur.wind_gusts_10m),
    pressure: num(cur.pressure_msl),
    cloudCover: num(cur.cloud_cover),
    precipitation: num(cur.precipitation),
    // These three are only published hourly, so read them from the current hour.
    visibility: num(h.visibility[start]),
    uvIndex: num(h.uv[start]),
    dewPoint: num(h.dewPoint[start]),
  };

  return {
    latitude: num(raw.latitude) ?? 0,
    longitude: num(raw.longitude) ?? 0,
    timezone: str(raw.timezone),
    timezoneAbbreviation: str(raw.timezone_abbreviation),
    utcOffsetSeconds: num(raw.utc_offset_seconds) ?? 0,
    current,
    hourly,
    daily,
    fetchedAt,
  };
}

/** Sanity check for forecasts restored from localStorage. */
function isForecast(value: unknown): value is Forecast {
  return (
    isObject(value) &&
    isObject(value.current) &&
    typeof value.current.temperature === 'number' &&
    Array.isArray(value.hourly) &&
    Array.isArray(value.daily)
  );
}

/**
 * Load the forecast for a place, reusing a cached copy while it is fresh.
 * If the network fails and an older copy exists, that copy is returned with
 * source "stale" and the error attached, so the UI can show both.
 */
export async function getForecast(place: Place, options: { force?: boolean } = {}): Promise<CacheResult<Forecast>> {
  const result = await forecastCache.resolve(
    place.id,
    async () => normaliseForecast(await fetchJson(buildForecastUrl(place))),
    { ttlMs: config.forecastTtlMs, force: options.force },
  );
  // A corrupted persisted entry must not reach the UI.
  if (!isForecast(result.value)) {
    forecastCache.clear();
    throw result.error ?? new AppError('malformed', 'Cached forecast was unreadable');
  }
  return result;
}

/** Test helper. */
export function clearForecastCache(): void {
  forecastCache.clear();
}
