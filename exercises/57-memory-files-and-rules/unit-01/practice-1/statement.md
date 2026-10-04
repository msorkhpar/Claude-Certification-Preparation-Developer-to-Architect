# Practice: split one oversized memory file into a root file, scoped rules and personal files

A team keeps all of its instructions for Claude Code in one long `CLAUDE.md`: universal rules, testing, API and Terraform conventions, a rule that must never
be broken, and some lines that are only one engineer's preferences. Claude reads that file in every session, so the conventions for Terraform sit in
the context while someone fixes a button, and the rule that matters most is one line among sixty. In this practice you split it. Pick your language
folder (`python` or `typescript`), open `starter/` and edit the files there. The tests are the same in both languages and read only the files: they are
configuration and notes, not code in a language, so there is no Java or Kotlin edition (no YAML or JSON reader is available offline for those two here,
and a second edition would test the same files). The checks run on the course's own model of the documented loading rules (`examples/57-memory-loading`,
with `examples/38-settings-layers` for permission rules). Nothing here starts Claude Code or touches the network.

## What to write

- `CLAUDE.md`, the shared root file:
  - at most 50 lines (the documentation targets under 200 for any file; the root of a project with scoped rules needs far fewer);
  - it keeps the three rules every task needs, under `## Always`: the commit message rule, running `npm test`, and asking before adding dependencies;
  - it imports `docs/standards/architecture.md` with an `@` import, spelled correctly and not inside a code span (the starter misspells it);
  - it holds none of the testing, API or Terraform conventions, and neither does anything it imports: an import loads at launch too.
- `.claude/rules/testing.md`, `.claude/rules/api.md` and `.claude/rules/terraform.md`: each starts with YAML frontmatter that has a `paths` list of globs, and holds
  the whole set of its area's conventions from the starter (move all of them, not some).
  - testing: test files wherever they are, `.test.ts` and `.test.tsx`, matched by file type and not by folder;
  - API: the TypeScript files under `src/api/`;
  - Terraform: everything under `terraform/`.
- `.claude/settings.json`: the rule "never edit files under `db/migrations/`" is a permission rule as well as a sentence, because memory is context and not
  enforcement. Deny edits there and nothing else.
- `user-memory.example.md` and `CLAUDE.local.example.md`: stand-ins for the user-scope memory file in your home folder and for the uncommitted
  `CLAUDE.local.md` at the project root. The preference about short answers goes in the first, the sandbox note in the second; neither stays in a shared file.
- `.gitignore`: ignore `CLAUDE.local.md`.
- No file holds a personal path, an email address or a key. (The starter has a personal path; find it.)

## Why each part is there, and what you should see

1. **Scoped rules.** The exam asks how to apply conventions to files by type, wherever they live. *You should see* seven touches (an API file, a component, two
   kinds of test file, a Terraform file, the README, a test in a tools folder) each bring in exactly the conventions that belong to it, and the root rules every time.
2. **A short root.** The documentation says longer files "consume more context and reduce adherence". *You should see* a root file that is a screenful and names
   where the rest lives.
3. **File type, not folder.** A directory-level `CLAUDE.md` cannot follow test files spread over many folders; a glob can. *You should see* the testing rule cover
   three test files in three folders and no other file.
4. **Imports.** An import keeps a file modular and saves no context; a typo imports nothing and says nothing. *You should see* one import that resolves.
5. **Who gets what.** A user-scope line reaches only you, and `CLAUDE.local.md` is for personal project notes that are never committed. *You should see* no
   personal line in a shared file and the local file ignored.
6. **Context versus enforcement.** A sentence in a memory file is guidance Claude may not follow. *You should see* the migration edit denied by a rule.
7. **Valid frontmatter.** A `paths` value that does not parse, or that is a bare folder name, scopes nothing: the rule loads in every session or never.
   *You should see* every pattern balanced and matching at least one file.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Seven touched files each bring in exactly the right conventions, and the root rules every time |
| `e1` | The root file is at most 50 lines, keeps the universal rules and carries no area convention, not even through an import |
| `e2` | The testing rule covers test files in three different folders and no other file |
| `e3` | The import names a file that exists and is expanded |
| `e4` | Personal lines are in the personal files, absent from shared ones, and `CLAUDE.local.md` is ignored |
| `e5` | Edits under `db/migrations/` are denied and other edits are not |
| `e6` | Every rule has a list of valid `paths`, and each pattern matches a file |
| `e7` | No file holds a personal path, an address or a key |

Run the tests with the command in the language folder's `run.sh`.
