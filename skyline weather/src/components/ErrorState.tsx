/**
 * The one way errors are shown. Takes an AppError, never a raw message, and
 * renders the friendly wording from describeError() plus a retry button when
 * retrying can help.
 */
import type { ReactNode } from 'react';
import { describeError, type AppError } from '../api/errors';
import { AlertIcon, OfflineIcon, RefreshIcon, Spinner } from './Icons';

interface Props {
  error: AppError;
  /** What failed to load: "the forecast", "air quality". */
  subject?: string;
  onRetry?: () => void;
  retrying?: boolean;
  /** Tighter layout for use inside a panel. */
  compact?: boolean;
  /** Extra actions, shown next to the retry button. */
  children?: ReactNode;
}

export function ErrorState({ error, subject, onRetry, retrying = false, compact = false, children }: Props) {
  const copy = describeError(error, subject);
  const Icon = error.kind === 'offline' || error.kind === 'network' ? OfflineIcon : AlertIcon;

  return (
    <div className={`error-state${compact ? ' error-state--compact' : ''}`} role="alert">
      <span className="error-state__icon">
        <Icon size={compact ? 22 : 30} />
      </span>
      <div className="error-state__body">
        <p className="error-state__title">{copy.title}</p>
        <p className="error-state__text">{copy.message}</p>
        {(copy.action && onRetry) || children ? (
          <div className="error-state__actions">
            {copy.action && onRetry ? (
              <button type="button" className="button" onClick={onRetry} disabled={retrying}>
                {retrying ? <Spinner size={16} /> : <RefreshIcon size={16} />}
                {retrying ? 'Trying again' : copy.action}
              </button>
            ) : null}
            {children}
          </div>
        ) : null}
      </div>
    </div>
  );
}
