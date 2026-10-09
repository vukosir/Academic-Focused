import { KEYS, type KeyDef } from '../engine/keys';

interface Props {
  shift: 'f' | 'g' | null;
  onPress: (id: string) => void;
}

function ariaLabel(key: KeyDef): string {
  const parts = [key.label];
  if (key.f?.label) parts.push(`f ${key.f.label}`);
  if (key.g?.label) parts.push(`g ${key.g.label}`);
  return parts.join(', ');
}

function Key({ def, shift, onPress, column, row }: { def: KeyDef; column: number; row: number } & Props) {
  const active = (def.id === 'f' && shift === 'f') || (def.id === 'g' && shift === 'g');
  const style = { gridColumn: column, gridRow: def.id === 'enter' ? '3 / span 2' : row };
  return (
    <div className={`key-cell${def.id === 'enter' ? ' key-tall' : ''}`} style={style}>
      <span className="lab-f" aria-hidden="true">
        {def.f?.label}
      </span>
      <button
        type="button"
        className={`key key-${def.kind}${active ? ' is-active' : ''}`}
        data-key={def.id}
        aria-label={ariaLabel(def)}
        aria-pressed={def.id === 'f' || def.id === 'g' ? active : undefined}
        onClick={() => onPress(def.id)}
      >
        {def.label}
        <span className="lab-g" aria-hidden="true">
          {def.g?.label}
        </span>
      </button>
    </div>
  );
}

export function Keypad({ shift, onPress }: Props) {
  return (
    <div className="keypad">
      {KEYS.map((row, r) =>
        row.map((def, c) => {
          // The bottom row skips column 6, which ENTER occupies from the row above.
          const column = r === 3 && c >= 5 ? c + 2 : c + 1;
          return <Key key={def.id} def={def} column={column} row={r + 1} shift={shift} onPress={onPress} />;
        }),
      )}
    </div>
  );
}

