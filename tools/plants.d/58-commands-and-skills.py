# Planted wrong solutions of module 58-commands-and-skills: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_RV, _TG, _SU, _MI, _PL = ".claude/skills/review-pr/SKILL.md", ".claude/skills/release-tag/SKILL.md", ".claude/commands/standup.md", "personal/review-pr-mine/SKILL.md", "docs/placement.md"

_P58 = {
    "wrong-fork-guidelines-only": {_RV: [("\n1. Run `gh pr view", "\n- Run `gh pr view")]},
    "wrong-no-fork": {_RV: [("context: fork\nagent: general-purpose\n", "")]},
    "wrong-no-agent": {_RV: [("agent: general-purpose\n", "")]},
    "wrong-no-hint": {_RV: [('argument-hint: "[pr-number]"\n', "")]},
    "wrong-no-placeholder": {_RV: [("Review pull request $0.", "Review the pull request."), ("`gh pr view $0`", "`gh pr view`"), ("`gh pr diff $0`", "`gh pr diff`")]},
    "wrong-allowed-restricts": {_RV: [("disallowed-tools: Edit Write\n", "")]},
    "wrong-disallow-scoped": {_RV: [("disallowed-tools: Edit Write", "disallowed-tools: Edit(src/**) Write(src/**)")]},
    "wrong-review-bare-bash": {_RV: [("allowed-tools: Bash(gh pr view *) Bash(gh pr diff *)", "allowed-tools: Bash")]},
    "wrong-tag-bare-bash": {_TG: [("allowed-tools: Bash(git tag *) Bash(git push origin *)", "allowed-tools: Bash")]},
    "wrong-tag-auto": {_TG: [("disable-model-invocation: true\n", "")]},
    "wrong-tag-no-arguments": {_TG: [("arguments: [version]\n", "")]},
    "wrong-name-collision": {_MI: [("name: review-pr-mine", "name: release-tag")]},
    "wrong-mine-shadows": {_MI: [("name: review-pr-mine", "name: review-pr")]},
    "wrong-standup-no-hint": {_SU: [('argument-hint: "[author]"\n', "")]},
    "wrong-placement-rule-in-memory": {_PL: [(".claude/rules/testing.md", "CLAUDE.md")]},
    "wrong-placement-personal-in-project": {_PL: [("`~/.claude/skills/review-pr-mine/SKILL.md`", "`.claude/skills/review-pr-mine/SKILL.md`")]},
    "wrong-no-use-when": {_RV: [("Use when the user asks for a review", "Run it for a review")]},
    "wrong-long-description": {_TG: [("cut a release of a given version.", "cut a release of a given version." + " Extra text." * 150)]},
    "wrong-home-path": {_PL: [("# Where each piece of guidance lives\n", "# Where each piece of guidance lives\n\nNotes kept in /home/dev/notes.\n")]},
}

PLANTS[f"{X}/58-commands-and-skills/unit-01/practice-1"] = both(_RV, _P58)
