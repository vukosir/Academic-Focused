/**
 * The scene is the mood of the page: the sky gradient behind the current
 * conditions. It is chosen from the weather group and whether it is day,
 * night, or close to sunrise or sunset.
 */
import type { Forecast } from '../types';
import { daylightProgress, wallNow } from './time';
import { describeWeather } from './weatherCodes';

export type Scene =
  | 'clear-day'
  | 'clear-night'
  | 'golden' // clear or lightly clouded sky near sunrise or sunset
  | 'cloudy-day'
  | 'cloudy-night'
  | 'fog'
  | 'rain'
  | 'snow'
  | 'thunder'
  | 'idle'; // nothing loaded yet

/** Fraction of the daylight period, at each end, that counts as golden hour. */
const GOLDEN_EDGE = 0.08;

export function pickScene(forecast: Forecast | null | undefined, now: number = Date.now()): Scene {
  if (!forecast) return 'idle';
  const { current, daily, utcOffsetSeconds } = forecast;
  const { group } = describeWeather(current.weatherCode, current.isDay);

  if (group === 'thunder') return 'thunder';
  if (group === 'snow') return 'snow';
  if (group === 'rain') return 'rain';
  if (group === 'fog') return 'fog';

  const today = daily[0];
  const progress = today ? daylightProgress(today.sunrise, today.sunset, wallNow(utcOffsetSeconds, now)) : null;
  const nearEdge = progress !== null && (progress < GOLDEN_EDGE || progress > 1 - GOLDEN_EDGE);

  if (group === 'cloudy') {
    // "Partly cloudy" still shows plenty of sky, so it can take the golden scene.
    if (nearEdge && current.weatherCode === 2) return 'golden';
    return current.isDay ? 'cloudy-day' : 'cloudy-night';
  }
  if (nearEdge) return 'golden';
  return current.isDay ? 'clear-day' : 'clear-night';
}
