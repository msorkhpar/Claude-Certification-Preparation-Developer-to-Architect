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
    """The findings, in order: bad_name, short_description, timeout, memory."""
    findings = []
    if not re.fullmatch(r"[a-z][a-z0-9_]{2,63}", proposal["name"]):
        findings.append("bad_name")
    if len(proposal["description"].split()) < policy["min_words"]:
        findings.append("short_description")
    if proposal["timeout_s"] > policy["max_timeout"]:
        findings.append("timeout")
    if proposal["memory_mb"] > policy["max_memory"]:
        findings.append("memory")
    return findings


def forbidden_calls(code):
    """The forbidden tokens the code contains, in alphabetical order."""
    return sorted(token for token in FORBIDDEN if token in code)


def permissions_used(code):
    """The permissions the code text shows, in alphabetical order."""
    return sorted(perm for perm, markers in MARKERS.items() if any(m in code for m in markers))


def refusals_of(forbidden, used, declared, denied):
    """forbidden:<token>, then undeclared:<permission>, then denied:<permission>, each group in alphabetical order."""
    refusals = [f"forbidden:{t}" for t in forbidden]
    refusals += [f"undeclared:{p}" for p in used if p not in declared]
    refusals += [f"denied:{p}" for p in sorted(set(declared) & set(denied))]
    return refusals


def is_gated(declared, approval):
    """True when any declared permission needs a person's approval."""
    return any(p in approval for p in declared)


def decide(refusals, findings, gated):
    """refuse beats revise, revise beats approve_with_gate, otherwise approve."""
    if refusals:
        return "refuse"
    if findings:
        return "revise"
    if gated:
        return "approve_with_gate"
    return "approve"


def review(proposal, policy):
    log.debug("review input: %r", proposal)
    name, code, declared = proposal["name"], proposal["code"], proposal["permissions"]
    findings = findings_of(proposal, policy)
    used = permissions_used(code)
    refusals = refusals_of(forbidden_calls(code), used, declared, policy["denied"])
    decision = decide(refusals, findings, is_gated(declared, policy["approval"]))
    return {"name": name, "decision": decision, "refusals": refusals, "findings": findings, "used": used, "audit": f"{name}: {decision}"}
