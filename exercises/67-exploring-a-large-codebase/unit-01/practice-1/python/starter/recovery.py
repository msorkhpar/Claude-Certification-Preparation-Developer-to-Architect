"""What a long exploration keeps outside its context: a scratchpad of findings, a manifest of agent state, and the prompt that resumes an agent after a crash. See ../../statement.md."""

STATUSES = ("done", "running", "failed")


def add_finding(findings, area, fact, location):
    # TODO: a new list with the finding added unless the same fact is already recorded for the area.
    return None


def render_scratchpad(findings):
    # TODO: Markdown text: one "## <area>" heading per area, in first-seen order, with one line per finding.
    return None


def build_manifest(agents):
    # TODO: {"version": 1, "agents": [...]} sorted by name; an error for a duplicate name or an unknown status.
    return None


def resume_plan(manifest, existing_files):
    # TODO: (name, action) for each agent of the manifest: reuse, resume or restart.
    return None


def resume_prompt(task, state_lines):
    # TODO: the prompt that continues a task from the state the last run exported.
    return None


def compact_command(keep):
    # TODO: the /compact command, telling it what to keep.
    return None
