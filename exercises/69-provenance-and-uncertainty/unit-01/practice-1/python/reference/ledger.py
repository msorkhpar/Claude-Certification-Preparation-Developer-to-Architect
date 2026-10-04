"""Claims that keep their sources: required provenance fields, a merge that records agreement, change over time and conflict, a coverage note with gaps, and rendering by content type. See ../../statement.md."""

REQUIRED = ("claim", "value", "source", "date")
KINDS = ("financial", "news", "technical")


def check_finding(finding):
    return [f for f in REQUIRED if not str(finding.get(f, "")).strip()]


def merge(findings):
    for i, f in enumerate(findings):
        missing = check_finding(f)
        if missing:
            raise ValueError(f"finding {i} is missing {', '.join(missing)}")
    claims = []
    for f in findings:
        if f["claim"] not in claims:
            claims.append(f["claim"])
    out = []
    for claim in claims:
        group = [f for f in findings if f["claim"] == claim]
        values = []
        for f in group:
            entry = next((v for v in values if v["value"] == f["value"]), None)
            if entry is None:
                entry = {"value": f["value"], "sources": []}
                values.append(entry)
            pair = {"source": f["source"], "date": f["date"]}
            if pair not in entry["sources"]:
                entry["sources"].append(pair)
        if len(values) == 1:
            status = "agreed"
        elif any(a["value"] != b["value"] and a["date"] == b["date"] for a in group for b in group):
            status = "conflict"
        else:
            status = "changed"
            values.sort(key=lambda v: min(s["date"] for s in v["sources"]))
        out.append({"claim": claim, "status": status, "values": values})
    return out


def coverage_note(planned, merged, unavailable):
    note = {"well_supported": [], "single_source": [], "changed": [], "contested": [], "gaps": []}
    for e in merged:
        if e["status"] == "conflict":
            note["contested"].append(e["claim"])
        elif e["status"] == "changed":
            note["changed"].append(e["claim"])
        elif len({s["source"] for s in e["values"][0]["sources"]}) >= 2:
            note["well_supported"].append(e["claim"])
        else:
            note["single_source"].append(e["claim"])
    have = {e["claim"] for e in merged}
    note["gaps"] = [{"claim": c, "reason": unavailable.get(c, "no source found")} for c in planned if c not in have]
    return note


def render(entry, kind):
    if kind not in KINDS:
        raise ValueError(f"unknown content type {kind}")
    rows = [(v["value"], s["source"], s["date"]) for v in entry["values"] for s in v["sources"]]
    if kind == "financial":
        return "\n".join(["| Source | Date | Value |", "|---|---|---|"] + [f"| {s} | {d} | {v} |" for v, s, d in rows])
    if kind == "technical":
        return "\n".join([f"{entry['claim']}:"] + [f"- {v} ({s}, {d})" for v, s, d in rows])
    text = f"{entry['claim']}: " + "; ".join(f"{v} ({s}, {d})" for v, s, d in rows) + "."
    if entry["status"] == "conflict":
        text += " The sources disagree."
    elif entry["status"] == "changed":
        text += " The figures are from different dates."
    return text
