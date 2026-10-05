# Practice: write a forked review skill, a manual release skill and a command, and place the rest of the guidance

A team wants three reusable procedures for Claude Code and keeps getting them wrong. A pull request review should run in isolation and report
findings. A release tag should only ever be started by a person and may run only two kinds of shell command without asking. A standup summary is an old
command file that takes an argument. A developer also wants a personal variant of the review that must not hide the team's version. In this practice you
write those files and say where five other pieces of guidance belong. Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/`
and edit the files there. The tests are the same in all four languages and read only the files: they are configuration and notes, not code in a language,
and the Java and Kotlin tests read the YAML front matter with Jackson's YAML module. The checks run on the course's own model of the documented rules
(`examples/58-skill-model`, in your language). Nothing here starts Claude Code or touches the network.

## What is already written, and what you write

The starter is a working set of skill, command and placement files with eight gaps cut out of it. Everything that is plumbing is written and correct: the names of the review and release skills, the one-line summary of the standup command, the body of the release steps and of the personal review, and the table of the placement notes. Each gap is a spot in one file that holds a neutral value (an empty list, an empty text, a literal where a reference belongs) or a comment that says what goes there, and the list below names the file, the rule and the case it unlocks. The starter is read by the same tests, so it fails the cases on an assertion until you fill the gaps. These tests read files, not code, so there is no function to log from: read the failure message under the case, which names the file and the rule. Write the gaps in this order:

1. The forked review, in `.claude/skills/review-pr/SKILL.md` (unlocks `m1`): `context: fork`, `agent: general-purpose`, an `argument-hint`, and a body that is a task with numbered steps using `$0` (the starter holds a list of guidelines, which gives a subagent nothing to do).
2. The tool limits, in `.claude/skills/review-pr/SKILL.md` (unlocks `e2`): `allowed-tools` pre-approves only `Bash(gh pr view *)` and `Bash(gh pr diff *)` as patterns, and `disallowed-tools: Edit Write` takes the editing tools away, because `allowed-tools` only pre-approves.
3. The release start, in `.claude/skills/release-tag/SKILL.md` (unlocks `e1`): `disable-model-invocation: true`, so that only a person starts it, and `allowed-tools` limited to `Bash(git tag *)` and `Bash(git push origin *)`.
4. The arguments, in `.claude/skills/release-tag/SKILL.md` and `.claude/commands/standup.md` (unlocks `e3`): `arguments: [version]` for the `$version` the release body uses, and `$ARGUMENTS` in the standup text so that the author the user types reaches it.
5. The command names, in `.claude/commands/standup.md` and `personal/review-pr-mine/SKILL.md` (unlocks `e4`): an `argument-hint` for the standup command, and a name for the personal review that differs from the team's `review-pr` and from `release-tag`, so it does not replace one.
6. The placement rows, in `docs/placement.md` (unlocks `e5`): the team review is a project skill, the personal variant a user skill under `~/.claude/skills/`, standing standards the root `CLAUDE.md`, test-file conventions a scoped rule under `.claude/rules/`, and the release a project skill.
7. The descriptions, in the two skills (unlocks `e6`): each says what the skill does and a `Use when ...` sentence, and stays well inside the 1,536 characters of the skill listing.
8. The personal path, in `docs/placement.md` (unlocks `e7`): the line that names a path in a home folder is removed; no file holds a personal path, an address or a key.

`m1` needs gap 1. About twelve lines in all, in the files of your language folder (the four folders hold the same files). The steps below describe the whole set, so you can see how your gaps are used.

## What to write

- `.claude/skills/review-pr/SKILL.md`, the team's review:
  - runs in a subagent (`context: fork`) of type `general-purpose` and has an explicit task as its body, with numbered steps and the argument `$0`, not a list of
    guidelines (a forked skill that holds only guidelines gives the subagent nothing to do);
  - pre-approves only `gh pr view` and `gh pr diff` with `allowed-tools`, as patterns, never a bare `Bash`;
  - takes `Edit` and `Write` away with `disallowed-tools`, because `allowed-tools` only pre-approves;
  - has an `argument-hint` and a description that says what it does and when to use it ("Use when ...").
- `.claude/skills/release-tag/SKILL.md`, the release:
  - `disable-model-invocation: true`, so that only a person can start it;
  - `arguments: [version]` with `$version` used in the body, and an `argument-hint`;
  - `allowed-tools` limited to the patterns `Bash(git tag *)` and `Bash(git push origin *)`;
  - a description with a "Use when" sentence.
- `.claude/commands/standup.md`, an old-format command: it uses `$ARGUMENTS`, so the author the user types reaches the text, and it has an `argument-hint`.
- `personal/review-pr-mine/SKILL.md`, a stand-in for `~/.claude/skills/review-pr-mine/SKILL.md`: its name must differ from the team's skill, because a personal skill with the
  same name replaces the project one.
- `docs/placement.md`: fix the five rows so that each piece of guidance is in the place where it loads the way it is used (the review checklist is a
  project skill, the personal variant a user skill, standing standards the root memory file, test-file conventions a scoped rule, the release a project skill).
- No file holds a personal path, an email address or a key (the starter has a personal path; find it).
- Every description fits well inside the 1,536 characters of the skill listing.

## Why each part is there, and what you should see

1. **Fork with a task.** A forked skill starts a subagent that does not see the conversation, so its body must be a task. *You should see* the review run as a
   `general-purpose` subagent with the step list as its prompt and `$0` replaced by the pull request number.
2. **Manual and narrow.** A release has side effects, so the model must not start it, and a person who does start it should only be asked about commands
   outside the two patterns. *You should see* `/release-tag` invocable by you and not by Claude, with `git tag v1.2.0` pre-approved and `git push --force` not.
3. **Pre-approve versus remove.** `allowed-tools` grants permission for the turn and restricts nothing; a bare name in `disallowed-tools` takes the tool away, and a
   scoped one does not. *You should see* `Edit` and `Write` removed while the review runs, `Bash(gh pr diff 12)` pre-approved and `rm -rf build` left to the settings.
4. **Arguments.** `$0` and named arguments substitute, `$ARGUMENTS` receives everything, and a file whose text has no placeholder gets the input appended. *You should
   see* every file's argument arrive in its text.
5. **Names.** A command file and a skill create slash commands the same way, and of two skills with one name the higher level wins. *You should see* four different
   slash commands and the personal review not shadowing the team's.
6. **Placement.** Guidance that is always needed goes in memory, guidance that applies to a kind of file in a scoped rule, a procedure you start in a skill. *You
   should see* each of the five rows in its place.
7. **Descriptions.** The description is what Claude reads to decide when to load a skill. *You should see* each one say when to use the skill, inside the budget.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The review skill is forked, uses `general-purpose`, has numbered steps with `$0`, and an `argument-hint` |
| `e1` | The release skill is manual, takes `$version`, and pre-approves only the two patterns |
| `e2` | `disallowed-tools` removes `Edit` and `Write` from the review, and `allowed-tools` is patterns only, so nothing is mistaken for a restriction |
| `e3` | Arguments fill the placeholders of all three files, and the command receives `$ARGUMENTS` |
| `e4` | Every file creates its own slash command and the personal variant has a new name |
| `e5` | Each of the five pieces of guidance sits where it loads the way it is used |
| `e6` | Every skill says when to use it and stays inside the listing budget |
| `e7` | No file holds a personal path, an address or a key |

Run the tests with the command in the language folder's `run.sh`.
