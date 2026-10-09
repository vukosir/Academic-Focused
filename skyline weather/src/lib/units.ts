/**
 * Unit conversion and number formatting.
 *
 * Data is stored in metric. Each formatter takes a metric value plus the
 * user's unit choice and returns the pieces separately, so components can
 * style the number and the unit differently. A null input always produces the
 * placeholder, which is how missing API fields surface in the UI.
 */
import type { MeasurementSystem, TemperatureUnit } from '../types';

export const PLACEHOLDER = '--';

export interface Measurement {
  value: string;
  unit: string;
  /** Spoken form for screen readers, for example "24 degrees Celsius". */
  spoken: string;
}

export const celsiusToFahrenheit = (c: number) => (c * 9) / 5 + 32;
export const kmhToMph = (kmh: number) => kmh * 0.621371;
export const hpaToInHg = (hpa: number) => hpa * 0.02953;
export const metresToKm = (m: number) => m / 1000;
export const metresToMiles = (m: number) => m / 1609.344;
export const mmToInches = (mm: number) => mm / 25.4;

function missing(unit: string): Measurement {
  return { value: PLACEHOLDER, unit, spoken: 'not available' };
}

/** Round to a number of decimals and avoid "-0". */
function fixed(value: number, decimals: number): string {
  const rounded = Number(value.toFixed(decimals));
  return (Object.is(rounded, -0) ? 0 : rounded).toFixed(decimals);
}

export function convertTemperature(celsius: number, unit: TemperatureUnit): number {
  return unit === 'fahrenheit' ? celsiusToFahrenheit(celsius) : celsius;
}

export function formatTemperature(celsius: number | null, unit: TemperatureUnit): Measurement {
  const symbol = unit === 'fahrenheit' ? '°F' : '°C';
  if (celsius === null) return missing(symbol);
  const value = fixed(convertTemperature(celsius, unit), 0);
  return { value, unit: symbol, spoken: `${value} degrees ${unit === 'fahrenheit' ? 'Fahrenheit' : 'Celsius'}` };
}

/** Compact form used in dense lists: "24°". */
export function shortTemperature(celsius: number | null, unit: TemperatureUnit): string {
  const { value } = formatTemperature(celsius, unit);
  return value === PLACEHOLDER ? value : `${value}°`;
}

export function formatSpeed(kmh: number | null, system: MeasurementSystem): Measurement {
  const unit = system === 'imperial' ? 'mph' : 'km/h';
  if (kmh === null) return missing(unit);
  const value = fixed(system === 'imperial' ? kmhToMph(kmh) : kmh, 0);
  return { value, unit, spoken: `${value} ${system === 'imperial' ? 'miles per hour' : 'kilometres per hour'}` };
}

export function formatPressure(hpa: number | null, system: MeasurementSystem): Measurement {
  const unit = system === 'imperial' ? 'inHg' : 'hPa';
  if (hpa === null) return missing(unit);
  const value = system === 'imperial' ? fixed(hpaToInHg(hpa), 2) : fixed(hpa, 0);
  return { value, unit, spoken: `${value} ${system === 'imperial' ? 'inches of mercury' : 'hectopascals'}` };
}

export function formatDistance(metres: number | null, system: MeasurementSystem): Measurement {
  const unit = system === 'imperial' ? 'mi' : 'km';
  if (metres === null) return missing(unit);
  const converted = system === 'imperial' ? metresToMiles(metres) : metresToKm(metres);
  // One decimal only helps when the distance is short.
  const value = fixed(converted, converted < 10 ? 1 : 0);
  return { value, unit, spoken: `${value} ${system === 'imperial' ? 'miles' : 'kilometres'}` };
}

export function formatPrecipitation(mm: number | null, system: MeasurementSystem): Measurement {
  const unit = system === 'imperial' ? 'in' : 'mm';
  if (mm === null) return missing(unit);
  const value = system === 'imperial' ? fixed(mmToInches(mm), 2) : fixed(mm, 1);
  return { value, unit, spoken: `${value} ${system === 'imperial' ? 'inches' : 'millimetres'}` };
}

export function formatPercent(percent: number | null): Measurement {
  if (percent === null) return missing('%');
  const value = fixed(percent, 0);
  return { value, unit: '%', spoken: `${value} percent` };
}
