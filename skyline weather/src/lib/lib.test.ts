import { describe, expect, it } from 'vitest';
import { getMoonPhase, nextPhaseTime, phaseFraction } from './moon';
import { formatCoordinates, isPlace, placeLabel } from './place';
import { compassPoint, compassWords, describeUsAqi, describeUv } from './scales';
import { pickScene } from './scene';
import { daylightProgress, formatAge, formatDuration, minutesBetween, wallDate } from './time';
import { formatDistance, formatPrecipitation, formatPressure, formatSpeed, formatTemperature, PLACEHOLDER, shortTemperature } from './units';
import { describeWeather } from './weatherCodes';
import { normaliseForecast } from '../api/forecast';
import { forecastResponse } from '../test/fixtures';

describe('units', () => {
  it('converts temperature', () => {
    expect(formatTemperature(25.6, 'celsius')).toMatchObject({ value: '26', unit: '°C' });
    expect(formatTemperature(25.6, 'fahrenheit')).toMatchObject({ value: '78', unit: '°F' });
    expect(formatTemperature(-40, 'fahrenheit').value).toBe('-40');
  });

  it('never prints negative zero', () => {
    expect(shortTemperature(-0.2, 'celsius')).toBe('0°');
  });

  it('converts wind, pressure, distance and rainfall for imperial units', () => {
    expect(formatSpeed(100, 'imperial')).toMatchObject({ value: '62', unit: 'mph' });
    expect(formatPressure(1013.25, 'imperial')).toMatchObject({ value: '29.92', unit: 'inHg' });
    expect(formatDistance(16093.44, 'imperial')).toMatchObject({ value: '10', unit: 'mi' });
    expect(formatPrecipitation(25.4, 'imperial')).toMatchObject({ value: '1.00', unit: 'in' });
  });

  it('keeps metric values as they are', () => {
    expect(formatSpeed(6.1, 'metric')).toMatchObject({ value: '6', unit: 'km/h' });
    expect(formatDistance(39460, 'metric')).toMatchObject({ value: '39', unit: 'km' });
    expect(formatDistance(800, 'metric').value).toBe('0.8');
  });

  it('shows a placeholder for missing values', () => {
    expect(formatTemperature(null, 'celsius').value).toBe(PLACEHOLDER);
    expect(formatSpeed(null, 'metric').spoken).toBe('not available');
    expect(shortTemperature(null, 'celsius')).toBe(PLACEHOLDER);
  });
});

describe('time', () => {
  it('measures and formats durations between local times', () => {
    expect(minutesBetween('2026-10-09T05:38', '2026-10-09T18:10')).toBe(752);
    expect(formatDuration(752)).toBe('12 h 32 min');
    expect(formatDuration(45)).toBe('45 min');
    expect(formatDuration(null)).toBe('--');
    expect(minutesBetween(null, '2026-10-09T18:10')).toBeNull();
  });

  it('places the sun between sunrise and sunset', () => {
    const noon = wallDate('2026-10-09T11:54');
    expect(daylightProgress('2026-10-09T05:38', '2026-10-09T18:10', noon)).toBeCloseTo(0.5, 2);
    expect(daylightProgress('2026-10-09T05:38', '2026-10-09T18:10', wallDate('2026-10-09T21:00'))).toBeNull();
    expect(daylightProgress(null, '2026-10-09T18:10', noon)).toBeNull();
  });

  it('describes how old data is', () => {
    expect(formatAge(1_000_000, 1_000_000 + 20_000)).toBe('just now');
    expect(formatAge(0, 5 * 60_000)).toBe('5 min ago');
    expect(formatAge(0, 3 * 3_600_000)).toBe('3 h ago');
  });
});

describe('weather codes and scenes', () => {
  it('describes known codes and falls back for unknown ones', () => {
    expect(describeWeather(0, true)).toMatchObject({ label: 'Clear sky', icon: 'clear-day' });
    expect(describeWeather(0, false).icon).toBe('clear-night');
    expect(describeWeather(95).group).toBe('thunder');
    expect(describeWeather(1234).icon).toBe('unknown');
    expect(describeWeather(null).label).toBe('Conditions unavailable');
  });

  it('picks a scene from the conditions', () => {
    const base = normaliseForecast(forecastResponse);
    const at = (code: number, isDay: boolean) => ({ ...base, current: { ...base.current, weatherCode: code, isDay } });
    // 21:00 local time, well after sunset.
    const night = Date.parse('2026-10-09T19:00:00Z');
    expect(pickScene(null)).toBe('idle');
    expect(pickScene(at(0, false), night)).toBe('clear-night');
    expect(pickScene(at(3, false), night)).toBe('cloudy-night');
    expect(pickScene(at(63, true), night)).toBe('rain');
    expect(pickScene(at(73, true), night)).toBe('snow');
    expect(pickScene(at(95, true), night)).toBe('thunder');
    expect(pickScene(at(45, true), night)).toBe('fog');
    // 12:00 local time.
    expect(pickScene(at(0, true), Date.parse('2026-10-09T10:00:00Z'))).toBe('clear-day');
    // 18:00 local time, ten minutes before sunset.
    expect(pickScene(at(0, true), Date.parse('2026-10-09T16:00:00Z'))).toBe('golden');
  });
});

describe('scales', () => {
  it('bands the US air quality index', () => {
    expect(describeUsAqi(42).label).toBe('Good');
    expect(describeUsAqi(86).label).toBe('Moderate');
    expect(describeUsAqi(151).level).toBe('high');
    expect(describeUsAqi(400).label).toBe('Hazardous');
  });

  it('bands the UV index', () => {
    expect(describeUv(1).label).toBe('Low');
    expect(describeUv(7.8).label).toBe('High');
    expect(describeUv(12).label).toBe('Extreme');
  });

  it('names compass points', () => {
    expect(compassPoint(0)).toBe('N');
    expect(compassPoint(251)).toBe('WSW');
    expect(compassPoint(359)).toBe('N');
    expect(compassPoint(-90)).toBe('W');
    expect(compassWords('WSW')).toBe('west-southwest');
    expect(compassWords('NE')).toBe('northeast');
  });
});

describe('moon', () => {
  // Published phase times for April 2024 (UTC).
  it('matches known phases', () => {
    expect(phaseFraction(Date.parse('2024-04-15T19:13:00Z'))).toBeCloseTo(0.25, 2);
    expect(phaseFraction(Date.parse('2024-04-23T23:49:00Z'))).toBeCloseTo(0.5, 2);
    expect(getMoonPhase(Date.parse('2024-04-23T23:49:00Z'))).toMatchObject({ name: 'Full moon' });
    expect(getMoonPhase(Date.parse('2024-04-08T18:21:00Z')).illumination).toBeLessThan(0.01);
    expect(getMoonPhase(Date.parse('2024-04-12T00:00:00Z'))).toMatchObject({ name: 'Waxing crescent', waxing: true });
  });

  it('finds the next full moon to within two hours', () => {
    const predicted = nextPhaseTime(0.5, Date.parse('2024-04-01T00:00:00Z'));
    expect(Math.abs(predicted - Date.parse('2024-04-23T23:49:00Z'))).toBeLessThan(2 * 3_600_000);
  });
});

describe('place helpers', () => {
  it('formats labels and coordinates', () => {
    expect(placeLabel({ id: 'x', name: 'Pretoria', region: 'Gauteng', country: 'South Africa', latitude: 0, longitude: 0 })).toBe(
      'Pretoria, Gauteng, South Africa',
    );
    expect(placeLabel({ id: 'x', name: 'Singapore', country: 'Singapore', latitude: 0, longitude: 0 })).toBe('Singapore');
    expect(formatCoordinates(-25.7449, 28.1878)).toBe('25.745°S, 28.188°E');
  });

  it('validates places read from storage', () => {
    expect(isPlace({ id: 'a', name: 'A', latitude: 1, longitude: 2 })).toBe(true);
    expect(isPlace({ id: 'a', name: 'A', latitude: 100, longitude: 2 })).toBe(false);
    expect(isPlace({ name: 'A' })).toBe(false);
    expect(isPlace(null)).toBe(false);
  });
});
