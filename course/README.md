# course/

The lessons of the course, one markdown file per page, grouped by module. These pages hold Level 1
(Foundations), modules 1 to 6 so far:

| Folder | Module | Pages |
|---|---|---|
| `01-how-a-language-model-works/` | How a language model works, for engineers | 2 |
| `02-how-models-are-made/` | How models are made (concepts only) | 2 |
| `03-claudes-family-and-its-surfaces/` | Claude's family and its surfaces | 2 |
| `04-capabilities-and-limits/` | Capabilities and limits | 2 |
| `05-working-with-an-ai-responsibly/` | Working with an AI, responsibly | 2 |
| `06-prompting-fundamentals/` | Prompting fundamentals | 4 |

Every page has the shape of a unit: a title with the level, module and exam codes, what the reader can do
afterwards, why it matters, the idea, examples, two or three traps, and a quiz or a practice. A page names
the model ids and documentation it was checked against. No page shows a live model reply: model output on
a page is a quotation from an official page (with its source) or a hand-scripted exchange labelled
**illustrative**, and every example program that produced the output shown under it ran in the course
container, offline.

## Interim markup

The markup below is interim. The framework defines the final syntax, and every construct here is
mechanical to convert. Untagged text is common to every reading mode.

### Language sections

A section that belongs to some languages is wrapped in a pair of comments, on lines of their own:

```text
<!-- lang: python,typescript -->
...prose shown when Python or TypeScript is the reading language...
<!-- /lang -->
```

The languages are `python`, `typescript`, `java` and `kotlin`. Claude Code and MCP configuration files are
language-neutral and are shown once, untagged.

### Example blocks

An example that exists in several languages is one block with a tab per language:

````text
<!-- example: <id> tabs: python,typescript -->
```python
...the source file, exactly as in examples/...
```
```text
...the output that program printed in the container...
```
```typescript
...the source file, exactly as in examples/...
```
```text
...the output that program printed...
```
<!-- /example -->
````

- Each language's code fence is followed by a `text` fence with that program's real output. The build's
  tests assert that the outputs are the same, or state the difference.
- `tabs:` lists the languages the example exists in. A page says in one line what a reader of a missing
  language does instead (the logic is the same; the example needs only the standard library).
- A block that is a library file, not a program, has no output fence.

### Illustrative exchanges and quotations

A hand-scripted model exchange is wrapped so the build can label it:

````text
<!-- illustrative -->
```text
...the scripted exchange...
```
<!-- /illustrative -->
````

A quotation from an official page is a block quote followed by a line `Source: <page>`; short quotations
only. Material adapted under a licence that permits copying carries its credit line directly beneath it.

### Quizzes

A quiz closes a page: a `## Quiz` heading, each question with its four options `a` to `d`, and a folded
`<details>` element that holds the answer key and the reason for every option. The last page of a module
closes with a `## Module quiz`. The same questions, keys and explanations are in
`exercises/<module>/tests/quiz.json`, and `tools/check_quiz.py` checks that the page and the file agree.

### Practices

A practice names its folder under `exercises/`, links its statement, and lists its cases by id. A code
practice exists in every language of the course that the module supports; the page shows the statement
once and the reader picks a language.
