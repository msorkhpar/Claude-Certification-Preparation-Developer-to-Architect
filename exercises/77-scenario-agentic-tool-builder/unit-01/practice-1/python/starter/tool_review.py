"""Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision."""
import logging
import re

log = logging.getLogger(__name__)

FORBIDDEN = ("os.system", "subprocess", "eval(", "exec(", "__import__")
# a deliberately crude scan of the code text: which permission each marker shows (the order of this table is not the order of the report)
MARKERS = {
    "network": ("requests.", "urllib"),
    "write_files": (".write(", "shutil."),
    "read_files": ("open(", ".read("),
    "run_process": ("subprocess",),
}


def findings_of(proposal, policy):
    """TODO 1 of 6 (unlocks e1, e2 and e3): the findings about the name, the description and the limits.

    Receives the proposal and the policy. Returns a list, in this order, of `bad_name` (the name does not match `[a-z][a-z0-9_]{2,63}`),
    `short_description` (fewer than `min_words` words), `timeout` (`timeout_s` above `max_timeout`) and `memory` (`memory_mb` above `max_memory`).
    Example: a 3-word description with a policy minimum of 12 -> ["short_description"]
    """
    return []


def forbidden_calls(code):
    """TODO 2 of 6 (unlocks e4): the forbidden tokens in the code.

    Receives the code text. Returns the tokens of `FORBIDDEN` that it contains, in alphabetical order.
    Example: "eval(text)\\nos.system('ls')" -> ["eval(", "os.system"]
    """
    return []


def permissions_used(code):
    """TODO 3 of 6 (unlocks e5, e6 and e8): the permissions the code shows.

    Receives the code text. Returns the permissions of `MARKERS` that have a marker in the code, in alphabetical order (the table is not in that order).
    Example: "shutil.copy(a, b)\\nopen(a).read()" -> ["read_files", "write_files"]
    """
    return []


def refusals_of(forbidden, used, declared, denied):
    """TODO 4 of 6 (unlocks e4 and e5): the refusals, in order.

    Receives the forbidden tokens, the permissions used, the permissions declared and the permissions the policy denies. Returns
    `forbidden:<token>` for each forbidden token, then `undeclared:<permission>` for each used permission that was not declared, then
    `denied:<permission>` for each declared permission that is denied (alphabetical within each group).
    Example: refusals_of([], ["network"], ["read_files"], ["network"]) -> ["undeclared:network"]
    """
    return []


def is_gated(declared, approval):
    """TODO 5 of 6 (unlocks e6): does a declared permission need approval?

    Receives the declared permissions and the permissions that need approval. Returns True when any declared permission is among them.
    Example: is_gated(["read_files", "write_files"], ["write_files"]) -> True
    """
    return False


def decide(refusals, findings, gated):
    """TODO 6 of 6 (unlocks m1, e6 and e7): the decision.

    Receives the refusals, the findings and `gated`. Returns `refuse` when there are refusals, otherwise `revise` when there are findings,
    otherwise `approve_with_gate` when gated, otherwise `approve`.
    Example: decide([], ["timeout"], True) -> "revise"
    """
    return ""


def review(proposal, policy):
    log.debug("review input: %r", proposal)
    name, code, declared = proposal["name"], proposal["code"], proposal["permissions"]
    findings = findings_of(proposal, policy)
    used = permissions_used(code)
    refusals = refusals_of(forbidden_calls(code), used, declared, policy["denied"])
    decision = decide(refusals, findings, is_gated(declared, policy["approval"]))
    return {"name": name, "decision": decision, "refusals": refusals, "findings": findings, "used": used, "audit": f"{name}: {decision}"}
