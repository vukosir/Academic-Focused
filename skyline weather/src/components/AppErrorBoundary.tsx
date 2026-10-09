/**
 * Last line of defence. Panel-level boundaries catch almost everything; if an
 * error still reaches the top, the user gets a readable page with two ways
 * out instead of a blank screen.
 */
import { Component, type ErrorInfo, type ReactNode } from 'react';

interface State {
  failed: boolean;
}

export class AppErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { failed: false };

  static getDerivedStateFromError(): State {
    return { failed: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('Skyline Weather stopped unexpectedly', error, info.componentStack);
  }

  /** Saved settings are the most likely cause of a crash on every load, so offer to clear them. */
  private resetAndReload = () => {
    try {
      Object.keys(window.localStorage)
        .filter((key) => key.startsWith('skyline:'))
        .forEach((key) => window.localStorage.removeItem(key));
    } catch {
      // Storage unavailable: reloading is still worth trying.
    }
    window.location.reload();
  };

  render(): ReactNode {
    if (!this.state.failed) return this.props.children;
    return (
      <div className="app app--crashed" data-scene="idle">
        <main className="crash" role="alert">
          <h1>Skyline Weather ran into a problem</h1>
          <p>Reloading usually fixes it. If it keeps happening, reset the app to clear your saved places and settings.</p>
          <div className="error-state__actions">
            <button type="button" className="button button--primary" onClick={() => window.location.reload()}>
              Reload
            </button>
            <button type="button" className="button" onClick={this.resetAndReload}>
              Reset and reload
            </button>
          </div>
        </main>
      </div>
    );
  }
}
