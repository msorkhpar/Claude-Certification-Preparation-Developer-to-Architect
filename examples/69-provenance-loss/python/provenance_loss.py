"""What a summary loses, and what a ledger keeps: sources, dates and disagreement.

The exam guide (task 5.6) says that source attribution is lost when findings are compressed without their claim-source mappings, that conflicting statistics from credible sources are annotated with their sources and not
settled by choosing one, and that dates are required so that a difference over time is not read as a contradiction. Below, seven findings from five invented sources are compressed twice (nothing here calls a model): once into
a plain summary that keeps one value per claim, and once into a ledger line per claim that keeps every value with its source and date. The names and figures are invented for the illustration.
"""
import logging

log = logging.getLogger(__name__)

FINDINGS = [
    ("market growth 2024", "12%", "Firm A report", "2024-05-01"),
    ("market growth 2024", "9%", "Firm B survey", "2024-05-01"),
    ("growth forecast", "7%", "Firm C yearbook", "2022-04-01"),
    ("growth forecast", "9%", "Firm B survey", "2024-05-01"),
    ("inflation 2023", "4%", "Firm A report", "2024-05-01"),
    ("inflation 2023", "4%", "Trade paper", "2024-06-10"),
    ("headcount", "910", "Press release", "2024-03-01"),
]


def claims_in_order(findings):
    return list(dict.fromkeys(claim for claim, _, _, _ in findings))


def status(rows):
    """agreed: one value; conflict: different values on the same date; changed: different values on different dates."""
    if len({value for _, value, _, _ in rows}) == 1:
        return "agreed"
    if any(a[1] != b[1] and a[3] == b[3] for a in rows for b in rows):
        return "conflict"
    return "changed"


def plain_summary(findings):
    """One line per claim with the first value seen: short, and the sources are gone."""
    return "\n".join(f"{claim}: {next(v for c, v, _, _ in findings if c == claim)}" for claim in claims_in_order(findings))


def ledger_lines(findings):
    """One line per claim: its status, then every value with its source and date, the oldest date first for a change."""
    lines = []
    for claim in claims_in_order(findings):
        rows = [f for f in findings if f[0] == claim]
        if status(rows) == "changed":
            rows = sorted(rows, key=lambda r: r[3])
        lines.append(f"{claim} [{status(rows)}]: " + "; ".join(f"{value} ({source}, {date})" for _, value, source, date in rows))
    return "\n".join(lines)


def sources_named(text, findings):
    return len({source for _, _, source, _ in findings if source in text})


def main():
    total = len({source for _, _, source, _ in FINDINGS})
    print(f"findings: {len(FINDINGS)} from {total} sources")
    summary = plain_summary(FINDINGS)
    print("plain summary:")
    print(summary)
    print(f"sources named by the plain summary: {sources_named(summary, FINDINGS)} of {total}")
    ledger = ledger_lines(FINDINGS)
    print("ledger:")
    print(ledger)
    print(f"sources named by the ledger: {sources_named(ledger, FINDINGS)} of {total}")


if __name__ == "__main__":
    main()
