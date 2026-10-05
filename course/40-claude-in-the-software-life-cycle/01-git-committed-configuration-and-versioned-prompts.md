# Git workflows, committed configuration and versioned prompts

**Level:** Developer · **Module 40:** Claude in the software life cycle · **Page 1 of 2**
**Exams:** DV1

**After this page you can** run Claude Code through a branch, commit and pull request workflow, decide what configuration is committed with the code and what stays local, version prompts and settings so a change is traceable, and review and refactor code that integrates Claude with a review habit that matches the risk.

Checked on 2026-10-03 against the Claude Code documentation (common workflows, settings, memory, worktrees and sessions), which mention behaviour up to Claude Code v2.1.286. Nothing here was run against a live service. The practice at the end of the module checks the files offline.

## Why it matters

Claude Code works inside a repository, so the repository is where it is configured, reviewed and controlled. A team that treats the configuration and the prompts as code gets what it gets for code: history, review, rollback and one version for everyone. A team that does not gets a different Claude on every laptop. The exam asks what is committed, what is not, and how a change to a prompt is made safely.

## The idea

### A git workflow with Claude

Claude Code sees "your git state": "Current branch, uncommitted changes, and recent commit history." It can run the usual workflow when you ask: create a branch, make the change, commit, and open a pull request. The documentation's example is to ask directly ("create a pr for my changes") or to guide it step by step, and `claude --from-pr 1234` reopens the session linked to a pull request, because Claude Code "links the session to the PR when Claude creates it with `gh pr create`".

Three habits keep the workflow safe.

- **Commit on a branch, never on `main`.** State the branch name pattern in the memory file, such as `feature/<ticket>`, so Claude names branches the way your tooling expects.
- **Ask for the commit.** Claude commits "if you ask". Review the diff with `/diff` first, then let it write the message, and read the message: the rule worth writing down is that it says why the change is needed.
- **Use a worktree for parallel work.** "Each git worktree is a separate checkout on its own branch", so two Claude sessions can edit two features without colliding. The repository needs at least one commit first.

Claude Code adds attribution lines to the commits and pull requests it writes. The `attribution` setting changes or removes them, which a team may want to set once in the shared settings.

### What is committed and what is not

The configuration has two audiences, and the file name says which.

| File | Committed | Holds |
|---|---|---|
| `CLAUDE.md` | Yes | Commands, conventions, the git workflow |
| `.claude/settings.json` | Yes | Team permission rules, default model, hooks, plugins |
| `.claude/skills/`, `.claude/agents/` | Yes | Team skills and subagents |
| `.mcp.json` | Yes | The MCP servers a project uses |
| `.claude/settings.local.json` | No, in `.gitignore` | One developer's overrides |
| `CLAUDE.local.md` | No, in `.gitignore` | One developer's notes for this project |

Two rules follow. A secret is never committed: keys and tokens are read from the environment or from GitHub Secrets, and no configuration file holds one. And a committed file is reviewed like code, because a line in `.claude/settings.json` that allows a command or enables a plugin changes what runs on every teammate's machine. Pull requests that touch these files deserve the same care as a change to a build script. Project allow rules and marketplaces wait for the folder to be trusted, so a hostile repository cannot act before a person has decided to trust it.

### Versioning prompts and settings

A prompt that a workflow sends to Claude is production configuration. Give it the treatment that other versioned artifacts get.

- **One file per prompt, in the repository**, such as `prompts/triage.md`, and not a string inside a workflow file.
- **A version in the file**, as a semantic version in the frontmatter, so a log line and a result can name the prompt that produced them.
- **A changelog entry for every change**, newest first, whose latest heading equals the file's version. A reviewer reading the pull request sees what changed and why, and a rollback is one revert.
- **A review of the change**, ideally with a sample of inputs whose outputs are compared before and after. A reworded prompt is a behaviour change, and models answer differently to small edits.

Settings follow the same pattern. Because `.claude/settings.json` is code, its history is the audit log of which commands the team allowed and when. For rules that no developer may weaken, use managed settings, which sit above every file in the repository.

### Reviewing and refactoring integration code

Code that calls Claude has its own review checklist, and an assistant that helps write it needs that checklist as written guidance. Ask of every change.

1. **Is the key outside the code?** No literal key, no key in a log, no key in a test fixture.
2. **Is every call bounded?** A maximum number of turns, a time limit, a spending cap, a limit on parallel runs.
3. **Does the code decide what the model may touch?** The tool list, the permission rule and the permission mode are explicit, and a read-only job has read-only permissions.
4. **Is untrusted text kept apart from instructions?** Module 41 covers this in depth.
5. **Is the output validated before it is used?** A reply that feeds a database or a deploy is parsed and checked, not trusted.
6. **Is there a test that runs without the model?** A scripted model makes a refactoring safe, because the behaviour around the model can be checked offline.

When Claude refactors such code, work in small steps on a branch. State the behaviour that must not change, run the tests before and after, and read the diff. For a review habit that scales, put the team's checklist in a `REVIEW.md` file that reviewers read, as page 2 shows, and keep it separate from `CLAUDE.md`, which applies to all tasks.

## Traps

1. **Committing `.claude/settings.local.json`.** It holds one developer's overrides. Add it to `.gitignore`, and put shared choices in `.claude/settings.json`.
2. **Treating a prompt as a string in a workflow file.** It then has no version, no changelog and no review.
3. **Letting Claude commit without reading the diff.** The commit is what the team keeps. Review the change, then ask for the commit.
4. **Trusting a pull request that touches configuration as if it were prose.** A new allow rule or plugin changes what runs on every machine.

## Quiz

1. A developer wants private notes about how they like to work in this repository, visible to nobody else. Which place holds them?
   - **a**: `.mcp.json`, which lists the servers the whole team uses
   - **b**: `.claude/settings.json`, which carries the team's rules to every clone
   - **c**: `CLAUDE.md`, which every session loads before the first prompt is typed
   - **d**: `CLAUDE.local.md`, which stays out of version control

2. A workflow embeds its instruction text in a string, and teammates tweak it whenever an answer looks off. What makes those edits safe to ship?
   - **a**: A separate file with a version, a changelog line and a review
   - **b**: A shorter string of a few lines in the same workflow
   - **c**: A secret-store entry in place of the repository text
   - **d**: A nightly rewrite by Claude from the day's results

3. A pull request adds a line to the committed `.claude/settings.json` that allows a new shell command. How should the team treat it?
   - **a**: As a personal preference, since each developer picks their own rules
   - **b**: As documentation, since the file only describes how Claude should act
   - **c**: As a change to a build script, since it alters what every clone runs
   - **d**: As a local override, since such lines belong in `.gitignore`

<details>
<summary>Answer key</summary>

1. **d**. The table puts "One developer's notes for this project" in `CLAUDE.local.md`, which is "No, in `.gitignore`". *b* is ruled out because the file is committed and holds "Team permission rules, default model, hooks, plugins", so the notes would reach the whole team. *c* is ruled out because `CLAUDE.md` is committed and holds "Commands, conventions, the git workflow", which every teammate loads. *a* is ruled out because `.mcp.json` holds "The MCP servers a project uses", which is configuration and not notes.
2. **a**. The page says to keep "One file per prompt, in the repository", with "A version in the file", "A changelog entry for every change" and "A review of the change". *b* is ruled out because "A reworded prompt is a behaviour change", and a shorter string is still a rewording with no version or review. *c* is ruled out because a prompt in the repository gives a diff and a revert: "a rollback is one revert", which a secret store does not offer. *d* is ruled out because a rewrite nobody reads skips the step the page asks for: "A review of the change, ideally with a sample of inputs whose outputs are compared before and after".
3. **c**. The page says "a line in `.claude/settings.json` that allows a command or enables a plugin changes what runs on every teammate's machine", so such pull requests "deserve the same care as a change to a build script". *a* is ruled out because the line "changes what runs on every teammate's machine", so it is shared behaviour and not a private choice. *b* is ruled out because the file holds "Team permission rules, default model, hooks, plugins", which are rules that take effect and not prose. *d* is ruled out because "a committed file is reviewed like code", and the shared settings file is committed, while the local file is the one kept out of version control.

</details>
