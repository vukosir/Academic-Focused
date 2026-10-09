/**
 * Interface icons. 24 px grid, 1.8 px strokes, coloured by `currentColor`.
 * All are decorative: the control that holds one supplies the accessible name.
 */
import type { ReactNode, SVGProps } from 'react';

type Props = Omit<SVGProps<SVGSVGElement>, 'children'> & { size?: number };

function make(children: ReactNode, filled = false) {
  return function Icon({ size = 20, ...rest }: Props) {
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill={filled ? 'currentColor' : 'none'}
        stroke={filled ? 'none' : 'currentColor'}
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
        focusable="false"
        {...rest}
      >
        {children}
      </svg>
    );
  };
}

const STAR = 'M12 3.6l2.6 5.3 5.8.8-4.2 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.2-4.1 5.8-.8L12 3.6Z';

export const SearchIcon = make(
  <>
    <circle cx="11" cy="11" r="6.5" />
    <path d="m16 16 4.5 4.5" />
  </>,
);
export const CloseIcon = make(<path d="M6 6l12 12M18 6 6 18" />);
export const LocateIcon = make(
  <>
    <circle cx="12" cy="12" r="6.5" />
    <circle cx="12" cy="12" r="1.6" fill="currentColor" stroke="none" />
    <path d="M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3" />
  </>,
);
export const HomeIcon = make(<path d="M4 11.2 12 4.5l8 6.7M6.5 9.8V19.5h11V9.8M10 19.5v-5h4v5" />);
export const StarIcon = make(<path d={STAR} />);
export const StarFilledIcon = make(<path d={STAR} />, true);
export const SunIcon = make(
  <>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.3 5.3l1.8 1.8M16.9 16.9l1.8 1.8M5.3 18.7l1.8-1.8M16.9 7.1l1.8-1.8" />
  </>,
);
export const MoonIcon = make(<path d="M19.5 14.2A8 8 0 0 1 9.8 4.5a8 8 0 1 0 9.7 9.7Z" />);
export const RefreshIcon = make(
  <>
    <path d="M19.5 12a7.5 7.5 0 0 1-13.2 4.9M4.5 12a7.5 7.5 0 0 1 13.2-4.9" />
    <path d="M18.2 3.6v3.8h-3.8M5.8 20.4v-3.8h3.8" />
  </>,
);
export const ChevronLeftIcon = make(<path d="m14.5 6-6 6 6 6" />);
export const ChevronRightIcon = make(<path d="m9.5 6 6 6-6 6" />);
export const AlertIcon = make(
  <>
    <path d="M12 4 2.8 19.5h18.4L12 4Z" />
    <path d="M12 10v4.5M12 17.2v.1" />
  </>,
);
export const OfflineIcon = make(
  <>
    <path d="M3 9.5a13 13 0 0 1 4.2-2.7M21 9.5a13 13 0 0 0-8.2-3.4M6.3 13a8.4 8.4 0 0 1 3.4-2M17.7 13a8.4 8.4 0 0 0-2.2-1.5M9.5 16.4a3.8 3.8 0 0 1 5 0" />
    <path d="M12 19.6v.1M4 4l16 16" />
  </>,
);
export const HistoryIcon = make(
  <>
    <path d="M4.5 12a7.5 7.5 0 1 0 2.4-5.5" />
    <path d="M4.2 4.5v3.4h3.4M12 8v4.3l2.8 1.7" />
  </>,
);
export const PinIcon = make(
  <>
    <path d="M12 21s6.5-5.6 6.5-11a6.5 6.5 0 0 0-13 0c0 5.4 6.5 11 6.5 11Z" />
    <circle cx="12" cy="10" r="2.3" />
  </>,
);
export const DropIcon = make(<path d="M12 3.5s6 6.2 6 10.5a6 6 0 0 1-12 0C6 9.7 12 3.5 12 3.5Z" />);
/** Points up (north). Rotate it to show a bearing. */
export const ArrowIcon = make(<path d="M12 3.5 17.5 19 12 15.8 6.5 19 12 3.5Z" />, true);

/** Indeterminate progress ring. The rotation is defined in CSS. */
export function Spinner({ size = 18 }: { size?: number }) {
  return (
    <svg className="spinner" width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeOpacity=".25" strokeWidth="2.5" />
      <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
    </svg>
  );
}
