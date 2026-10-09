/**
 * Error boundary for a single panel.
 *
 * If rendering a panel throws (a bug, or data in a shape nobody expected),
 * only that panel is replaced by a short message. The rest of the dashboard
 * keeps working. The boundary resets itself when `resetKey` changes, for
 * example when the user picks another place.
 */
import { Component, type ErrorInfo, type ReactNode } from 'react';
import { AlertIcon } from './Icons';

interface Props {
  /** What the panel shows, used in the message: "the hourly forecast". */
  name: string;
  resetKey?: string | number | null;
  children: ReactNode;
}

interface State {
  failed: boolean;
}

export class WidgetBoundary extends Component<Props, State> {
  state: State = { failed: false };

  static getDerivedStateFromError(): State {
    return { failed: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    // Details go to the console for developers. Users only see the friendly message.
    console.error(`[${this.props.name}] failed to render`, error, info.componentStack);
  }

  componentDidUpdate(previous: Props): void {
    if (this.state.failed && previous.resetKey !== this.props.resetKey) {
      this.setState({ failed: false });
    }
  }

  private reset = () => this.setState({ failed: false });

  render(): ReactNode {
    if (!this.state.failed) return this.props.children;
    return (
      <div className="widget-error" role="alert">
        <AlertIcon size={22} />
        <div>
          <p className="widget-error__title">This panel could not be shown</p>
          <p className="widget-error__text">
            Something went wrong while drawing {this.props.name}. The rest of the page is unaffected.
          </p>
        </div>
        <button type="button" className="button button--quiet" onClick={this.reset}>
          Reload panel
        </button>
      </div>
    );
  }
}
