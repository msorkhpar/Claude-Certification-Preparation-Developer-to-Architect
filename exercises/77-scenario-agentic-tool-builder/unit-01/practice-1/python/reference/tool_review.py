"""Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision."""
import re

FORBIDDEN = ("os.system", "subprocess", "eval(", "exec(", "__import__")
# a deliberately crude scan of the code text: which permission each marker shows (the order of this table is not the order of the report)
MARKERS = {
    "network": ("requests.", "urllib"),
    "write_files": (".write(", "shutil."),
    "read_files": ("open(", ".read("),
    "run_process": ("subprocess",),
}


def review(proposal, policy):
    name, code, declared = proposal["name"], proposal["code"], proposal["permissions"]
    findings = []
    if not re.fullmatch(r"[a-z][a-z0-9_]{2,63}", name):
        findings.append("bad_name")
    if len(proposal["description"].split()) < policy["min_words"]:
        findings.append("short_description")
    if proposal["timeout_s"] > policy["max_timeout"]:
        findings.append("timeout")
    if proposal["memory_mb"] > policy["max_memory"]:
        findings.append("memory")
    forbidden = sorted(token for token in FORBIDDEN if token in code)
    used = sorted(perm for perm, markers in MARKERS.items() if any(m in code for m in markers))
    refusals = [f"forbidden:{t}" for t in forbidden]
    refusals += [f"undeclared:{p}" for p in used if p not in declared]
    refusals += [f"denied:{p}" for p in sorted(set(declared) & set(policy["denied"]))]
    gated = any(p in policy["approval"] for p in declared)
    if refusals:
        decision = "refuse"
    elif findings:
        decision = "revise"
    elif gated:
        decision = "approve_with_gate"
    else:
        decision = "approve"
    return {"name": name, "decision": decision, "refusals": refusals, "findings": findings, "used": used, "audit": f"{name}: {decision}"}
