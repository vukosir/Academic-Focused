/**
 * Air quality has its own request, so it also has its own loading, error and
 * empty states. Whatever happens here, the rest of the dashboard is unaffected.
 */
import type { CSSProperties } from 'react';
import type { Resource } from '../hooks/useResource';
import { AQI_SCALE_MAX, describeUsAqi } from '../lib/scales';
import type { AirQuality } from '../types';
import { ErrorState } from './ErrorState';
import { PanelSkeleton } from './Skeletons';

const POLLUTANTS: { key: keyof AirQuality; label: string; name: string }[] = [
  { key: 'pm25', label: 'PM2.5', name: 'Fine particles' },
  { key: 'pm10', label: 'PM10', name: 'Coarse particles' },
  { key: 'ozone', label: 'O₃', name: 'Ozone' },
  { key: 'nitrogenDioxide', label: 'NO₂', name: 'Nitrogen dioxide' },
  { key: 'sulphurDioxide', label: 'SO₂', name: 'Sulphur dioxide' },
  { key: 'carbonMonoxide', label: 'CO', name: 'Carbon monoxide' },
];

/** Band edges of the US AQI scale, as fractions of the meter. */
const BANDS = [50, 100, 150, 200, 300].map((edge) => edge / AQI_SCALE_MAX);

export function AirQualityPanel({ resource }: { resource: Resource<AirQuality> }) {
  const { state, isRefreshing, refresh } = resource;

  if (state.status === 'idle' || state.status === 'loading') {
    return <PanelSkeleton rows={5} label="Loading air quality" />;
  }

  if (state.status === 'error') {
    // "empty" means the model simply has no coverage here. That is not a failure to retry.
    if (state.error.kind === 'empty') {
      return <p className="panel__empty">Air quality is not measured for this place.</p>;
    }
    return <ErrorState error={state.error} subject="air quality" onRetry={refresh} retrying={isRefreshing} compact />;
  }

  const data = state.data;
  const aqi = data.usAqi;
  const reading = aqi === null ? null : describeUsAqi(aqi);

  return (
    <div className="aqi">
      {aqi !== null && reading ? (
        <>
          <p className="aqi__headline">
            <span className="aqi__value">{Math.round(aqi)}</span>
            <span className="aqi__words">
              <span className="aqi__label" data-level={reading.level}>
                <span className="aqi__swatch" aria-hidden="true" />
                {reading.label}
              </span>
              <span className="aqi__scale-name">US air quality index</span>
            </span>
          </p>
          <div className="aqi__meter" aria-hidden="true" data-level={reading.level}>
            {BANDS.map((edge) => (
              <span key={edge} className="aqi__tick" style={{ '--at': edge } as CSSProperties} />
            ))}
            <span className="aqi__marker" style={{ '--at': Math.min(1, aqi / AQI_SCALE_MAX) } as CSSProperties} />
          </div>
          <p className="aqi__advice">{reading.advice}</p>
        </>
      ) : (
        <p className="panel__empty">The overall index is not available here, but some pollutant readings are.</p>
      )}

      <dl className="aqi__pollutants">
        {POLLUTANTS.map(({ key, label, name }) => {
          const value = data[key];
          return (
            <div key={key}>
              <dt>
                <abbr title={name}>{label}</abbr>
              </dt>
              <dd>{typeof value === 'number' ? value.toFixed(value < 10 ? 1 : 0) : '--'}</dd>
            </div>
          );
        })}
      </dl>
      <p className="aqi__footnote">Pollutants in micrograms per cubic metre.</p>

      {state.staleError ? (
        <p className="panel__note" role="status">
          Showing an earlier reading. The latest one could not be loaded.
        </p>
      ) : null}
    </div>
  );
}
