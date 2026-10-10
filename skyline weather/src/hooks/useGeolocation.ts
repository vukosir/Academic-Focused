/**
 * "Use my location". Wraps the browser geolocation API in a promise and turns
 * each way it can fail into a message that tells the user what to do next.
 */
import { useCallback, useState } from 'react';
import type { Coordinates } from '../api/geocoding';

export type GeolocationFailure = 'unsupported' | 'insecure' | 'denied' | 'unavailable' | 'timeout';

export class GeolocationError extends Error {
  constructor(readonly reason: GeolocationFailure) {
    super(reason);
    this.name = 'GeolocationError';
  }
}

export const GEOLOCATION_MESSAGES: Record<GeolocationFailure, string> = {
  unsupported: 'This browser cannot share a location. Search for your city instead.',
  insecure: 'Location sharing only works on secure (https) pages. Search for your city instead.',
  denied:
    'Location access is blocked for this site. Search for your city, or allow location in the browser site settings and try again.',
  unavailable: 'Your device could not work out where it is. Search for your city instead.',
  timeout: 'Finding your location took too long. Try again, or search for your city.',
};

export function getCurrentCoordinates(timeoutMs = 10_000): Promise<Coordinates> {
  return new Promise((resolve, reject) => {
    if (typeof navigator === 'undefined' || !('geolocation' in navigator)) {
      reject(new GeolocationError('unsupported'));
      return;
    }
    if (typeof window !== 'undefined' && window.isSecureContext === false) {
      reject(new GeolocationError('insecure'));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude }),
      (error) => {
        // 1 = PERMISSION_DENIED, 2 = POSITION_UNAVAILABLE, 3 = TIMEOUT
        const reason: GeolocationFailure = error.code === 1 ? 'denied' : error.code === 3 ? 'timeout' : 'unavailable';
        reject(new GeolocationError(reason));
      },
      // City-level accuracy is enough for weather and is faster and kinder to batteries.
      { enableHighAccuracy: false, timeout: timeoutMs, maximumAge: 5 * 60_000 },
    );
  });
}

export interface GeolocationControls {
  locating: boolean;
  /** A ready-to-show message, or null. */
  error: string | null;
  locate: () => Promise<Coordinates | null>;
  clearError: () => void;
}

export function useGeolocation(): GeolocationControls {
  const [locating, setLocating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const locate = useCallback(async () => {
    setLocating(true);
    setError(null);
    try {
      return await getCurrentCoordinates();
    } catch (failure) {
      const reason = failure instanceof GeolocationError ? failure.reason : 'unavailable';
      setError(GEOLOCATION_MESSAGES[reason]);
      return null;
    } finally {
      setLocating(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);

  return { locating, error, locate, clearError };
}
