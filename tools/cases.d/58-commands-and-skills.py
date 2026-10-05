# Case lists of module 58-commands-and-skills: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/58-commands-and-skills/unit-01/practice-1"] = {
    "name": "skill_setup", "suite": "SkillSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the review skill is a forked skill with an explicit task and an argument"),
        ("e1", "edge", "the release skill is started only by a person and pre approves patterns"),
        ("e2", "edge", "tools are taken away with disallowed tools and allowed tools only pre approves"),
        ("e3", "edge", "arguments fill the placeholders of every file"),
        ("e4", "edge", "every file creates its own slash command and the personal variant has a new name"),
        ("e5", "edge", "each piece of guidance lives where it loads the way it is used"),
        ("e6", "edge", "every skill says when to use it and stays inside the listing budget"),
        ("e7", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-fork-guidelines-only": (["m1"], "writes guidelines instead of numbered steps in a forked skill"),
        "wrong-no-fork": (["m1"], "runs the review in the conversation instead of a forked context"),
        "wrong-no-agent": (["m1"], "leaves the subagent type out of the forked skill"),
        "wrong-no-hint": (["m1"], "leaves the argument hint out of the review skill"),
        "wrong-no-placeholder": (["e3", "m1"], "never uses the pull request number in the steps"),
        "wrong-allowed-restricts": (["e2"], "relies on allowed tools to keep the review from editing"),
        "wrong-disallow-scoped": (["e2"], "writes scoped rules in disallowed tools, which leave Edit and Write in place"),
        "wrong-review-bare-bash": (["e2"], "pre approves the whole Bash tool in the review skill"),
        "wrong-tag-bare-bash": (["e1"], "pre approves the whole Bash tool in the release skill"),
        "wrong-tag-auto": (["e1"], "lets Claude start the release skill on its own"),
        "wrong-tag-no-arguments": (["e3"], "uses a named argument without declaring it"),
        "wrong-name-collision": (["e4"], "gives the personal variant the name of the release skill"),
        "wrong-mine-shadows": (["e4"], "gives the personal variant the name of the team skill, so it replaces it for you"),
        "wrong-standup-no-hint": (["e4"], "leaves the hint out of the standup command"),
        "wrong-placement-rule-in-memory": (["e5"], "places the test file conventions in the always loaded file"),
        "wrong-placement-personal-in-project": (["e5"], "places the personal variant in the shared project folder"),
        "wrong-no-use-when": (["e6"], "describes the review skill without saying when to use it"),
        "wrong-long-description": (["e6"], "writes a description longer than the listing keeps"),
        "wrong-home-path": (["e7"], "writes a personal home path into the placement notes"),
    },
}
