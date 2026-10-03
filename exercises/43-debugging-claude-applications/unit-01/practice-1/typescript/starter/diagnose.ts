// Diagnose a failure from a trace. See ../../statement.md.
export type Event = Record<string, any>;
export type Diagnosis = { index: number; type: string; origin: string; recovery: string; recovered: boolean };

/** The first failure in the trace: its index, type, origin, recovery, and whether a later response recovered. */
export function diagnose(_trace: Event[]): Diagnosis {
  return { index: -1, type: "ok", origin: "none", recovery: "none", recovered: false };
}
