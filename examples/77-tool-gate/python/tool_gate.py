"""An approval gate for tools that an agent proposes, in miniature: the proposal is decided from the permissions it asks for, a reviewer answers the ones that need a person,
an approved tool runs and its result is checked against the schema it declared, and every step leaves a line in an audit log.

The tools are made-up proposals with made-up results: nothing generated is executed here, because this example is about the decisions around a run, not about running code.
The shapes (a decision, a reviewer's answer, a result check, an audit line) are this course's design, not an Anthropic interface.
"""
import logging

log = logging.getLogger(__name__)

DENIED = ("network", "run_process")
NEEDS_APPROVAL = ("write_files",)
SCHEMA = {"headline": str, "rows": int}
MAX_CHARS = 200

PROPOSALS = [
    {"name": "read_report", "permissions": ["read_files"], "result": {"headline": "Q3 up 4%", "rows": 12}},
    {"name": "write_summary", "permissions": ["read_files", "write_files"], "result": {"headline": "Q3 up 4%"}},
    {"name": "fetch_prices", "permissions": ["network"], "result": {"headline": "x", "rows": 1}},
    {"name": "tidy_up", "permissions": ["write_files"], "result": {"headline": "x", "rows": 1}},
]
REVIEWER = {"write_summary": True, "tidy_up": False}  # the person's answers, by tool name


def decide(permissions):
    """Refused when a denied permission is asked for, held for a person when a permission needs one, otherwise automatic."""
    log.debug("decide input: %r", permissions)
    denied = [p for p in permissions if p in DENIED]
    if denied:
        return "refused", denied
    if any(p in NEEDS_APPROVAL for p in permissions):
        return "needs_approval", [p for p in permissions if p in NEEDS_APPROVAL]
    return "auto", []


def check_output(result):
    """The result of a tool is data to check before the agent uses it: every declared field, of its declared type, and no more than the limit of characters of text."""
    problems = [f"missing: {field}" for field in SCHEMA if field not in result]
    problems += [f"type: {field}" for field, kind in SCHEMA.items() if field in result and not isinstance(result[field], kind)]
    if sum(len(v) for v in result.values() if isinstance(v, str)) > MAX_CHARS:
        problems.append("too large")
    return problems


def main():
    log = []
    for proposal in PROPOSALS:
        name = proposal["name"]
        decision, why = decide(proposal["permissions"])
        if decision == "refused":
            print(f"{name}: refused ({', '.join(why)})")
            log.append((name, "refused"))
            continue
        if decision == "needs_approval":
            answer = REVIEWER[name]
            print(f"{name}: needs_approval -> {'approved' if answer else 'declined'}")
            if not answer:
                log.append((name, "declined"))
                continue
        else:
            print(f"{name}: auto")
        problems = check_output(proposal["result"])
        log.append((name, "ran" if not problems else "rejected"))
        if problems:
            print(f"  result of {name} rejected ({'; '.join(problems)})")
    ran = sum(1 for _, s in log if s == "ran")
    print(f"audit: {len(PROPOSALS)} proposals, {ran} ran, {sum(1 for _, s in log if s == 'rejected')} rejected, "
          f"{sum(1 for _, s in log if s == 'refused')} refused, {sum(1 for _, s in log if s == 'declined')} declined")


if __name__ == "__main__":
    main()
