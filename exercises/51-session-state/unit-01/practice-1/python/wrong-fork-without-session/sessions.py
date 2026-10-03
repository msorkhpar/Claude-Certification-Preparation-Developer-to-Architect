"""Session state with the Agent SDK: choose between resuming and starting fresh, say what changed, carry a summary, set the options. See ../../statement.md."""
from claude_agent_sdk import ClaudeAgentOptions, ResultMessage, query

WEEK_SECONDS = 7 * 24 * 3600  # a session idle for longer than this is not resumed
STALE_SHARE = 0.5  # more than this share of the analysed files changed or gone: start fresh


def resolve_name(name, index):
    """The id of the one session with this exact name; an unknown or a shared name is refused."""
    ids = [entry["id"] for entry in index if entry["name"] == name]
    if not ids:
        raise ValueError(f"no session is named {name}")
    if len(ids) > 1:
        raise ValueError(f"{len(ids)} sessions are named {name}: resume by id")
    return ids[0]


def plan_session(record, current, now, fork=False):
    """Decide how to continue from a saved record ({id, name, last_used, files: {path: digest}}) given the files as they are now."""
    if record is None:
        return {"action": "fresh", "session_id": None, "changed": [], "deleted": [], "added": [], "fork": False}
    before = record["files"]
    changed = sorted(p for p in before if p in current and before[p] != current[p])
    deleted = sorted(p for p in before if p not in current)
    added = sorted(p for p in current if p not in before)
    share = (len(changed) + len(deleted)) / len(before) if before else 0.0
    if share > STALE_SHARE or now - record["last_used"] > WEEK_SECONDS:
        action = "fresh_with_summary"
    elif changed or deleted or added:
        action = "resume_with_notice"
    else:
        action = "resume"
    resumed = action in ("resume", "resume_with_notice")
    return {"action": action, "session_id": record["id"] if resumed else None, "changed": changed, "deleted": deleted, "added": added, "fork": bool(fork)}


def change_notice(plan):
    """What to tell a resumed session: the files that differ from what it analysed, and to read them again."""
    lines = []
    for label, key in (("changed", "changed"), ("deleted", "deleted"), ("new", "added")):
        if plan[key]:
            lines.append(f"- {label}: {', '.join(plan[key])}")
    if not lines:
        return ""
    return "\n".join(["Since your earlier analysis:", *lines, "Re-read these files before relying on earlier conclusions about them. Every other file is unchanged."])


def _clean(items):
    seen = []
    for item in items or []:
        text = str(item).strip()
        if text and text not in seen:
            seen.append(text)
    return seen


def build_summary(findings, decisions, open_questions, files):
    """A fixed-layout summary for a fresh session: conclusions and the state they rest on, never raw tool output."""
    parts = []
    for title, items in (("Findings", _clean(findings)), ("Decisions", _clean(decisions)), ("Open questions", _clean(open_questions))):
        parts.append(f"## {title}\n" + ("\n".join(f"- {item}" for item in items) if items else "- none"))
    listed = [f"- {path} ({files[path]})" for path in sorted(files)]
    parts.append("## Files\n" + ("\n".join(listed) if listed else "- none"))
    return "\n\n".join(parts)


def first_prompt(plan, task, summary=""):
    """The prompt of the next run: the notice or the summary goes in front of the task."""
    if plan["action"] == "resume_with_notice":
        return f"{change_notice(plan)}\n\n{task}"
    if plan["action"] == "fresh_with_summary":
        return f"{summary}\n\n{task}"
    return task


def session_options(plan, **extra):
    """SDK options for a plan: resume by id, and fork together with it. A fresh start sets neither."""
    options = dict(extra)
    if plan["session_id"] is not None:
        options["resume"] = plan["session_id"]
        if plan["fork"]:
            options["fork_session"] = True
    return options


def continue_options(sessions_in_directory, **extra):
    """`continue` takes the most recent session of the directory, so it is only safe when there is exactly one."""
    if len(sessions_in_directory) != 1:
        raise ValueError(f"{len(sessions_in_directory)} sessions in this directory: resume one by id")
    return {**extra, "continue_conversation": True}


async def run_session(prompt, options):
    """Run one single-shot query and keep the session id, which every result carries, and the error the SDK raises after an error result."""
    session_id, result, error = None, None, None
    try:
        async for message in query(prompt=prompt, options=ClaudeAgentOptions(**options)):
            if isinstance(message, ResultMessage):
                session_id, result = message.session_id, message.result
    except Exception as exc:  # the error result, if there was one, has already been read
        error = str(exc)
    return {"session_id": session_id, "result": result, "error": error}
