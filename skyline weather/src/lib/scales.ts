/**
 * Plain-language readings for index values: air quality, UV, wind direction
 * and comfort. Each scale returns a level so the UI can pair a colour with a
 * written label and never rely on colour alone.
 */

/** Severity steps shared by the scales, mapped to status colours in CSS. */
export type Level = 'good' | 'moderate' | 'elevated' | 'high' | 'severe' | 'extreme';

export interface Reading {
  label: string;
  level: Level;
  advice: string;
}

/** US EPA Air Quality Index bands. */
export function describeUsAqi(aqi: number): Reading {
  if (aqi <= 50) return { label: 'Good', level: 'good', advice: 'Air quality is fine for everyone.' };
  if (aqi <= 100)
    return { label: 'Moderate', level: 'moderate', advice: 'Fine for most people. Unusually sensitive people may notice it.' };
  if (aqi <= 150)
    return {
      label: 'Unhealthy for sensitive groups',
      level: 'elevated',
      advice: 'People with asthma or heart conditions should take it easy outdoors.',
    };
  if (aqi <= 200) return { label: 'Unhealthy', level: 'high', advice: 'Everyone may feel effects. Keep outdoor exertion short.' };
  if (aqi <= 300) return { label: 'Very unhealthy', level: 'severe', advice: 'Avoid outdoor exertion and keep windows closed.' };
  return { label: 'Hazardous', level: 'extreme', advice: 'Stay indoors if you can.' };
}

/** Upper bound of the US AQI scale used for the meter. */
export const AQI_SCALE_MAX = 300;

/** WHO UV index bands. */
export function describeUv(uv: number): Reading {
  if (uv < 3) return { label: 'Low', level: 'good', advice: 'No protection needed.' };
  if (uv < 6) return { label: 'Moderate', level: 'moderate', advice: 'Wear sunscreen if you are out for long.' };
  if (uv < 8) return { label: 'High', level: 'elevated', advice: 'Sunscreen and a hat. Seek shade around midday.' };
  if (uv < 11) return { label: 'Very high', level: 'high', advice: 'Skin burns quickly. Cover up and stay in shade.' };
  return { label: 'Extreme', level: 'extreme', advice: 'Avoid the midday sun entirely.' };
}

const COMPASS = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW'];
const COMPASS_WORDS: Record<string, string> = { N: 'north', E: 'east', S: 'south', W: 'west' };

/** 251 -> "WSW" */
export function compassPoint(degrees: number): string {
  const normalised = ((degrees % 360) + 360) % 360;
  return COMPASS[Math.round(normalised / 22.5) % 16] ?? 'N';
}

/** "WSW" -> "west-southwest" for screen readers. */
export function compassWords(point: string): string {
  const words = point.split('').map((letter) => COMPASS_WORDS[letter] ?? letter);
  if (words.length === 3) return `${words[0]}-${words[1]}${words[2]}`;
  return words.join('');
}

/** How the humidity level feels, from the dew point in Celsius. */
export function describeDewPoint(dewPointC: number): string {
  if (dewPointC < 10) return 'Dry air';
  if (dewPointC < 16) return 'Comfortable';
  if (dewPointC < 19) return 'A little humid';
  if (dewPointC < 22) return 'Humid';
  return 'Oppressive';
}

export function describeVisibility(metres: number): string {
  if (metres >= 20_000) return 'Very clear';
  if (metres >= 10_000) return 'Clear';
  if (metres >= 4_000) return 'Hazy';
  if (metres >= 1_000) return 'Poor';
  return 'Fog-level';
}

export function describePressure(hpa: number): string {
  if (hpa < 1000) return 'Low, unsettled weather likely';
  if (hpa <= 1020) return 'Normal';
  return 'High, settled weather likely';
}
