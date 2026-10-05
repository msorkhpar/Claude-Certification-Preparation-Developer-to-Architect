# Planted wrong solutions of module 57-memory-files-and-rules: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_T0 = "Name test files `<module>.test.ts` or `<Component>.test.tsx`, next to the file they test."

_P57 = {
    "wrong-api-unscoped": {".claude/rules/api.md": [('---\npaths:\n  - "src/api/**/*.ts"\n---\n\n', "")]},
    "wrong-api-bare-folder": {".claude/rules/api.md": [('paths:\n  - "src/api/**/*.ts"\n', "paths: src/api\n")]},
    "wrong-testing-folder": {".claude/rules/testing.md": [('  - "**/*.test.ts"\n  - "**/*.test.tsx"\n', '  - "src/ui/**/*.test.tsx"\n')]},
    "wrong-testing-ts-only": {".claude/rules/testing.md": [('  - "**/*.test.tsx"\n', "")]},
    "wrong-terraform-everything": {".claude/rules/terraform.md": [('"terraform/**/*"', '"**/*"')]},
    "wrong-root-keeps-testing": {"CLAUDE.md": [("## Always\n", "## Always\n- " + _T0 + "\n")]},
    "wrong-root-long": {"CLAUDE.md": [("## Where things are\n", "## Where things are\n" + "- Background note: kept for history, it changes no behaviour.\n" * 40)]},
    "wrong-import-typo": {"CLAUDE.md": [("@docs/standards/architecture.md", "@docs/standards/architecure.md")]},
    "wrong-import-in-code-span": {"CLAUDE.md": [("@docs/standards/architecture.md", "`@docs/standards/architecture.md`")]},
    "wrong-personal-in-root": {"CLAUDE.md": [("## Always\n", "## Always\n- I prefer short answers with no preamble.\n")]},
    "wrong-local-not-ignored": {".gitignore": [("CLAUDE.local.md\n", "")]},
    "wrong-no-migration-deny": {".claude/settings.json": [('"deny": ["Edit(db/migrations/**)"]', '"deny": []')]},
    "wrong-write-rule": {".claude/settings.json": [("Edit(db/migrations/**)", "Write(db/migrations/**)")]},
    "wrong-home-path": {"CLAUDE.md": [("## Where things are\n", "## Where things are\n- My notes are in /home/dev/notes.\n")]},
}

PLANTS[f"{X}/57-memory-files-and-rules/unit-01/practice-1"] = both("CLAUDE.md", _P57)
