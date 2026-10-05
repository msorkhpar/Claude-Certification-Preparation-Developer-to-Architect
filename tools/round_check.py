#!/usr/bin/env python3
"""Round spec check.

  round_check.py spec ROUND.json
      Validate the spec: required fields, types, a 40-hex commit per input, from <= to.
  round_check.py handback ROUND.json --repo NAME --branch BRANCH [--head SHA] [--dir PATH] [--main REF]
      Refuse (exit 1) a hand-back whose branch was not cut from the frozen input commit of
      repository NAME: the frozen commit must be an ancestor of the branch and the first
      commit of the branch (first-parent line) must have it as its parent. A branch that
      was rebased onto, or has merged, a newer main fails this; so does a branch whose fork
      point from --main (default main) is not the frozen commit. With --head, the branch tip
      must equal SHA.

Exit codes: 0 ok, 1 refused, 2 usage or unreadable input. Standard library only.
The built-in spec check mirrors docs/process/rounds/round.schema.json.
"""
import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

LANGS = {"python", "typescript", "java", "kotlin"}
REQUIRED = ["id", "purpose", "inputs", "module_range", "languages", "gates", "acceptance", "frozen_at"]
SCHEMA = Path(__file__).resolve().parent.parent / "docs" / "process" / "rounds" / "round.schema.json"


def load(path):
    try:
        return json.loads(Path(path).read_text())
    except (OSError, ValueError) as e:
        print(f"round_check: cannot read {path}: {e}")
        sys.exit(2)


def spec_errors(spec):
    errs = []
    if not isinstance(spec, dict):
        return ["spec is not an object"]
    for k in REQUIRED:
        if k not in spec:
            errs.append(f"missing field: {k}")
    if errs:
        return errs
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]*", str(spec["id"])):
        errs.append("id must be lowercase letters, digits and dashes")
    if not isinstance(spec["purpose"], str) or not spec["purpose"].strip():
        errs.append("purpose must be a non-empty string")
    inputs = spec["inputs"]
    if not isinstance(inputs, dict) or not inputs:
        errs.append("inputs must be a non-empty object")
    else:
        for name, v in inputs.items():
            if not isinstance(v, dict) or not re.fullmatch(r"[0-9a-f]{40}", str(v.get("commit", ""))):
                errs.append(f"inputs.{name}.commit must be a full 40-hex commit id")
    mr = spec["module_range"]
    if not (isinstance(mr, dict) and isinstance(mr.get("from"), int) and isinstance(mr.get("to"), int)):
        errs.append("module_range needs integer from and to")
    elif mr["from"] > mr["to"] or mr["from"] < 1:
        errs.append("module_range must satisfy 1 <= from <= to")
    langs = spec["languages"]
    if not isinstance(langs, list) or not langs or not set(langs) <= LANGS or len(set(langs)) != len(langs):
        errs.append("languages must be a non-empty set drawn from " + ", ".join(sorted(LANGS)))
    g = spec["gates"]
    if not isinstance(g, list) or not g or not all(isinstance(x, str) and x for x in g):
        errs.append("gates must be a non-empty list of commands")
    a = spec["acceptance"]
    if not isinstance(a, dict) or not a or not all(isinstance(v, int) and not isinstance(v, bool) and v >= 0 for v in a.values()):
        errs.append("acceptance must be a non-empty object of non-negative integers")
    if not re.fullmatch(r"\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(\.\d+)?Z", str(spec["frozen_at"])):
        errs.append("frozen_at must be a UTC time like 2026-10-05T09:00:00Z")
    unknown = set(spec) - set(REQUIRED) - {"office"}
    if unknown:
        errs.append("unknown fields: " + ", ".join(sorted(unknown)))
    return errs


def git(d, *args):
    r = subprocess.run(["git", "-C", str(d), *args], capture_output=True, text=True)
    return r.returncode, r.stdout.strip(), r.stderr.strip()


def handback_errors(spec, repo, branch, head, d, main_ref="main"):
    frozen = spec["inputs"].get(repo, {}).get("commit")
    if not frozen:
        return [f"round has no input named {repo}"]
    rc, tip, err = git(d, "rev-parse", "--verify", branch + "^{commit}")
    if rc:
        return [f"branch {branch} not found: {err}"]
    if head and tip != head:
        return [f"branch tip {tip} differs from the reported head {head}"]
    if git(d, "cat-file", "-e", frozen + "^{commit}")[0]:
        return [f"frozen commit {frozen} does not exist in {d}"]
    if git(d, "merge-base", "--is-ancestor", frozen, tip)[0]:
        return [f"frozen commit {frozen[:12]} is not an ancestor of {branch}"]
    if tip == frozen:
        return []  # no commits yet: base equals the frozen commit
    rc, out, _ = git(d, "rev-list", "--first-parent", "--reverse", f"{frozen}..{tip}")
    first = out.splitlines()[0] if out else ""
    rc, parents, _ = git(d, "rev-list", "--parents", "-n", "1", first)
    plist = parents.split()[1:]
    if frozen not in plist:
        return [f"branch base differs from the frozen commit: first commit {first[:12]} has parent(s) "
                f"{', '.join(p[:12] for p in plist)}, expected {frozen[:12]}"]
    # the branch must not carry commits that main already had at hand-back time: its fork point
    # from main is the frozen commit (skipped once the branch is merged into main)
    if main_ref and git(d, "rev-parse", "--verify", main_ref)[0] == 0 \
            and git(d, "merge-base", "--is-ancestor", tip, main_ref)[0] != 0:
        fork = git(d, "merge-base", main_ref, tip)[1]
        if fork != frozen:
            return [f"branch base differs from the frozen commit: it forks from {main_ref} at {fork[:12]}, "
                    f"expected {frozen[:12]}"]
    # no other line may have brought in history the frozen commit lacks
    rc, merges, _ = git(d, "rev-list", "--merges", f"{frozen}..{tip}")
    if merges:
        return ["branch contains merge commits since the frozen commit: " + ", ".join(m[:12] for m in merges.splitlines())]
    return []


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("spec")
    s.add_argument("round")
    h = sub.add_parser("handback")
    h.add_argument("round")
    h.add_argument("--repo", required=True)
    h.add_argument("--branch", required=True)
    h.add_argument("--head")
    h.add_argument("--dir", default=".")
    h.add_argument("--main", default="main", help="the integration branch the fork point is checked against")
    a = ap.parse_args(argv)
    spec = load(a.round)
    errs = spec_errors(spec)
    if a.cmd == "handback" and not errs:
        errs = handback_errors(spec, a.repo, a.branch, a.head, a.dir, a.main)
    if errs:
        for e in errs:
            print("REFUSED: " + e)
        return 1
    print(f"round {spec['id']}: ok")
    return 0


if __name__ == "__main__":
    sys.exit(main())
