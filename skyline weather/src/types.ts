/**
 * Shared domain types.
 *
 * Everything the UI renders is expressed in these normalised shapes, never in
 * the raw API payloads. All measurements are stored in metric units and
 * converted at display time (see lib/units.ts), so switching units never needs
 * a new request.
 */

export interface Place {
  /** Stable id derived from rounded coordinates, used for favorites and cache keys. */
  id: string;
  name: string;
  /** First-level administrative area, for example a province or state. */
  region?: string;
  country?: string;
  countryCode?: string;
  latitude: number;
  longitude: number;
  timezone?: string;
}

export type TemperatureUnit = 'celsius' | 'fahrenheit';
export type MeasurementSystem = 'metric' | 'imperial';
export type Theme = 'light' | 'dark';

export interface CurrentWeather {
  /** Local wall-clock time at the place, ISO without offset (2026-10-09T18:45). */
  time: string;
  /** Degrees Celsius. The only field that must be present. */
  temperature: number;
  apparentTemperature: number | null;
  isDay: boolean;
  /** WMO weather interpretation code. */
  weatherCode: number | null;
  /** Percent. */
  humidity: number | null;
  /** km/h. */
  windSpeed: number | null;
  /** Degrees, direction the wind blows from. */
  windDirection: number | null;
  /** km/h. */
  windGusts: number | null;
  /** hPa at mean sea level. */
  pressure: number | null;
  /** Percent. */
  cloudCover: number | null;
  /** mm in the current interval. */
  precipitation: number | null;
  /** Metres. */
  visibility: number | null;
  uvIndex: number | null;
  /** Degrees Celsius. */
  dewPoint: number | null;
}

export interface HourlyPoint {
  time: string;
  temperature: number | null;
  apparentTemperature: number | null;
  /** Percent. */
  precipitationProbability: number | null;
  /** mm. */
  precipitation: number | null;
  weatherCode: number | null;
  isDay: boolean;
  windSpeed: number | null;
}

export interface DailyPoint {
  /** Local date, YYYY-MM-DD. */
  date: string;
  weatherCode: number | null;
  tempMax: number | null;
  tempMin: number | null;
  sunrise: string | null;
  sunset: string | null;
  uvIndexMax: number | null;
  /** mm. */
  precipitationSum: number | null;
  /** Percent. */
  precipitationProbabilityMax: number | null;
  windSpeedMax: number | null;
}

export interface Forecast {
  latitude: number;
  longitude: number;
  timezone: string | null;
  timezoneAbbreviation: string | null;
  /** Offset of the place from UTC, used to compute its local "now". */
  utcOffsetSeconds: number;
  current: CurrentWeather;
  /** The next 24 hours, starting with the current hour. May be empty. */
  hourly: HourlyPoint[];
  /** Up to 7 days, starting today. May be empty. */
  daily: DailyPoint[];
  /** Epoch milliseconds when this forecast came off the network. */
  fetchedAt: number;
}

export interface AirQuality {
  time: string | null;
  usAqi: number | null;
  europeanAqi: number | null;
  /** All pollutant concentrations are in micrograms per cubic metre. */
  pm25: number | null;
  pm10: number | null;
  ozone: number | null;
  nitrogenDioxide: number | null;
  sulphurDioxide: number | null;
  carbonMonoxide: number | null;
  fetchedAt: number;
}
