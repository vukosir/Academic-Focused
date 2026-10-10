import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AppError } from '../api/errors';
import { ErrorState } from './ErrorState';
import { WidgetBoundary } from './WidgetBoundary';

function Broken({ fail }: { fail: boolean }): never | null {
  if (fail) throw new Error('TypeError: cannot read properties of undefined');
  return null;
}

describe('WidgetBoundary', () => {
  it('contains a crashing panel and leaves its neighbours alone', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    render(
      <>
        <WidgetBoundary name="the hourly forecast">
          <Broken fail />
        </WidgetBoundary>
        <p>Daily forecast still here</p>
      </>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('This panel could not be shown');
    expect(screen.getByText('Daily forecast still here')).toBeInTheDocument();
    // The raw error text stays out of the page.
    expect(document.body).not.toHaveTextContent('TypeError');
  });

  it('recovers when the user reloads the panel after the cause is gone', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    let fail = true;
    const Flaky = () => {
      if (fail) throw new Error('boom');
      return <p>Recovered</p>;
    };
    render(
      <WidgetBoundary name="air quality">
        <Flaky />
      </WidgetBoundary>,
    );
    fail = false;
    await userEvent.click(screen.getByRole('button', { name: 'Reload panel' }));
    expect(screen.getByText('Recovered')).toBeInTheDocument();
  });
});

describe('ErrorState', () => {
  it('shows a retry button for retryable errors and calls back', async () => {
    const onRetry = vi.fn();
    render(<ErrorState error={new AppError('timeout', 'internal')} onRetry={onRetry} />);
    expect(screen.getByText('The weather service took too long')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('shows no retry button when retrying cannot help', () => {
    render(<ErrorState error={new AppError('bad-request', 'internal')} onRetry={() => {}} />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
