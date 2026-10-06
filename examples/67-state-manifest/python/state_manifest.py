"""Surviving a crash during a long exploration: each agent exports its state to a known place, and the coordinator reads a manifest on resume.

The exam guide (task 5.4) describes crash recovery as agents that export structured state to a known location and a coordinator that loads a manifest on resume and injects the state into the prompts of the agents it
restarts. The Claude Code documentation (read 2026-10-04) says that subagents explore in a separate context and report back summaries, and that a context window which fills up degrades Claude's work. Below, a dictionary
stands for the file system, three agents explore three modules, one crashes, and the coordinator recovers. The sizes of the transcripts are invented for the illustration; nothing here calls a model.
"""
import logging

log = logging.getLogger(__name__)

MANIFEST = "state/manifest.txt"
TRANSCRIPT_CHARS = {"auth": 3200, "billing": 2400, "search": 1600}


def tokens(text_or_chars):
    chars = text_or_chars if isinstance(text_or_chars, int) else len(text_or_chars)
    return -(-chars // 4)


def read_manifest(fs):
    entries = {}
    for line in fs.get(MANIFEST, "").splitlines():
        name, status, path = line.split("|")
        entries[name] = (status, path)
    return entries


def write_manifest(fs, entries):
    fs[MANIFEST] = "\n".join(f"{name}|{status}|{path}" for name, (status, path) in entries.items())


def start(fs, agent):
    """The manifest is written when the agent starts, so that a crash leaves a trace."""
    entries = read_manifest(fs)
    entries[agent] = ("running", f"state/{agent}.md")
    write_manifest(fs, entries)


def finish(fs, agent, findings):
    """The state file is written when the agent has something to keep, and the manifest then says done."""
    entries = read_manifest(fs)
    path = entries[agent][1]
    fs[path] = "\n".join(f"- {fact} ({where})" for fact, where in findings)
    entries[agent] = ("done", path)
    write_manifest(fs, entries)


def recovery_plan(fs, planned):
    entries = read_manifest(fs)
    plan = []
    for agent in planned:
        status, path = entries.get(agent, ("running", f"state/{agent}.md"))
        plan.append((agent, "restart" if path not in fs else "reuse" if status == "done" else "resume"))
    return plan


def injected_state(fs, plan):
    """What the coordinator puts into the next phase's prompt: the exported findings of every agent that need not run again."""
    entries = read_manifest(fs)
    return "\n".join(f"{agent}:\n{fs[entries[agent][1]]}" for agent, action in plan if action in ("reuse", "resume"))


def main():
    fs = {}
    work = {
        "auth": [("sessions expire after 30 minutes", "auth/Session.java:18"), ("tokens are signed in TokenSigner", "auth/TokenSigner.java:12"), ("the login route is POST /login", "auth/Routes.java:7")],
        "billing": [("amounts are integer cents", "billing/Money.java:5"), ("refunds go through RefundService", "billing/RefundService.java:41")],
    }
    for agent, findings in work.items():
        start(fs, agent)
        finish(fs, agent, findings)
        print(f"{agent}: exported {read_manifest(fs)[agent][1]} ({len(findings)} findings), manifest says {read_manifest(fs)[agent][0]}")
    start(fs, "search")
    print("search: manifest says running, state file never written (crash)")
    plan = recovery_plan(fs, ["auth", "billing", "search"])
    print("recovery plan: " + ", ".join(f"{agent} {action}" for agent, action in plan))
    state = injected_state(fs, plan)
    replay = sum(tokens(n) for n in TRANSCRIPT_CHARS.values())
    print(f"injected into the next phase: {sum(len(f) for f in work.values())} findings from {len(work)} agents, about {tokens(state)} tokens")
    print(f"replaying the three transcripts instead: about {replay} tokens")


if __name__ == "__main__":
    main()
