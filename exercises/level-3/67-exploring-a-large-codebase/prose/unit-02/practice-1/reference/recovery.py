"""What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)

STATUSES = ("done", "running", "failed")


def add_finding(findings, area, fact, location):
    if any(f["area"] == area and f["fact"] == fact for f in findings):
        return list(findings)
    return list(findings) + [{"area": area, "fact": fact, "location": location}]


def render_scratchpad(findings):
    areas = []
    for f in findings:
        if f["area"] not in areas:
            areas.append(f["area"])
    blocks = []
    for area in areas:
        lines = [f"- {f['fact']} ({f['location']})" for f in findings if f["area"] == area]
        blocks.append(f"## {area}\n" + "\n".join(lines))
    return "\n\n".join(blocks)


def build_manifest(agents):
    log.debug("build_manifest input: %r", agents)
    names = [a["name"] for a in agents]
    if len(set(names)) != len(names):
        raise ValueError("duplicate agent name")
    for a in agents:
        if a["status"] not in STATUSES:
            raise ValueError(f"unknown status {a['status']}")
    return {"version": 1, "agents": [{"name": a["name"], "state_file": a["state_file"], "status": a["status"]} for a in sorted(agents, key=lambda a: a["name"])]}


def resume_plan(manifest, existing_files):
    plan = []
    for a in manifest["agents"]:
        if a["state_file"] not in existing_files:
            action = "restart"
        elif a["status"] == "done":
            action = "reuse"
        else:
            action = "resume"
        plan.append((a["name"], action))
    return plan


def resume_prompt(task, state_lines):
    if not state_lines:
        return task
    return task + "\n\nState from the last run:\n" + "\n".join(f"- {line}" for line in state_lines) + "\nContinue from the first unfinished step."


def compact_command(keep):
    return "/compact" if not keep else "/compact Focus on " + ", ".join(keep)
