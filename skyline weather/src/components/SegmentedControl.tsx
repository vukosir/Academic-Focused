/**
 * A two-or-more-way switch built on native radio buttons, so arrow keys,
 * focus and screen reader announcements all work without extra code.
 */
import { useId } from 'react';

interface Option<T extends string> {
  value: T;
  /** What is shown. */
  label: string;
  /** Read out instead of the label when the label is an abbreviation. */
  spoken?: string;
}

interface Props<T extends string> {
  legend: string;
  options: Option<T>[];
  value: T;
  onChange: (value: T) => void;
}

export function SegmentedControl<T extends string>({ legend, options, value, onChange }: Props<T>) {
  const name = useId();
  return (
    <fieldset className="segmented">
      <legend className="visually-hidden">{legend}</legend>
      {options.map((option) => (
        <label key={option.value} className="segmented__option">
          <input
            type="radio"
            name={name}
            value={option.value}
            checked={option.value === value}
            onChange={() => onChange(option.value)}
            aria-label={option.spoken}
          />
          <span aria-hidden={option.spoken ? true : undefined}>{option.label}</span>
        </label>
      ))}
    </fieldset>
  );
}
