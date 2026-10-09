/**
 * Recorded Open-Meteo responses, used only by the test suite.
 *
 * These were captured from the live API for Pretoria on 2026-10-09 and trimmed
 * to 48 hourly values. The running app never imports this file.
 */

const hours = (day: string) => Array.from({ length: 24 }, (_, h) => `${day}T${String(h).padStart(2, '0')}:00`);

export const forecastResponse = {
  latitude: -25.7645,
  longitude: 28.235294,
  generationtime_ms: 325.1678943634033,
  utc_offset_seconds: 7200,
  timezone: 'Africa/Johannesburg',
  timezone_abbreviation: 'GMT+2',
  elevation: 1321,
  current: {
    time: '2026-10-09T19:00',
    interval: 900,
    temperature_2m: 25.6,
    relative_humidity_2m: 39,
    apparent_temperature: 24.9,
    is_day: 0,
    precipitation: 0,
    weather_code: 2,
    cloud_cover: 50,
    pressure_msl: 1018.7,
    wind_speed_10m: 6.1,
    wind_direction_10m: 251,
    wind_gusts_10m: 15.5,
  },
  hourly: {
    time: [...hours('2026-10-09'), ...hours('2026-10-10')],
    temperature_2m: [
      19.6, 19.2, 18.3, 18.2, 17.8, 17.4, 17.4, 19.5, 22.5, 24.8, 26.6, 28.1, 28.9, 29.1, 29.7, 30.1, 29.9, 29.1, 27.3, 25.6,
      24.5, 23.4, 22.5, 22.3, 22.1, 21.5, 20.9, 19.7, 19.1, 19.2, 19.2, 21.3, 24.2, 26.5, 28.3, 29.3, 30, 30.8, 31, 31.1, 30.4,
      29.8, 28.3, 26.4, 23.3, 22.5, 21.6, 21.1,
    ],
    apparent_temperature: [
      19.4, 19, 19, 18.7, 18.3, 17.8, 17.6, 20, 22.9, 25, 27.6, 29.8, 30.1, 29.5, 29.9, 29.4, 28.5, 27.8, 26.5, 24.9, 23.8, 22.8,
      21.8, 20.9, 20.7, 20, 19.6, 18.8, 17.9, 17.8, 18, 20, 23, 25.6, 28, 29.2, 30.2, 31.4, 30.9, 29.8, 28, 27.4, 26, 24.8, 23.1,
      23.7, 22.7, 21.1,
    ],
    precipitation_probability: [
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 5, 6, 7, 7, 6, 4, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 4,
      13, 25, 35, 43, 49, 49,
    ],
    precipitation: [
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0.1, 0.3, 0, 0,
    ],
    weather_code: [
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1,
      2, 2, 51, 51, 1, 0,
    ],
    visibility: [
      30840, 30880, 30900, 30980, 28320, 28240, 28100, 28000, 33040, 36900, 36980, 39080, 39160, 36780, 39460, 39540, 39580,
      39640, 39600, 39460, 37300, 37200, 37200, 37260, 37360, 37460, 37540, 33580, 33540, 33460, 33300, 33160, 36940, 38960,
      39040, 39160, 39340, 39500, 39720, 39900, 39980, 39880, 39760, 39520, 6940, 9960, 30680, 33140,
    ],
    dew_point_2m: [
      12.5, 12.5, 13.8, 13.7, 13.8, 13.9, 14, 14.6, 14.1, 13.1, 12.3, 11.4, 9.9, 9.8, 9.3, 8.9, 8.5, 8.4, 9.6, 10.8, 11.9, 11.9,
      11.7, 10.7, 10.4, 10.6, 10.6, 10.8, 10.8, 10.6, 10.4, 10.9, 10.7, 10.9, 10.4, 10.1, 9.9, 9.7, 9.3, 8.4, 7.8, 7.3, 7, 7.9,
      13.2, 15.1, 15, 13.1,
    ],
    uv_index: [
      0, 0, 0, 0, 0, 0, 0, 0.75, 2.45, 4.6, 6.6, 8.05, 8.85, 8.75, 7.85, 6.25, 4.2, 2, 0.5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.05,
      0.75, 2.45, 4.6, 6.6, 8.1, 8.85, 8.8, 7.85, 2.3, 1.4, 2.1, 0.5, 0, 0, 0, 0, 0,
    ],
    is_day: [
      0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
      1, 0, 0, 0, 0, 0,
    ],
    wind_speed_10m: [
      6.4, 6, 3.6, 4.3, 4.8, 5.8, 6.9, 7.1, 6.2, 6.8, 7.3, 7, 7, 8.4, 7.9, 7.2, 6.4, 5.6, 5, 6.1, 8.9, 7.9, 8.5, 11, 10.8, 11.2,
      10.5, 7.6, 10.1, 10.6, 9.4, 10.8, 9.5, 9.6, 11.7, 14, 13.6, 10.9, 9.7, 11.6, 12.1, 11.8, 10.5, 6.8, 8, 3.1, 3.2, 6.3,
    ],
  },
  daily: {
    time: ['2026-10-09', '2026-10-10', '2026-10-11', '2026-10-12', '2026-10-13', '2026-10-14', '2026-10-15'],
    weather_code: [2, 51, 51, 3, 2, 3, 2],
    temperature_2m_max: [30.1, 31.1, 28.8, 30.6, 31.8, 32.1, 31.9],
    temperature_2m_min: [17.4, 19.1, 17.3, 16, 17, 19.7, 18.1],
    sunrise: [
      '2026-10-09T05:38',
      '2026-10-10T05:37',
      '2026-10-11T05:36',
      '2026-10-12T05:35',
      '2026-10-13T05:34',
      '2026-10-14T05:33',
      '2026-10-15T05:32',
    ],
    sunset: [
      '2026-10-09T18:10',
      '2026-10-10T18:10',
      '2026-10-11T18:11',
      '2026-10-12T18:11',
      '2026-10-13T18:12',
      '2026-10-14T18:12',
      '2026-10-15T18:13',
    ],
    uv_index_max: [8.85, 8.85, 8.85, 8.9, 8.7, 7.45, 9.2],
    precipitation_sum: [0, 0.4, 0.2, 0, 0, 0, 0],
    precipitation_probability_max: [7, 49, 39, 0, 0, 6, 15],
    wind_speed_10m_max: [11, 14, 8.8, 11.8, 14.1, 19.2, 11.9],
  },
};

export const airQualityResponse = {
  latitude: -25.699997,
  longitude: 28.199997,
  utc_offset_seconds: 7200,
  timezone: 'Africa/Johannesburg',
  current: {
    time: '2026-10-09T19:00',
    interval: 3600,
    us_aqi: 86,
    european_aqi: 44,
    pm2_5: 21.9,
    pm10: 22.2,
    ozone: 94,
    nitrogen_dioxide: 21.7,
    sulphur_dioxide: 29.7,
    carbon_monoxide: 300,
  },
};

export const geocodingResponse = {
  results: [
    {
      id: 964137,
      name: 'Pretoria',
      latitude: -25.74486,
      longitude: 28.18783,
      elevation: 1332,
      feature_code: 'PPLC',
      country_code: 'ZA',
      timezone: 'Africa/Johannesburg',
      population: 2112693,
      country: 'South Africa',
      admin1: 'Gauteng',
      admin2: 'City of Tshwane Metropolitan Municipality',
    },
    {
      id: 8864870,
      name: 'Pretoria',
      latitude: 19.78722,
      longitude: -101.62083,
      elevation: 2040,
      feature_code: 'PPL',
      country_code: 'MX',
      timezone: 'America/Mexico_City',
      population: 406,
      country: 'Mexico',
      admin1: 'Michoacán',
    },
  ],
  generationtime_ms: 0.39720535,
};

/** What the geocoding API returns when nothing matches: no `results` key at all. */
export const geocodingEmptyResponse = { generationtime_ms: 0.54228306 };

/** HTTP 400 body for an invalid parameter. */
export const badRequestResponse = { reason: 'Latitude must be in range of -90 to 90°. Given: 999.0.', error: true };

export const reverseGeocodingResponse = {
  latitude: -25.7449,
  longitude: 28.1878,
  countryName: 'South Africa',
  countryCode: 'ZA',
  principalSubdivision: 'Gauteng',
  city: 'Pretoria',
  locality: 'Pretoria',
};

/** Minimal Response stand-in for tests that mock fetch. */
export function jsonResponse(body: unknown, init: { status?: number; headers?: Record<string, string> } = {}): Response {
  return new Response(typeof body === 'string' ? body : JSON.stringify(body), {
    status: init.status ?? 200,
    headers: { 'Content-Type': 'application/json', ...init.headers },
  });
}
