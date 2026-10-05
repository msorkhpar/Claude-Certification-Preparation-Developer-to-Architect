"""Claims that keep their sources: required provenance fields, a merge that records agreement, change over time and conflict, a coverage note with gaps, and rendering by content type. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)

REQUIRED = ("claim", "value", "source", "date")
KINDS = ("financial", "news", "technical")


def check_finding(finding):
    # TODO 1 of 8 (finish this to pass e1): the provenance check. Receives one finding. Return the names, in REQUIRED
    #   order, of the fields that are absent, empty or only white space. Example: source "" and date " " -> [source,
    #   date].
    return [f for f in REQUIRED if f not in finding]


def merge(findings):
    log.debug("merge input: %r", findings)
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
            # TODO 2 of 8 (finish this to pass m1): the sources of a value. When a finding repeats a source and date
            #   pair that the value already holds, do not add it again; otherwise add the pair last. Example: Annual
            #   report 2024-02-01 reported twice -> kept once.
            entry["sources"].append(pair)
        # TODO 3 of 8 (finish this to pass e2, e3): the status of a claim. Receives the group of findings and its
        #   distinct values. One value -> agreed. Several values where two different values share a date -> conflict (keep
        #   all). Several values with no shared date -> changed, with the values ordered by their earliest date. Example:
        #   12% and 9% both on 2024-05-01 -> conflict.
        status = "agreed"
        out.append({"claim": claim, "status": status, "values": values})
    return out


def coverage_note(planned, merged, unavailable):
    note = {"well_supported": [], "single_source": [], "changed": [], "contested": [], "gaps": []}
    for e in merged:
        if e["status"] == "conflict":
            note["contested"].append(e["claim"])
        elif e["status"] == "changed":
            note["changed"].append(e["claim"])
        # TODO 4 of 8 (finish this to pass e4): the split of the agreed claims. For an agreed claim, add it to
        #   well_supported when its value comes from at least two different sources, otherwise to single_source. Example:
        #   two sources -> well_supported; one -> single_source.
        else:
            note["single_source"].append(e["claim"])
    have = {e["claim"] for e in merged}
    # TODO 5 of 8 (finish this to pass e5): the gaps. Receives the planned claims, the merged entries and the reasons
    #   for unavailable claims. Return one {claim, reason} for each planned claim that has no merged entry, with the
    #   reason from `unavailable` or "no source found". Example: planned [a, b], merged a -> gap b.
    note["gaps"] = []
    return note


def render(entry, kind):
    if kind not in KINDS:
        raise ValueError(f"unknown content type {kind}")
    rows = [(v["value"], s["source"], s["date"]) for v in entry["values"] for s in v["sources"]]
    # TODO 6 of 8 (finish this to pass e6): the financial rendering. Return a Markdown table: the header "| Source |
    #   Date | Value |", the line "|---|---|---|" and one row "| source | date | value |" for each source of each value.
    #   Example: one source -> three lines.
    # TODO 7 of 8 (finish this to pass e8): the technical rendering. Return the claim followed by a colon, then one line
    #   "- value (source, date)" for each source of each value. Example: one source -> two lines.
    text = f"{entry['claim']}: " + "; ".join(f"{v} ({s}, {d})" for v, s, d in rows) + "."
    # TODO 8 of 8 (finish this to pass e7): the news ending. After the prose, add " The sources disagree." for a
    #   conflict and " The figures are from different dates." for a changed claim. Example: conflict -> the prose ends
    #   with that sentence.
    return text
