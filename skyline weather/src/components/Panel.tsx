import { useId, type ReactNode } from 'react';
import { WidgetBoundary } from './WidgetBoundary';

interface Props {
  title: string;
  /** Used in the error boundary message, for example "the hourly forecast". */
  name: string;
  /** Small text or controls shown to the right of the title. */
  aside?: ReactNode;
  className?: string;
  resetKey?: string | null;
  children: ReactNode;
}

/** A titled section of the dashboard, isolated by its own error boundary. */
export function Panel({ title, name, aside, className = '', resetKey, children }: Props) {
  const headingId = useId();
  return (
    <section className={`panel ${className}`.trim()} aria-labelledby={headingId}>
      <header className="panel__head">
        <h2 id={headingId} className="panel__title">
          {title}
        </h2>
        {aside ? <div className="panel__aside">{aside}</div> : null}
      </header>
      <WidgetBoundary name={name} resetKey={resetKey}>
        {children}
      </WidgetBoundary>
    </section>
  );
}
