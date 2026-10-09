import { useCallback } from 'react';
import { getAirQuality } from '../api/airQuality';
import { getForecast } from '../api/forecast';
import { config } from '../config';
import type { AirQuality, Forecast, Place } from '../types';
import { useResource, type Resource } from './useResource';

/** Forecast for the selected place, refreshed in the background. */
export function useForecast(place: Place | null): Resource<Forecast> {
  const loader = useCallback(
    (options: { force: boolean }) => {
      if (!place) return Promise.reject(new Error('No place selected'));
      return getForecast(place, options);
    },
    [place],
  );
  return useResource(place?.id ?? null, loader, { refreshIntervalMs: config.forecastTtlMs });
}

/** Air quality for the selected place. Independent of the forecast request. */
export function useAirQuality(place: Place | null): Resource<AirQuality> {
  const loader = useCallback(
    (options: { force: boolean }) => {
      if (!place) return Promise.reject(new Error('No place selected'));
      return getAirQuality(place, options);
    },
    [place],
  );
  return useResource(place?.id ?? null, loader, { refreshIntervalMs: config.airQualityTtlMs });
}
