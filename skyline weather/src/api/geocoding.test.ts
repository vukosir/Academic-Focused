import { beforeEach, describe, expect, it, vi } from 'vitest';
import { geocodingEmptyResponse, geocodingResponse, jsonResponse, reverseGeocodingResponse } from '../test/fixtures';
import { clearGeocodingCache, normalisePlaces, parseCoordinates, resolveQuery, reverseGeocode, searchPlaces } from './geocoding';

function mockFetch(...responses: Response[]) {
  const fetchMock = vi.fn<typeof fetch>();
  responses.forEach((response) => fetchMock.mockResolvedValueOnce(response));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

beforeEach(() => clearGeocodingCache());

describe('parseCoordinates', () => {
  it.each([
    ['-25.75, 28.19', -25.75, 28.19],
    ['-25.75 28.19', -25.75, 28.19],
    ['25.75S 28.19E', -25.75, 28.19],
    ['40.71° N, 74.01° W', 40.71, -74.01],
    ['0,0', 0, 0],
  ])('reads %s', (input, latitude, longitude) => {
    expect(parseCoordinates(input)).toEqual({ latitude, longitude });
  });

  it.each(['Pretoria', '90210', '91, 10', '10, 181', '12.5', '10, 20, 30', ''])('rejects %j', (input) => {
    expect(parseCoordinates(input)).toBeNull();
  });
});

describe('normalisePlaces', () => {
  it('maps API results to places', () => {
    const [first] = normalisePlaces(geocodingResponse);
    expect(first).toMatchObject({ name: 'Pretoria', region: 'Gauteng', country: 'South Africa', countryCode: 'ZA', latitude: -25.74486 });
  });

  it('returns an empty list when the API omits "results"', () => {
    expect(normalisePlaces(geocodingEmptyResponse)).toEqual([]);
  });

  it('skips entries without a name or coordinates instead of failing', () => {
    const places = normalisePlaces({ results: [{ name: 'Nowhere' }, null, 'junk', { name: 'Somewhere', latitude: 1, longitude: 2 }] });
    expect(places.map((p) => p.name)).toEqual(['Somewhere']);
  });

  it('rejects a response that is not an object', () => {
    expect(() => normalisePlaces('nope')).toThrowError(expect.objectContaining({ kind: 'malformed' }));
  });
});

describe('searchPlaces', () => {
  it('does not call the API for very short input', async () => {
    const fetchMock = mockFetch();
    await expect(searchPlaces('p')).resolves.toEqual([]);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('sends the query and caches the answer', async () => {
    const fetchMock = mockFetch(jsonResponse(geocodingResponse));
    const first = await searchPlaces('Pretoria');
    const second = await searchPlaces('  pretoria ');
    expect(first).toHaveLength(2);
    expect(second).toEqual(first);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain('name=Pretoria');
  });
});

describe('resolveQuery', () => {
  it('returns the best match for a name', async () => {
    mockFetch(jsonResponse(geocodingResponse));
    await expect(resolveQuery('Pretoria')).resolves.toMatchObject({ name: 'Pretoria', countryCode: 'ZA' });
  });

  it('rejects with "not-found" when nothing matches', async () => {
    mockFetch(jsonResponse(geocodingEmptyResponse));
    await expect(resolveQuery('zzzxqqqjk')).rejects.toMatchObject({ kind: 'not-found' });
  });

  it('passes API failures through with their own kind', async () => {
    mockFetch(jsonResponse({}, { status: 429 }));
    await expect(resolveQuery('Pretoria')).rejects.toMatchObject({ kind: 'rate-limit' });
  });

  it('handles coordinates without the geocoding search', async () => {
    const fetchMock = mockFetch(jsonResponse(reverseGeocodingResponse));
    const place = await resolveQuery('-25.7449, 28.1878');
    expect(place).toMatchObject({ name: 'Pretoria', region: 'Gauteng', latitude: -25.7449, longitude: 28.1878 });
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain('bigdatacloud');
  });
});

describe('reverseGeocode', () => {
  it('labels the place with its coordinates when the lookup fails', async () => {
    mockFetch(jsonResponse({}, { status: 500 }));
    const place = await reverseGeocode({ latitude: -25.7449, longitude: 28.1878 });
    expect(place.name).toBe('25.74°S, 28.19°E');
    expect(place.latitude).toBe(-25.7449);
  });

  it('labels the place with its coordinates when the lookup has no locality', async () => {
    mockFetch(jsonResponse({ city: '', locality: '' }));
    const place = await reverseGeocode({ latitude: 0, longitude: -30 });
    expect(place.name).toBe('0.00°N, 30.00°W');
  });
});
