# Inventory platform

Shared instructions for everyone who works on this repository. Area conventions live in `.claude/rules/` and load when matching
files are touched. The architecture notes are imported below.

@docs/standards/architecture.md

## Always
- Commit messages use the imperative mood and stay under 72 characters in the first line.
- Run `npm test` before saying a task is done.
- Do not add dependencies without asking.

## Where things are
- Area conventions: `.claude/rules/` (testing, API handlers, Terraform).
- Files in `db/migrations/` are protected by the permission rules, not by this file.
