# Cloud round cloud-r4-release: release preparation from the source

For a cloud session working alone on this repository. Read `CLAUDE.md` first: its rules bind
this round. Push only the branch `cloud/cloud-r4-release`. Budget cap: about 25 USD; stop and
hand back what you have when you near it. No Docker is needed. Do not edit lesson prose,
practices, quizzes or examples, except where task 2 finds a wrong model id or product fact.

## Tasks

1. **Learner README.** Rewrite `README.md` for a learner who clones the repository:
   - what the course is (four levels, Foundations to Architect Professional);
   - that it is not official and makes no pass claim;
   - what they need (Docker Desktop);
   - how to start it with the commands in `docs/SETUP.md` (check them against the compose files);
   - how a practice works (Run executes your try-it file and shows your prints and logs;
     Submit grades against the tests);
   - that every graded practice runs offline, and that an API key is optional and never graded;
   - where help lives.
   Plain words, short, current state only.
2. **Model ids and product facts.** List every model id and dated product claim in `course/` and
   `docs/` (grep for `claude-`, `Opus`, `Sonnet`, `Haiku`, `Fable` and "checked"). Check each
   against Anthropic's official pages (docs.claude.com, docs.anthropic.com, code.claude.com,
   anthropic.com):
   - correct only what is wrong, and update the "checked" date to the day you checked;
   - put the per-claim verdicts (page, line, claim, official wording ≤ 25 words, URL, verdict)
     in the hand-back, not in the repository: sources are never named in the repository.
3. **Current-state docs.** Read every file in `docs/` outside `docs/process/`. Remove fix
   history, round or task ids and any wording about how the course was built; describe the
   course as it is. Keep the meaning.
4. **The board.** Bring `docs/process/BOARD.md` up to date from the repository itself: levels
   written, practices with try-it files, quizzes read, sites built. Mark M4 to M7b done where the
   repository shows it. Write M9's remaining items as a checklist.
5. **The process split, as a list only.** Write `docs/process/PROCESS-SPLIT.md` listing every
   path that is process material and moves to the `process` branch before release (CLAUDE.md
   says what counts), and every path that stays on `main` for learners. Move nothing.

## Checks before the hand-back

`python3 tools/check_quiz.py`, `python3 tools/check_coverage.py`,
`python3 tools/check_personal_data.py --modules '.*'`, `python3 tools/check_revision.py`: all
exit 0.

## Hand back

Commit `docs/process/rounds/cloud-r4-release.handback.md`, at most 40 lines:
- the files changed;
- the model and fact verdicts;
- what you corrected;
- anything left open.
Commit messages end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Push only
`cloud/cloud-r4-release`. No pull request.
