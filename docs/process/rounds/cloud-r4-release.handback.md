# Hand-back: cloud-r4-release

Branch `cloud/cloud-r4-release`, cut from `be0d7ce`. Checks: `check_quiz`, `check_coverage`,
`check_personal_data --modules '.*'` and `check_revision` all exit 0.

## Files changed

- `README.md`: rewritten for a learner (levels, not official, Docker Desktop, how a practice works, offline grading, optional key, help).
- `course/02-how-models-are-made/02-fine-tuning-feedback-and-what-training-gives.md`: constitution date corrected, checked date 2026-10-06.
- `docs/GOAL.md`, `docs/IDEA.md`, `docs/COURSE-OUTLINE.md`, `docs/EXAM-MAP.md`, `docs/VERSIONS.md`: board, draft, survey and batch references and build history removed; meaning kept. `VERSIONS.md` "Not yet pinned" replaced by the container run-times already recorded in `FEASIBILITY.md`.
- `docs/SETUP.md`: heavy-job rule brought in line with `CLAUDE.md` (two slots); otherwise left as process material.
- `docs/process/BOARD.md`: M4 and M6 done, M5 doing (no real capture pass yet), M7b todo, M9 doing with a checklist; L3-F and L4 done.
- `docs/process/PROCESS-SPLIT.md`: new; every tracked path classified (3909 files, none left out).

## Model and fact verdicts (2026-10-06, official pages only)

About 290 distinct claims in `course/` and `docs/` were checked against platform.claude.com, code.claude.com, anthropic.com and support.claude.com.
- Model ids: all correct. `claude-fable-5-1`, `claude-opus-5-5`, `claude-sonnet-5-5`, `claude-haiku-4-5-20251001` (alias `claude-haiku-4-5`) are current; `claude-mythos-5-1`, `claude-opus-5`, `claude-sonnet-4-6` are active; `claude-sonnet-4-5-20250929` is deprecated, retiring 2026-11-30; `claude-opus-4-1-20250805` was retired 2026-08-05. Prices, windows, outputs, effort defaults and retirement dates match the models overview and deprecations pages.
- Levels 1 to 4: 1 WRONG, everything else OK or UNVERIFIED. WRONG: module 02 page 2 gave the constitution's publication as 2026-01-21; the official post is dated 22 January 2026.
- Claude Code version claims (v2.1.286, v2.1.288) match the changelog; the latest release is now v2.1.290.
- SDK pins in `VERSIONS.md` match the registries at the pin date; newer releases exist (`mcp` 2.3.0, `@modelcontextprotocol/sdk` 1.32.1, `@anthropic-ai/claude-agent-sdk` 0.3.290, Claude Code 2.1.290).
- The full per-claim table (page, line, claim, official wording, URL, verdict) is in the session hand-back, not in the repository.

## Corrected

- Constitution date 2026-01-21 changed to 2026-01-22 (prose and source line), checked date updated.

## Left open

- UNVERIFIED because the page could not be reached from the container: every MCP revision 2026-07-28 claim (modules 32, 33, 35, 52, 53; `VERSIONS.md`); exam fees, item counts, timing, scoring, retakes, cancellation and eligibility (`EXAM-MAP.md`, module 11).
- UNVERIFIED for exact wording only: monitoring quotes in module 87, HIPAA-readiness scope on Claude Platform on AWS and Foundry (module 83), A2A quotes (module 86), the batch cache "30% to 98%" figure (module 21), and the 1-hour-after-5-minute breakpoint rule (module 20).
- No compose file or start command exists in the repository or `docs/SETUP.md`, so the README says the start commands come with the published images (M9 checklist).
- Try-it files exist for 34 practices (modules 6 to 53); the code practices of modules 54 to 93 have none.
- Commits carry the attribution of the model that ran this round, not the one the brief named.
