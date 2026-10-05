# Planted wrong solutions of module 39-extending-claude-code: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/39-extending-claude-code/unit-01/practice-1"] = both("scripts/guard.py", {
    "wrong-push-prefix": [('if program == "git" and git_subcommand(args) == "push":', 'if program == "git" and args[:1] == ["push"]:')],
    "wrong-rm-literal": [('    return program == "rm" and ("r" in short or "R" in short or "--recursive" in args) and ("f" in short or "--force" in args)', '    return program == "rm" and "-rf" in args')],
    "wrong-pipe-shell-allowed": [('    if program not in SHELLS or position == 0 or "-c" in args:\n        return False\n', "    return False\n")],
    "wrong-edit-exit-one": [('is protected ({pattern})")\n            return 2', 'is protected ({pattern})")\n            return 1')],
    "wrong-no-reason": [('            sys.stderr.write(f"Blocked: {path} is protected ({pattern})")\n', "")],
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

PLANTS[f"{X}/39-extending-claude-code/unit-02/practice-1"] = both(".claude-plugin/marketplace.json", {
    "wrong-entry-name-mismatch": [('"name": "standards-kit",\n      "source": "./plugins/standards-kit"', '"name": "standards-toolkit",\n      "source": "./plugins/standards-kit"')],
    "wrong-official-plugin-name": [('    }\n  ],\n  "renames": {', '    },\n    {\n      "name": "official-claude-tools",\n      "source": { "source": "github", "repo": "example-org/claude-helper", "ref": "v1.0.0", "sha": "1111111111111111111111111111111111111111" },\n      "description": "Looks like an official plugin"\n    }\n  ],\n  "renames": {')],
    "wrong-dotdot-source": [('"source": "./plugins/standards-kit"', '"source": "./plugins/../plugins/standards-kit"')],
    "wrong-reserved-marketplace-name": [('"name": "example-org-tools",\n  "description"', '"name": "claude-plugins-official",\n  "description"')],
    "wrong-hooks-without-wrapper": {"plugins/standards-kit/hooks/hooks.json": [('  "hooks": {\n    "PreToolUse"', '  "handlers": {\n    "PreToolUse"')]},
    "wrong-hook-script-path": {"plugins/standards-kit/hooks/hooks.json": [('"\\"${CLAUDE_PLUGIN_ROOT}\\"/scripts/protect.sh"', '"scripts/protect.sh"')]},
    "wrong-version-in-both": [('"description": "Changelog skill, edit guard and ticket server for the platform team"\n', '"description": "Changelog skill, edit guard and ticket server for the platform team",\n      "version": "1.0.0"\n')],
    "wrong-version-not-semver": {"plugins/standards-kit/.claude-plugin/plugin.json": [('"version": "1.0.0"', '"version": "1.0"')]},
    "wrong-short-sha": [('"sha": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0"', '"sha": "a1b2c3d4e5f6"')],
    "wrong-url-shorthand": [('"source": "github",\n        "repo": "example-org/lint-helper",', '"source": "url",\n        "url": "example-org/lint-helper",')],
    "wrong-no-pin": [(',\n        "sha": "0f1e2d3c4b5a69788796a5b4c3d2e1f00f1e2d3c"', "")],
    "wrong-dependency-unconstrained": {"plugins/standards-kit/.claude-plugin/plugin.json": [('{ "name": "lint-helper", "version": "~1.2.0" }', '"lint-helper"')]},
    "wrong-cross-marketplace-no-allowlist": [('  "allowCrossMarketplaceDependenciesOn": ["shared-tools"],\n', "")],
    "wrong-settings-key-mismatch": {".claude/settings.json": [('"example-org-tools": {', '"example-org-plugins": {')]},
    "wrong-enable-external-plugin": {".claude/settings.json": [('"standards-kit@example-org-tools": true', '"standards-kit@example-org-tools": true,\n    "lint-helper@example-org-tools": true')]},
    "wrong-rename-dangling": [('"std-kit": "standards-kit"', '"std-kit": "standards-tools"')],
    "wrong-personal-path": [('"description": "Plugins for the platform team"', '"description": "Plugins for the platform team, notes in /home/jane/team-notes"')],
})

_PERSONAL39 = {
    "wrong-style-name-case": {"home/.claude/settings.json": [('"outputStyle": "terse-review"', '"outputStyle": "Terse-Review"')]},
    "wrong-status-command-outside-user-folder": {"home/.claude/settings.json": [('"command": "~/.claude/statusline.sh"', '"command": "./statusline.sh"')]},
    "wrong-style-drops-coding": {"home/.claude/output-styles/terse-review.md": [("keep-coding-instructions: true", "keep-coding-instructions: false")]},
    "wrong-style-field-misspelled": {"home/.claude/output-styles/terse-review.md": [("keep-coding-instructions: true", "keep-coding-instruction: true")]},
    "wrong-status-field-unknown": {"home/.claude/statusline.sh": [(".context_window.used_percentage", ".context_window.percent_used")]},
    "wrong-status-refresh-zero": {"home/.claude/settings.json": [('"refreshInterval": 30', '"refreshInterval": 0')]},
    "wrong-key-reserved": {"home/.claude/keybindings.json": [('        "ctrl+s": null\n', '        "ctrl+c": "chat:cancel",\n        "ctrl+s": null\n')]},
    "wrong-key-context-case": {"home/.claude/keybindings.json": [('"context": "Chat"', '"context": "chat"')]},
    "wrong-key-modifier-typo": {"home/.claude/keybindings.json": [('"ctrl+e": "chat:externalEditor"', '"ctl+e": "chat:externalEditor"')]},
    "wrong-no-unbind": {"home/.claude/keybindings.json": [('"ctrl+e": "chat:externalEditor",\n        "ctrl+s": null\n', '"ctrl+e": "chat:externalEditor"\n')]},
    "wrong-style-in-project-file": {"project/.claude/settings.json": [('{\n  "permissions"', '{\n  "outputStyle": "terse-review",\n  "permissions"')]},
    "wrong-local-style-lowercase": {"project/.claude/settings.local.json": [('"Explanatory"', '"explanatory"')]},
    "wrong-local-file-tracked": {"project/.gitignore": [(".claude/settings.local.json\n", "node_modules/\n")]},
    "wrong-home-path-in-script": {"home/.claude/statusline.sh": [("# Reads the session JSON", "# Installed in /home/example-user/.claude. Reads the session JSON")]},
    "wrong-address-in-style": {"home/.claude/output-styles/terse-review.md": [("description: Lead with the verdict", "description: Lead with the verdict (contact: jane@example.org)")]},
}

PLANTS[f"{X}/39-extending-claude-code/unit-03/practice-1"] = {lang: ("home/.claude/settings.json", _PERSONAL39) for lang in ("python", "typescript", "java", "kotlin")}
