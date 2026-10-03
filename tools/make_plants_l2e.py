#!/usr/bin/env python3
"""Derive the planted wrong solutions of the Level 2 practices of modules 38 to 41 from their reference solutions.

Each plant is the reference with exact replacements in one file; the script fails if a replacement does not
change the file (so a plant can never silently equal the reference) or if a pattern is missing.
usage: tools/make_plants_l2e.py
"""
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"

# practice dir -> language -> (main file, {plant: [(old, new), ...]})
PLANTS = {}

# --- PLANTS BELOW ---


def both(file, plants):
    """The same edits for the Python and the TypeScript folder: a project set-up is the same files in both."""
    return {"python": (file, plants), "typescript": (file, plants)}


PLANTS[f"{X}/38-claude-code-for-developers/unit-01/practice-1"] = both("CLAUDE.md", {
    "wrong-bypass-mode": {".claude/settings.json": [('"defaultMode": "acceptEdits"', '"defaultMode": "bypassPermissions"')]},
    "wrong-write-deny": {".claude/settings.json": [('"Read(./.env)"', '"Write(./.env)"'), ('"Read(./secrets/**)"', '"Write(./secrets/**)"')]},
    "wrong-push-ask-only": {".claude/settings.json": [('"Bash(git push *)",\n      "Bash(curl *)"', '"Bash(curl *)"'), ('"Bash(git commit *)"\n', '"Bash(git commit *)",\n      "Bash(git push *)"\n')]},
    "wrong-bare-bash-allow": {".claude/settings.json": [('"Bash(make test)",', '"Bash",\n      "Bash(make test)",')]},
    "wrong-local-not-ignored": {".gitignore": [(".claude/settings.local.json\n", "")]},
    "wrong-local-widens": {".claude/settings.local.json": [('"model": "sonnet"', '"model": "sonnet",\n  "permissions": {"allow": ["Bash(make deploy)"]}')]},
    "wrong-bloated-memory": {"CLAUDE.md": [("## Gotchas", "## Notes\n\n" + "- keep this in mind\n" * 205 + "\n## Gotchas")]},
    "wrong-no-import": {"CLAUDE.md": [("See @docs/architecture.md for", "See docs/architecture.md for")]},
    "wrong-skill-auto": {".claude/skills/fix-issue/SKILL.md": [("disable-model-invocation: true\n", "")]},
    "wrong-headless-bypass": {"scripts/ci-review.sh": [("--permission-mode dontAsk", "--dangerously-skip-permissions")]},
    "wrong-headless-unbounded": {"scripts/ci-review.sh": [("  --max-turns 5 \\\n", "")]},
    "wrong-key-in-script": {"scripts/ci-review.sh": [("set -eu\n", "set -eu\nexport ANTHROPIC_API_KEY=sk-ant-api03-EXAMPLEKEY123\n")]},
})

PLANTS[f"{X}/39-extending-claude-code/unit-01/practice-1"] = both("scripts/guard.py", {
    "wrong-push-prefix": [('if program == "git" and git_subcommand(args) == "push":', 'if program == "git" and args[:1] == ["push"]:')],
    "wrong-rm-literal": [('if program == "rm" and ("r" in short or "R" in short or "--recursive" in args) and ("f" in short or "--force" in args):', 'if program == "rm" and "-rf" in args:')],
    "wrong-pipe-shell-allowed": [('if program in ("sh", "bash", "zsh") and position > 0 and "-c" not in args:', "if False:")],
    "wrong-edit-exit-one": [('is protected ({pattern})")\n                return 2', 'is protected ({pattern})")\n                return 1')],
    "wrong-no-reason": [('                sys.stderr.write(f"Blocked: {path} is protected ({pattern})")\n', "")],
    "wrong-bad-json-allowed": [('        sys.stderr.write("Blocked: the hook event could not be read")\n        return 2\n', "        return 0\n")],
    "wrong-matcher-bash-only": {"hooks/hooks.json": [('"Bash|Edit|Write"', '"Bash"')]},
    "wrong-relative-path": {"hooks/hooks.json": [('python3 \\"${CLAUDE_PLUGIN_ROOT}/scripts/guard.py\\"', "python3 scripts/guard.py")]},
    "wrong-publish-auto": {"skills/publish/SKILL.md": [("disable-model-invocation: true\n", "")]},
    "wrong-bare-bash": {"skills/publish/SKILL.md": [("allowed-tools: Bash(git tag *) Bash(gh release create *)", "allowed-tools: Bash")]},
    "wrong-agent-writes": {"agents/changelog-reviewer.md": [("tools: Read, Grep, Glob", "tools: Read, Grep, Edit")]},
    "wrong-agent-bypass": {"agents/changelog-reviewer.md": [("memory: project\n", "memory: project\npermissionMode: bypassPermissions\n")]},
    "wrong-dependency-unpinned": {".claude-plugin/plugin.json": [('{ "name": "secrets-vault", "version": "~2.1.0" }', '"secrets-vault"')]},
    "wrong-marketplace-mismatch": {".claude/settings.json": [('"release-kit@acme-tools": true', '"release-kit@acme-plugins": true')]},
})

PLANTS[f"{X}/40-claude-in-the-software-life-cycle/unit-01/practice-1"] = both(".github/workflows/claude.yml", {
    "wrong-mention-unguarded": [("    if: contains(github.event.comment.body, '@claude')\n", "")],
    "wrong-mention-beta": [("claude-code-action@v1", "claude-code-action@beta")],
    "wrong-review-writes": {".github/workflows/review.yml": [("      contents: read\n", "      contents: write\n")]},
    "wrong-review-no-comment": {".github/workflows/review.yml": [("/code-review:code-review --comment ", "/code-review:code-review ")]},
    "wrong-review-no-checkout": {".github/workflows/review.yml": [("      - uses: actions/checkout@v6\n        with:\n          fetch-depth: 1\n", "")]},
    "wrong-prompt-version-mismatch": {"prompts/CHANGELOG.md": [("## 1.2.0\n\n- triage: ask for one sentence of reasoning.\n\n", "")]},
    "wrong-prompt-no-version": {"prompts/triage.md": [("version: 1.2.0\n", "")]},
    "wrong-review-no-skip": {"REVIEW.md": [("## Skip", "## Ignore")]},
    "wrong-no-timeout": [("    timeout-minutes: 20\n", "")],
    "wrong-no-turn-cap": [("          claude_args: --max-turns 8\n", "")],
    "wrong-literal-key": [("${{ secrets.ANTHROPIC_API_KEY }}", "sk-ant-api03-EXAMPLEKEY12345")],
})

PLANTS[f"{X}/41-security-and-safety/unit-01/practice-1"] = {
    "python": ("gate.py", {
        "wrong-concatenate": [('payload = json.dumps({"source": source, "trust": "untrusted", "content": content}, separators=(",", ":"))', 'payload = f"source={source}\\ntrust=untrusted\\n{content}"')],
        "wrong-screen-case-sensitive": [(r'\b(instructions?|prompts?|rules)\b", re.I | re.S),', r'\b(instructions?|prompts?|rules)\b", re.S),')],
        "wrong-screen-leaks": [("withheld: possible prompt injection ({', '.join(signals)})\"", "withheld: possible prompt injection ({', '.join(signals)}): {content[:40]}\"")],
        "wrong-prefix-sibling": [('return path == self.root or path.startswith(self.root + "/")', "return path.startswith(self.root)")],
        "wrong-env-variants": [('return (base == ".env" or (base.startswith(".env.") and base != ".env.example") or "secrets" in parts[:-1]', 'return (base == ".env" or "secrets" in parts[:-1]')],
        "wrong-chain-allowed": [("if any(t in command for t in SHELL_TRICKS):", "if False:")],
        "wrong-host-suffix": [('if not any(host == h or host.endswith("." + h) for h in self.allowed_hosts):', "if not any(host.endswith(h) for h in self.allowed_hosts):")],
        "wrong-email-tainted-allowed": [('        if self._tainted:\n            return _result("deny", "a person must send it")\n', "")],
        "wrong-luhn-skip": [('return "[CARD]" if 13 <= len(digits) <= 19 and _luhn(digits) else m.group(0)', 'return "[CARD]" if 13 <= len(digits) <= 19 else m.group(0)')],
        "wrong-audit-raw": [("clean = {k: (redact(v) if isinstance(v, str) else v) for k, v in args.items()}", "clean = dict(args)")],
        "wrong-hook-exit2": [('return {"exit_code": 0, "stdout": json.dumps(out, separators=(",", ":")), "stderr": ""}', 'return {"exit_code": 2, "stdout": json.dumps(out, separators=(",", ":")), "stderr": ""}')],
        "wrong-alert-asks": [('if record["decision"] == "deny":', 'if record["decision"] != "allow":')],
    }),
    "typescript": ("gate.ts", {
        "wrong-concatenate": [('JSON.stringify({ source, trust: "untrusted", content })', '`source=${source}\\ntrust=untrusted\\n${content}`')],
        "wrong-screen-case-sensitive": [(r'\b(instructions?|prompts?|rules)\b/i],', r'\b(instructions?|prompts?|rules)\b/],')],
        "wrong-screen-leaks": [('possible prompt injection (${signals.join(", ")})`', 'possible prompt injection (${signals.join(", ")}): ${content.slice(0, 40)}`')],
        "wrong-prefix-sibling": [('return path === this.root || path.startsWith(this.root + "/");', "return path.startsWith(this.root);")],
        "wrong-env-variants": [('return base === ".env" || (base.startsWith(".env.") && base !== ".env.example") || parts.slice(0, -1).includes("secrets")', 'return base === ".env" || parts.slice(0, -1).includes("secrets")')],
        "wrong-chain-allowed": [('if (SHELL_TRICKS.some((t) => command.includes(t))) return result("deny", "chaining or redirection");', "")],
        "wrong-host-suffix": [('if (!this.allowedHosts.some((h) => host === h || host.endsWith("." + h))) return result("deny", "host not allowed");', 'if (!this.allowedHosts.some((h) => host.endsWith(h))) return result("deny", "host not allowed");')],
        "wrong-email-tainted-allowed": [('    if (this.isTainted) return result("deny", "a person must send it");\n', "")],
        "wrong-luhn-skip": [('return digits.length >= 13 && digits.length <= 19 && luhn(digits) ? "[CARD]" : m;', 'return digits.length >= 13 && digits.length <= 19 ? "[CARD]" : m;')],
        "wrong-audit-raw": [('clean[k] = typeof v === "string" ? redact(v) : v;', "clean[k] = v;")],
        "wrong-hook-exit2": [('return { exit_code: 0, stdout: JSON.stringify(out), stderr: "" };', 'return { exit_code: 2, stdout: JSON.stringify(out), stderr: "" };')],
        "wrong-alert-asks": [('if (r.decision === "deny") {', 'if (r.decision !== "allow") {')],
    }),
    "java": ("Gate.java", {
        "wrong-concatenate": [('"content", Json.stringify(map("source", source, "trust", "untrusted", "content", content)));', r'"content", "source=" + source + "\ntrust=untrusted\n" + content);')],
        "wrong-screen-case-sensitive": [(r'\\b(instructions?|prompts?|rules)\\b", flags));', r'\\b(instructions?|prompts?|rules)\\b", Pattern.DOTALL));')],
        "wrong-screen-leaks": [('+ String.join(", ", signals) + ")");', '+ String.join(", ", signals) + "): " + content.substring(0, Math.min(40, content.length())));')],
        "wrong-prefix-sibling": [('return path.equals(root) || path.startsWith(root + "/");', "return path.startsWith(root);")],
        "wrong-env-variants": [('return base.equals(".env") || (base.startsWith(".env.") && !base.equals(".env.example")) || parts.subList', 'return base.equals(".env") || parts.subList')],
        "wrong-chain-allowed": [('for (String t : SHELL_TRICKS) if (command.contains(t)) return result("deny", "chaining or redirection");', "")],
        "wrong-host-suffix": [('for (String h : allowedHosts) if (host.equals(h) || host.endsWith("." + h)) listed = true;', "for (String h : allowedHosts) if (host.endsWith(h)) listed = true;")],
        "wrong-email-tainted-allowed": [('        if (tainted) return result("deny", "a person must send it");\n', "")],
        "wrong-luhn-skip": [('digits.length() >= 13 && digits.length() <= 19 && luhn(digits) ? "[CARD]" : m.group()', 'digits.length() >= 13 && digits.length() <= 19 ? "[CARD]" : m.group()')],
        "wrong-audit-raw": [("clean.put(e.getKey(), e.getValue() instanceof String s ? redact(s) : e.getValue());", "clean.put(e.getKey(), e.getValue());")],
        "wrong-hook-exit2": [('return map("exit_code", 0, "stdout", Json.stringify(out), "stderr", "");', 'return map("exit_code", 2, "stdout", Json.stringify(out), "stderr", "");')],
        "wrong-alert-asks": [('if (r.get("decision").equals("deny")) {', 'if (!r.get("decision").equals("allow")) {')],
    }),
    "kotlin": ("Gate.kt", {
        "wrong-concatenate": [('"content" to Json.stringify(linkedMapOf("source" to source, "trust" to "untrusted", "content" to content)))', r'"content" to ("source=" + source + "\ntrust=untrusted\n" + content))')],
        "wrong-screen-case-sensitive": [(r'\\b(instructions?|prompts?|rules)\\b", flags),', r'\\b(instructions?|prompts?|rules)\\b", setOf(RegexOption.DOT_MATCHES_ALL)),')],
        "wrong-screen-leaks": [('(${found.joinToString(", ")})")', '(${found.joinToString(", ")}): ${content.take(40)}")')],
        "wrong-prefix-sibling": [('private fun inside(path: String) = path == root || path.startsWith("$root/")', "private fun inside(path: String) = path.startsWith(root)")],
        "wrong-env-variants": [('return base == ".env" || (base.startsWith(".env.") && base != ".env.example") || parts.dropLast(1)', 'return base == ".env" || parts.dropLast(1)')],
        "wrong-chain-allowed": [('if (shellTricks.any { it in command }) return result("deny", "chaining or redirection")', "")],
        "wrong-host-suffix": [('if (allowedHosts.none { host == it || host.endsWith(".$it") }) return result("deny", "host not allowed")', 'if (allowedHosts.none { host.endsWith(it) }) return result("deny", "host not allowed")')],
        "wrong-email-tainted-allowed": [('        if (tainted) return result("deny", "a person must send it")\n', "")],
        "wrong-luhn-skip": [('if (digits.length in 13..19 && luhn(digits)) "[CARD]" else m.value', 'if (digits.length in 13..19) "[CARD]" else m.value')],
        "wrong-audit-raw": [("val clean = args.mapValues { (_, v) -> if (v is String) redact(v) else v }", "val clean = args")],
        "wrong-hook-exit2": [('return linkedMapOf("exit_code" to 0, "stdout" to Json.stringify(out), "stderr" to "")', 'return linkedMapOf("exit_code" to 2, "stdout" to Json.stringify(out), "stderr" to "")')],
        "wrong-alert-asks": [('if (r["decision"] == "deny") {', 'if (r["decision"] != "allow") {')],
    }),
}

# --- PLANTS ABOVE ---


def main():
    made = 0
    for practice, langs in PLANTS.items():
        cases = json.loads((ROOT / practice / "cases.json").read_text())
        for lang, (fname, plants) in langs.items():
            if sorted(plants) != sorted(cases["plants"]):
                sys.exit(f"{practice}/{lang}: plants {sorted(plants)} differ from cases.json {sorted(cases['plants'])}")
            ref = ROOT / practice / lang / "reference"
            for name, edits in plants.items():
                dest = ROOT / practice / lang / name
                if dest.exists():
                    shutil.rmtree(dest)
                shutil.copytree(ref, dest)
                by_file = edits if isinstance(edits, dict) else {fname: edits}
                for target, pairs in by_file.items():
                    f = dest / target
                    text = f.read_text()
                    for old, new in pairs:
                        if old not in text:
                            sys.exit(f"{practice}/{lang}/{name}: pattern not found: {old!r}")
                        text = text.replace(old, new, 1)
                    if text == (ref / target).read_text():
                        sys.exit(f"{practice}/{lang}/{name}: plant equals the reference")
                    f.write_text(text)
                made += 1
    print(f"made {made} planted wrong solutions")


if __name__ == "__main__":
    main()
