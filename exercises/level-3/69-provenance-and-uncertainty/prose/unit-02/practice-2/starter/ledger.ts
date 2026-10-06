import { logger } from "./logger.ts";
const log = logger("ledger");
/** Claims that keep their sources: required provenance fields, a merge that records agreement, change over time and conflict, a coverage note with gaps, and rendering by content type. See ../../statement.md. */

export const REQUIRED = ["claim", "value", "source", "date"];
export const KINDS = ["financial", "news", "technical"];

export function checkFinding(finding: Record<string, any>): string[] {
  // TODO 1 of 8 (finish this to pass e1): the provenance check. Receives one finding. Return the names, in REQUIRED
  //   order, of the fields that are absent, empty or only white space. Example: source "" and date " " -> [source, date].
  return REQUIRED.filter((f) => !(f in finding));
}

export function merge(findings: any[]): any[] {
  log.debug("merge input", findings);
  findings.forEach((f, i) => {
    const missing = checkFinding(f);
    if (missing.length > 0) throw new Error(`finding ${i} is missing ${missing.join(", ")}`);
  });
  const claims: string[] = [];
  for (const f of findings) if (!claims.includes(f.claim)) claims.push(f.claim);
  return claims.map((claim) => {
    const group = findings.filter((f) => f.claim === claim);
    const values: Array<{ value: string; sources: Array<{ source: string; date: string }> }> = [];
    for (const f of group) {
      let entry = values.find((v) => v.value === f.value);
      if (!entry) {
        entry = { value: f.value, sources: [] };
        values.push(entry);
      }
      // TODO 2 of 8 (finish this to pass m1): the sources of a value. When a finding repeats a source and date pair that
      //   the value already holds, do not add it again; otherwise add the pair last. Example: Annual report 2024-02-01
      //   reported twice -> kept once.
      entry.sources.push({ source: f.source, date: f.date });
    }
    let status = "agreed";
    // TODO 3 of 8 (finish this to pass e2, e3): the status of a claim. Receives the group of findings and its distinct
    //   values. One value -> agreed. Several values where two different values share a date -> conflict (keep all).
    //   Several values with no shared date -> changed, with the values ordered by their earliest date. Example: 12% and 9%
    //   both on 2024-05-01 -> conflict.
    return { claim, status, values };
  });
}

export function coverageNote(planned: string[], merged: any[], unavailable: Record<string, string>): any {
  const note: any = { well_supported: [], single_source: [], changed: [], contested: [], gaps: [] };
  for (const e of merged) {
    if (e.status === "conflict") note.contested.push(e.claim);
    else if (e.status === "changed") note.changed.push(e.claim);
    // TODO 4 of 8 (finish this to pass e4): the split of the agreed claims. For an agreed claim, add it to
    //   well_supported when its value comes from at least two different sources, otherwise to single_source. Example: two
    //   sources -> well_supported; one -> single_source.
    else note.single_source.push(e.claim);
  }
  const have = new Set(merged.map((e) => e.claim));
  // TODO 5 of 8 (finish this to pass e5): the gaps. Receives the planned claims, the merged entries and the reasons for
  //   unavailable claims. Return one {claim, reason} for each planned claim that has no merged entry, with the reason from
  //   `unavailable` or "no source found". Example: planned [a, b], merged a -> gap b.
  note.gaps = [];
  return note;
}

export function render(entry: any, kind: string): string {
  if (!KINDS.includes(kind)) throw new Error(`unknown content type ${kind}`);
  const rows: Array<[string, string, string]> = entry.values.flatMap((v: any) => v.sources.map((s: any) => [v.value, s.source, s.date]));
  // TODO 6 of 8 (finish this to pass e6): the financial rendering. Return a Markdown table: the header "| Source | Date
  //   | Value |", the line "|---|---|---|" and one row "| source | date | value |" for each source of each value. Example:
  //   one source -> three lines.
  // TODO 7 of 8 (finish this to pass e8): the technical rendering. Return the claim followed by a colon, then one line
  //   "- value (source, date)" for each source of each value. Example: one source -> two lines.
  let text = `${entry.claim}: ` + rows.map(([v, s, d]) => `${v} (${s}, ${d})`).join("; ") + ".";
  // TODO 8 of 8 (finish this to pass e7): the news ending. After the prose, add " The sources disagree." for a conflict
  //   and " The figures are from different dates." for a changed claim. Example: conflict -> the prose ends with that
  //   sentence.
  return text;
}
