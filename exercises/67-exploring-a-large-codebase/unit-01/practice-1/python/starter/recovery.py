"""What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)

STATUSES = ("done", "running", "failed")


def add_finding(findings, area, fact, location):
    # TODO 1 of 6 (finish this to pass m1): the scratchpad's duplicate rule. Receives the findings so far and a new one
    #   (area, fact, location). When a finding with the same area and the same fact already exists, return the findings
    #   unchanged; otherwise return them with the new one added last. Example: adding (api, "uses JWT") twice -> one
    #   entry.
    return list(findings) + [{"area": area, "fact": fact, "location": location}]


def render_scratchpad(findings):
    areas = []
    for f in findings:
        if f["area"] not in areas:
            areas.append(f["area"])
    blocks = []
    for area in areas:
        # TODO 2 of 6 (finish this to pass e1): the lines of one area. Receives all the findings and one area. Return
        #   one line "- fact (location)" for each finding of that area only, in order. Example: findings of api and db,
        #   area api -> the api lines only.
        lines = [f"- {f['fact']} ({f['location']})" for f in findings]
        blocks.append(f"## {area}\n" + "\n".join(lines))
    return "\n\n".join(blocks)


def build_manifest(agents):
    log.debug("build_manifest input: %r", agents)
    names = [a["name"] for a in agents]
    # TODO 3 of 6 (finish this to pass e2): the checks of the manifest. Refuse with an error when two agents have the
    #   same name, and when an agent's status is not one of STATUSES. Example: two agents named a -> refused; status
    #   "paused" -> refused.
    return {"version": 1, "agents": [{"name": a["name"], "state_file": a["state_file"], "status": a["status"]} for a in sorted(agents, key=lambda a: a["name"])]}


def resume_plan(manifest, existing_files):
    plan = []
    for a in manifest["agents"]:
        # TODO 4 of 6 (finish this to pass e3, e4, e5): the action for one agent. When its state file does not exist,
        #   restart; otherwise when its status is done, reuse it; otherwise resume it from the state file. Example: done
        #   with its file -> reuse; failed with its file -> resume; any status without its file -> restart.
        action = "skip"
        plan.append((a["name"], action))
    return plan


def resume_prompt(task, state_lines):
    # TODO 5 of 6 (finish this to pass e6): the resume prompt. Receives the task and the state lines. With no state
    #   lines, return the task alone; otherwise the task, a blank line, "State from the last run:", one "- line" per state
    #   line, and "Continue from the first unfinished step.". Example: [] -> the task.
    return task


def compact_command(keep):
    # TODO 6 of 6 (finish this to pass e7): the compact command. Receives the things to keep. Return /compact when there
    #   are none, otherwise "/compact Focus on " followed by them joined with ", ". Example: [auth flow, schema] ->
    #   /compact Focus on auth flow, schema.
    return "/compact"
