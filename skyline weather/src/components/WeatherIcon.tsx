/**
 * Weather icons, drawn as inline SVG so they inherit theme colours and need
 * no network requests. Colours come from CSS variables (--icon-sun,
 * --icon-cloud, --icon-rain and so on) defined in styles/tokens.css.
 */
import type { ReactNode } from 'react';
import type { IconName } from '../lib/weatherCodes';

const CLOUD = 'M14 36a8 8 0 0 1-1.2-15.9A11 11 0 0 1 34.4 18 9.1 9.1 0 0 1 35 36Z';

const Cloud = ({ lifted = false }: { lifted?: boolean }) => (
  <path d={CLOUD} fill="var(--icon-cloud)" transform={lifted ? 'translate(0 -5)' : undefined} />
);

const Sun = ({ cx = 24, cy = 24, r = 8 }: { cx?: number; cy?: number; r?: number }) => {
  const rays = Array.from({ length: 8 }, (_, i) => {
    const angle = (i * Math.PI) / 4;
    const inner = r + 4;
    const outer = r + 8;
    return (
      <line
        key={i}
        x1={cx + Math.cos(angle) * inner}
        y1={cy + Math.sin(angle) * inner}
        x2={cx + Math.cos(angle) * outer}
        y2={cy + Math.sin(angle) * outer}
      />
    );
  });
  return (
    <g>
      <g stroke="var(--icon-sun)" strokeWidth="2.6" strokeLinecap="round">
        {rays}
      </g>
      <circle cx={cx} cy={cy} r={r} fill="var(--icon-sun)" />
    </g>
  );
};

/** A crescent, drawn as a disc with a second disc cut out of it. */
const Moon = ({ small = false }: { small?: boolean }) => (
  <path
    d="M28.5 8.5a15.5 15.5 0 1 0 11 24.2A12.4 12.4 0 0 1 28.5 8.5Z"
    fill="var(--icon-moon)"
    transform={small ? 'translate(19 -5) scale(.56)' : undefined}
  />
);

const Drops = ({ count, long = false }: { count: 2 | 3 | 4; long?: boolean }) => {
  const xs = count === 2 ? [19, 29] : count === 3 ? [16, 24, 32] : [14, 21, 28, 35];
  const length = long ? 7 : 5;
  return (
    <g stroke="var(--icon-rain)" strokeWidth="2.6" strokeLinecap="round">
      {xs.map((x) => (
        <line key={x} x1={x} y1={36} x2={x - length * 0.4} y2={36 + length} />
      ))}
    </g>
  );
};

const Flakes = ({ xs }: { xs: number[] }) => (
  <g fill="var(--icon-snow)">
    {xs.map((x, i) => (
      <circle key={x} cx={x} cy={i % 2 === 0 ? 37.5 : 41.5} r="2" />
    ))}
  </g>
);

const ICONS: Record<IconName, ReactNode> = {
  'clear-day': <Sun />,
  'clear-night': <Moon />,
  'partly-day': (
    <>
      <Sun cx={32} cy={16} r={6.5} />
      <Cloud />
    </>
  ),
  'partly-night': (
    <>
      <Moon small />
      <Cloud />
    </>
  ),
  cloudy: (
    <>
      <path d={CLOUD} fill="var(--icon-cloud-back)" transform="translate(9 -7) scale(.78)" />
      <Cloud />
    </>
  ),
  fog: (
    <>
      <Cloud lifted />
      <g stroke="var(--icon-cloud-back)" strokeWidth="2.6" strokeLinecap="round">
        <line x1="12" y1="36" x2="36" y2="36" />
        <line x1="16" y1="41.5" x2="32" y2="41.5" />
      </g>
    </>
  ),
  drizzle: (
    <>
      <Cloud lifted />
      <Drops count={2} />
    </>
  ),
  rain: (
    <>
      <Cloud lifted />
      <Drops count={3} />
    </>
  ),
  'heavy-rain': (
    <>
      <Cloud lifted />
      <Drops count={4} long />
    </>
  ),
  sleet: (
    <>
      <Cloud lifted />
      <g stroke="var(--icon-rain)" strokeWidth="2.6" strokeLinecap="round">
        <line x1="17" y1="36" x2="15" y2="41" />
        <line x1="33" y1="36" x2="31" y2="41" />
      </g>
      <circle cx="24" cy="39.5" r="2" fill="var(--icon-snow)" />
    </>
  ),
  snow: (
    <>
      <Cloud lifted />
      <Flakes xs={[16, 24, 32]} />
    </>
  ),
  thunder: (
    <>
      <Cloud lifted />
      <path d="M25.5 29 19 38.2h4.6L21.8 45l7.4-9.8h-4.8L27 29Z" fill="var(--icon-bolt)" />
    </>
  ),
  unknown: (
    <circle cx="24" cy="24" r="12" fill="none" stroke="var(--icon-cloud-back)" strokeWidth="2.6" strokeDasharray="3 6" strokeLinecap="round" />
  ),
};

interface Props {
  name: IconName;
  size?: number;
  /** Accessible name. Leave empty when a text label sits right next to the icon. */
  label?: string;
  className?: string;
}

export function WeatherIcon({ name, size = 32, label, className }: Props) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 48 48"
      role={label ? 'img' : undefined}
      aria-label={label || undefined}
      aria-hidden={label ? undefined : true}
      focusable="false"
    >
      {ICONS[name]}
    </svg>
  );
}
