# Path-scoped rules, file size and the practice

**Level:** Architect · **Module 57:** Memory files and rules · **Page 2 of 2**
**Exams:** A3.1, A3.3; S2

**After this page you can** split one oversized instruction file into a short root file and rules that load only for matching files, write `paths` globs that follow file type and not folder, decide which instruction is guidance and which needs a permission rule, keep personal lines out of shared files, and write the module's practice.

Checked on 2026-10-03 against the Claude Code documentation pages "How Claude remembers your project" (memory, rules and `paths`) and "Best practices for Claude Code", documenting behaviour up to Claude Code v2.1.286. The practice is a set of files graded by Python and TypeScript test suites, offline, on the course's model of the documented loading rules (`examples/57-memory-loading`, with `examples/38-settings-layers` for permission rules); nothing in it starts Claude Code. This page deepens module 38 (the memory file and its size) and does not repeat it. The loading rules it builds on are on the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* in its task on conventions (3.3) it describes `.claude/rules/` files "with YAML frontmatter `paths` fields containing glob patterns" that "load only when editing matching files", and says path-scoping beats directory-level CLAUDE.md files "for conventions that span multiple directories (e.g., test files spread throughout a codebase)". *What the current product does (documentation checked 2026-10-03):* the same, with details. A rule with no `paths` loads at launch with the priority of the project file. A rule with `paths` loads when Claude "uses the Read, Write, or Edit tool on a file matching the pattern, not on every tool use". `paths` is the only field Claude Code reads in a rule. On the exam, a convention that follows a file type across many folders is a `.claude/rules/` file with a glob; a convention for one folder may be a directory file.

## Why it matters

One team's `CLAUDE.md` has grown to four hundred lines: testing conventions, API conventions, Terraform conventions, a commit-message rule, and a line that says never to edit the migrations. Claude reads all of it in every session, so the Terraform section is in context while someone fixes a button, and the one rule that must not be broken is a single line among hundreds. Scenario S2 asks how to reorganise it, and the tempting answers all keep the text loading every time. The documentation states the cost: files over 200 lines "consume more context and may reduce adherence". The remedy is to load text only where it applies.

## The idea

### Rules that load on a match

Every Markdown file under `.claude/rules/` is a rule. Without frontmatter it behaves like part of the project file and loads at launch. With a `paths` list in YAML frontmatter, it loads only when Claude works with a file that matches:

```yaml
---
paths:
  - "src/api/**/*.ts"
---
```

The globs are the usual ones: `**/*.ts` matches TypeScript files in any directory, `src/**/*` matches everything under `src/`, `*.md` matches Markdown files in the project root, and `src/**/*.{ts,tsx}` matches a set, with brace groups expanded. Two details decide exam answers. First, matching is by file, and the trigger is a Read, Write or Edit of a matching file, so a rule can be absent at the start of a session and present after Claude opens its first test file. Second, a glob follows the file's type wherever it sits. Test files "spread throughout a codebase", `src/auth/login.test.ts`, `web/ui/Button.test.tsx` and `tools/export.test.ts`, are all reached by one `**/*.test.{ts,tsx}` rule, which a directory-level CLAUDE.md cannot do: a directory file covers one folder and what is below it.

A broken `paths` value fails quietly in two ways. If the YAML does not parse, "Claude Code ignores the frontmatter and loads the rule as if it had no `paths`", so a rule meant to be scoped loads in every session. In the course's model of the glob rules, a bare folder name such as `terraform` matches no file, so the rule never loads; the documentation's own examples all use a wildcard or an extension, so write `terraform/**/*`. Both are caught by checking, for each pattern, that it matches at least one file in the project, which the practice's sixth case does.

### The split that saves context

The three splits compare as follows. An import moves text to another file and loads it at launch, so the cost is unchanged. A directory CLAUDE.md loads on demand, but only for one folder, so it cannot follow a file type. A path-scoped rule loads on a match, wherever the matching files are. A short root file keeps what every task needs: the commit-message rule, how to run the tests, the habit of asking before adding a dependency. Everything that concerns one kind of file moves to a rule. The documentation's own size guidance is a target of "under 200 lines per CLAUDE.md file", and the practice asks for far fewer, because a root file with scoped rules has little left to carry.

A word on dilution, which the guide names as a reason to keep files short: when guidance is long, the sentence that matters competes with all the others. The rule that must never break is better placed where it cannot be diluted at all, which is the next section.

### Guidance and enforcement

"Never edit files under `db/migrations/`" is a sentence Claude will usually follow. The documentation is explicit that it is not a guarantee: a memory file is context, delivered as a user message, and "there's no guarantee of strict compliance". If breaking the rule is costly, the same rule also becomes a permission rule that denies edits to that path, in the project's `.claude/settings.json`. The sentence stays, because it tells Claude why the edit is refused and what to do instead. The practice's fifth case checks the deny rule, and checks that it denies only that path.

### Personal lines belong in personal files

Two kinds of line do not belong in a committed file. A preference that follows you into every project, such as short answers, goes in the user file or in `~/.claude/rules/`, which apply to every project on your machine. A note about one project that nobody else needs, such as a sandbox address, goes in `CLAUDE.local.md`, which is added to `.gitignore`. A shared file that carries someone's home path, an address or a key shares them with the team, so the practice also checks that none of the files holds one.

### Reorganising an oversized file, in order

A workable order, which the practice follows:

1. Read the file and mark each line with who needs it: everyone always, a kind of file, one person.
2. Move each kind-of-file group, whole, into a rule with a glob that names the files by type; never leave half of a group behind.
3. Move personal lines to the user file or the local file.
4. Turn the line that must not be broken into a permission rule as well, and keep the sentence.
5. Keep the root file to the lines everyone needs, with imports spelled correctly (an import saves nothing, and a typo imports nothing).
6. Open a session and run `/context` to see the files that loaded, then touch a file of each kind and look again.

<!-- example: m57-memory-loading tabs: python,typescript -->
```python
"""Which instruction files are in Claude Code's context, and when: the launch set, the files that load on demand, path-scoped rules, imports and AGENTS.md.

The model follows the memory documentation read on 2026-10-03 (Claude Code v2.1.286): files in the directories above the working directory load at
launch, root first; files below it load when Claude reads there; a rule with `paths` loads when a matching file is read, written or edited; an
import expands at launch to at most four hops; AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above it.
Nothing here starts Claude Code: the "project" is a list of file paths and a dict of file texts.
"""
```
<!-- /example -->

### The practice: split one oversized memory file

The practice is in [`exercises/57-memory-files-and-rules`](../../exercises/57-memory-files-and-rules/unit-01/practice-1/statement.md). You split a long `CLAUDE.md` into a short root file with a correct import, three rules with `paths`, a permission rule for the migrations, and the personal lines in their own files. It is graded by test suites in Python and TypeScript, offline, on files that are not code in any language, which is why it has no Java or Kotlin edition. The statement lists eight cases, and each says what you should see when it works.

## Traps

1. **"Move the testing, API and Terraform sections into files and import them from `CLAUDE.md`."** It is tempting because the root file becomes short. The exam rejects it because imported files "load at launch", so every session still carries all of it. Scoped rules are the answer.
2. **"Put a `CLAUDE.md` in each folder that has tests."** It is tempting because directory files load on demand. The exam rejects it because test files "spread throughout a codebase" are in many folders, and each folder would need its own copy to be kept in step. One glob covers them.
3. **"The migrations rule is in `CLAUDE.md`, so edits there are blocked."** It is tempting because the sentence is clear and emphatic. The exam rejects it because memory is context, not enforcement; a permission rule that denies the path is what blocks the edit.
4. **"Write `paths: terraform` so the rule covers the folder."** It is tempting because it reads like the folder name. The exam rejects it because a pattern that matches no file scopes nothing, and the rule never loads; the pattern is `terraform/**/*`.

## Quiz

1. Testing conventions must govern spec files that live in dozens of different folders, and no other file. What delivers that?
   - **a**: A scoped rule whose glob matches by extension anywhere
   - **b**: A separate memory file placed in every one of those folders
   - **c**: An import of the conventions from the project root file
   - **d**: A section for each project in the personal memory file

2. A rule meant for infrastructure code loads in every session instead. What is the most likely cause?
   - **a**: The rules folder sits below the working directory
   - **b**: Infrastructure files are always opened at the start of a session
   - **c**: Its frontmatter is invalid YAML, so the scoping is dropped
   - **d**: The rule's file name does not match the folder it governs

<details>
<summary>Answer key</summary>

1. **a**. One glob follows the type across folders, and the rule loads when a matching file is read or edited. *b* is ruled out because the guide's reason for the glob is "test files spread throughout a codebase", and a copy in each folder would have to be kept in step. *c* is ruled out because an import "moves text to another file and loads it at launch", so the conventions would reach every task. *d* is ruled out because personal rules "apply to every project on your machine", so they neither scope to test files nor reach the team.
2. **c**. When the frontmatter does not parse, the rule is treated as unscoped. *a* is ruled out because "Every Markdown file under .claude/rules/ is a rule", wherever the session starts. *b* is ruled out because a scoped rule loads when Claude uses Read, Write or Edit on a matching file, "not on every tool use". *d* is ruled out because the file name plays no part, since `paths` "is the only field Claude Code reads in a rule".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. A project holds `AGENTS.md` for other tools and nothing named `CLAUDE.md`, and Claude follows it. An engineer adds `CLAUDE.local.md` with a sandbox address, and Claude stops following `AGENTS.md`. What explains it?
   - **a**: The sandbox address contradicts a rule in the team's shared notes
   - **b**: Local notes load first and replace everything that follows them
   - **c**: The local note counts as an instruction file, so the fallback no longer applies
   - **d**: Ignored files are read in place of the ones kept in version control

2. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. Its single `CLAUDE.md` has grown to four hundred lines, and a developer splits it into five files that the root pulls in with `@` references, expecting sessions to start lighter. What results?
   - **a**: The loaded text is unchanged, since imported material still arrives at launch
   - **b**: Context shrinks by four fifths, because each part loads only on demand
   - **c**: Context shrinks for any session that never touches the split-out areas
   - **d**: Claude loads the root alone and fetches the others when it needs them

3. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. A new rule for component files should apply to `.tsx` sources anywhere in the project and to nothing else. Which `paths` entry does it?
   - **a**: `*.tsx`
   - **b**: `src/components`
   - **c**: `components/**`
   - **d**: `**/*.tsx`

<details>
<summary>Answer key</summary>

1. **c**. The default is that AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above, and the local file is one. *a* is ruled out because presence decides, not content: "A `CLAUDE.md`, a `.claude/CLAUDE.md` or a `CLAUDE.local.md` in the working directory or any directory above it counts". *b* is ruled out because "All discovered files are concatenated into context rather than overriding each other", and "In each folder the local file comes after the shared one". *d* is ruled out because the file is ignored by git only so that it stays personal, "which is why that file is ignored by git", and the ignore setting plays no part in what Claude reads.
2. **a**. An import changes where text is kept and not how much of it is loaded. *b* is ruled out because imported files "are expanded and loaded into context at launch alongside the CLAUDE.md that references them". *c* is ruled out because that describes a scoped rule, since "A path-scoped rule loads on a match, wherever the matching files are", while an import "moves text to another file and loads it at launch". *d* is ruled out because an import "moves text to another file and loads it at launch, so the cost is unchanged".
3. **d**. The leading `**/` makes the pattern match the file type at any depth. *a* is ruled out because without it the pattern behaves like "`*.md` matches Markdown files in the project root", so only files at the top of the project match. *b* is ruled out because a bare folder name matches nothing: "matches no file, so the rule never loads". *c* is ruled out because it names a folder and not a type, while "a glob follows the file's type wherever it sits".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
