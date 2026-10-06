import { logger } from "./logger.ts";
const log = logger("ledger");
/** Claims that keep their sources: required provenance fields, a merge that records agreement, change over time and conflict, a coverage note with gaps, and rendering by content type. See ../../statement.md. */

export const REQUIRED = ["claim", "value", "source", "date"];
export const KINDS = ["financial", "news", "technical"];

export function checkFinding(finding: Record<string, any>): string[] {
  return REQUIRED.filter((f) => String(finding[f] ?? "").trim() === "");
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
      if (!entry.sources.some((s) => s.source === f.source && s.date === f.date)) entry.sources.push({ source: f.source, date: f.date });
    }
    let status = "agreed";
    if (values.length > 1) {
      const same = group.some((a) => group.some((b) => a.value !== b.value && a.date === b.date));
      if (same) status = "conflict";
      else {
        status = "changed";
        const earliest = (v: { sources: Array<{ date: string }> }) => v.sources.map((s) => s.date).sort()[0];
        values.sort((a, b) => (earliest(a) < earliest(b) ? -1 : earliest(a) > earliest(b) ? 1 : 0));
      }
    }
    return { claim, status, values };
  });
}

export function coverageNote(planned: string[], merged: any[], unavailable: Record<string, string>): any {
  const note: any = { well_supported: [], single_source: [], changed: [], contested: [], gaps: [] };
  for (const e of merged) {
    if (e.status === "conflict") note.contested.push(e.claim);
    else if (e.status === "changed") note.changed.push(e.claim);
    else if (new Set(e.values[0].sources.map((s: any) => s.source)).size >= 2) note.well_supported.push(e.claim);
    else note.single_source.push(e.claim);
  }
  const have = new Set(merged.map((e) => e.claim));
  note.gaps = planned.filter((c) => !have.has(c)).map((c) => ({ claim: c, reason: unavailable[c] ?? "no source found" }));
  return note;
}

export function render(entry: any, kind: string): string {
  if (!KINDS.includes(kind)) throw new Error(`unknown content type ${kind}`);
  const rows: Array<[string, string, string]> = entry.values.flatMap((v: any) => v.sources.map((s: any) => [v.value, s.source, s.date]));
  if (kind === "financial") return ["| Source | Date | Value |", "|---|---|---|", ...rows.map(([v, s, d]) => `| ${s} | ${d} | ${v} |`)].join("\n");
  if (kind === "technical") return [`${entry.claim}:`, ...rows.map(([v, s, d]) => `- ${v} (${s}, ${d})`)].join("\n");
  let text = `${entry.claim}: ` + rows.map(([v, s, d]) => `${v} (${s}, ${d})`).join("; ") + ".";
  if (entry.status === "conflict") text += " The sources disagree.";
  else if (entry.status === "changed") text += " The figures are from different dates.";
  return text;
}
