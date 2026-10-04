# Practice: a complete Claude Code setup for a small project

A team builds a small web application with Claude Code. Its first setup is one long root memory file that holds every convention, a review checklist and a personal note, and its settings
allow the whole `Bash` tool. In this practice you correct the setup: the conventions move to rule files that load only for the files of their area, the review becomes a shared command, the
settings protect the environment file, and a table settles when to plan and when to edit directly. There is no program to write and no model is called: the tests read your files. The names and
paths the tests check are the documented ones of Claude Code (checked 2026-10-04): `CLAUDE.md`, `.claude/rules/` with a `paths` list of globs, `.claude/commands/`, and permission rules in
`.claude/settings.json`; the limit of 25 lines for the root file and the modes table are this course's own conventions. It is in Python, TypeScript, Java and Kotlin; pick your language folder,
open `starter/` and edit the files there; each language folder holds its own copy of the files.

## What to write

The project folder holds `CLAUDE.md`, `.claude/rules/`, `.claude/commands/review.md`, `.claude/settings.json` and `docs/working-modes.md`. The sample files the tests use are
`src/ui/Button.tsx`, `server/handlers/orders.ts`, `server/db/orderRepo.ts`, a `.spec.ts` or `.spec.tsx` file beside each, and `docs/readme.md`.

- **Rules.** One rule file for each convention: components (hooks) for `src/ui`, handlers (async/await) for `server/handlers`, database access (the repository pattern) for `server/db`, and
  tests (`describe`) for every `.spec.ts` and `.spec.tsx` file wherever it sits. Each file has front matter with a `paths` list of globs, written like `src/ui/**/*.tsx`. A convention must load
  for the files of its area and for no other sample file.
- **The root file.** `CLAUDE.md` holds only what every task needs, in at most 25 lines that are not blank, and none of the area conventions. It points to the rules and to `docs/working-modes.md`.
- **The review command.** `.claude/commands/review.md` has a `description`, an `allowed-tools` line that has `Read` and none of `Bash`, `Edit` or `Write` on its own (`Bash(git diff *)` is
  allowed), and a body that says to run `git diff`.
- **The settings.** `Read(./.env)` is denied, and no allow rule is `Bash`, `Bash(*)`, `Edit` or `Write`.
- **The modes table.** `docs/working-modes.md` has a table of `Task | Mode | Why`. A typo fix, a validation check in one handler and a rename of a local variable are `direct`; the restructuring
  of the monolith, the migration to a new auth library and a feature with unclear requirements are `plan`.
- **No personal data.** No file holds a home path, an address other than `example.com`, or a key.

## Why each part is there, and what you should see

1. **A convention should cost context only where it applies.** Area conventions in the root file load in every session. *You should see* each convention load for its own files and no others,
   and the testing convention load for spec files wherever they are.
2. **The root file is for what every task needs.** *You should see* a short file that points elsewhere.
3. **A shared command is a file in the project.** *You should see* a review that every clone has and that can only read.
4. **A rule that must hold goes in the settings, not in a sentence.** *You should see* the environment file denied and no whole tool approved.
5. **Planning has a cost, so it is chosen by the task.** *You should see* open design work sent to plan mode and clear small work sent to direct execution.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Each convention loads for exactly the files of its area |
| `e1` | The root file is short and holds none of the area conventions |
| `e2` | The review command is shared, read only and says what it does |
| `e3` | The settings protect the environment file and approve no whole tool |
| `e4` | The modes table sends open design work to plan mode and clear small work to direct |
| `e5` | Every rule has a `paths` list, and its globs match at least one sample file |
| `e6` | No file holds a personal path, an address or a key |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
