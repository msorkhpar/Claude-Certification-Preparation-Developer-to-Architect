# course/

The lessons of the course, one markdown file per page, grouped by module. These pages hold Level 1
(Foundations), modules 1 to 11, which is the whole of Level 1, and the modules of Level 2 (Developer),
modules 12 to 44, which is the whole of Level 2:

| Folder | Module | Pages |
|---|---|---|
| `01-how-a-language-model-works/` | How a language model works, for engineers | 2 |
| `02-how-models-are-made/` | How models are made (concepts only) | 2 |
| `03-claudes-family-and-its-surfaces/` | Claude's family and its surfaces | 2 |
| `04-capabilities-and-limits/` | Capabilities and limits | 2 |
| `05-working-with-an-ai-responsibly/` | Working with an AI, responsibly | 2 |
| `06-prompting-fundamentals/` | Prompting fundamentals | 4 |
| `07-claude-in-the-apps/` | Claude in the apps | 3 |
| `08-claudes-apps-in-depth/` | Claude's apps in depth | 3 |
| `09-claude-for-every-role/` | Claude for every role | 2 |
| `10-safety-privacy-and-policy/` | Safety, privacy and policy | 2 |
| `11-exam-readiness-1/` | Exam readiness 1 (three pages and the Level 1 mock exam) | 4 |
| `12-from-business-need-to-a-testable-spec/` | From business need to a testable spec | 3 |
| `13-one-rest-api-under-every-sdk/` | One REST API under every SDK | 3 |
| `14-the-messages-api/` | The Messages API | 3 |
| `15-errors-retries-and-timeouts/` | Errors, retries and timeouts | 3 |
| `16-async-concurrency-and-backpressure/` | Async, concurrency and backpressure | 2 |
| `17-streaming/` | Streaming | 2 |
| `18-model-choice-cost-and-migration/` | Model choice, cost and migration | 3 |
| `19-thinking-effort-and-speed/` | Thinking, effort and speed | 2 |
| `20-prompt-caching/` | Prompt caching | 2 |
| `21-message-batches/` | Message Batches | 2 |
| `22-claude-on-the-cloud-platforms/` | Claude on the cloud platforms | 2 |
| `23-setting-up-claude-on-the-cloud-platforms/` | Setting up Claude on the cloud platforms | 2 |
| `24-prompt-engineering-for-applications/` | Prompt engineering for applications | 2 |
| `25-structured-output-and-defensive-parsing/` | Structured output and defensive parsing | 2 |
| `26-tool-use/` | Tool use | 3 |
| `27-choosing-an-extension/` | Choosing an extension | 2 |
| `28-retrieval/` | Retrieval | 2 |
| `29-context-engineering/` | Context engineering | 3 |
| `30-vision-and-documents/` | Vision and documents | 2 |
| `31-computer-use/` | Computer use | 2 |
| `32-mcp-fundamentals/` | MCP fundamentals | 3 |
| `33-mcp-advanced/` | MCP advanced | 2 |
| `34-workflows-and-agents/` | Workflows and agents | 2 |
| `35-the-claude-agent-sdk/` | The Claude Agent SDK | 3 |
| `36-managed-and-self-hosted-agents/` | Managed and self-hosted agents | 2 |
| `37-agent-frameworks-compared/` | Agent frameworks compared | 2 |
| `38-claude-code-for-developers/` | Claude Code for developers | 3 |
| `39-extending-claude-code/` | Extending Claude Code | 3 |
| `40-claude-in-the-software-life-cycle/` | Claude in the software life cycle | 2 |
| `41-security-and-safety/` | Security and safety | 3 |
| `42-evaluation/` | Evaluation | 3 |
| `43-debugging-claude-applications/` | Debugging Claude applications | 2 |
| `44-exam-readiness-2/` | Exam readiness 2 (two pages and two Developer mock exams) | 4 |

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

### Mock exams

A mock exam is a page whose last section is a `## Mock exam` heading, in the same form as a quiz: each question with its
four options `a` to `d`, then a folded `<details>` key that explains every option. It covers a whole level, so its
questions are checked against the prose of every page of the level (an explanation's quoted phrase may come from any page of
Level 1) and against every page and module question, which it must not repeat. Its ids in `quiz.json` are `<page>#x<n>` and
its scope is `level`. Everything else about a quiz holds: `tools/check_quiz.py` applies the same rules.

### Flashcards and the review bank

Two plain JSON files in `exercises/<module>/` carry a level's revision aids; Level 1 has them in
`exercises/11-exam-readiness-1/`. `tools/check_revision.py` checks both, and `tools/test_check_revision.py` plants defects to
prove the checker catches them.

`flashcards.json`:

```text
{ "level": 1, "format": 1, "title": "...",
  "cards": [ { "id": "fc-001",                      // fc-, then a running number, in order
               "module": 1,                         // 1 to 11
               "page": "course/<module>/<page>.md", // the page the card revises; the file must exist
               "domains": ["AS3", "AS2"],           // Associate domains AS1 to AS7
               "front": "question or term",         // at most 200 characters
               "back": "answer" } ] }               // at most 420 characters
```

`review-bank.json`:

```text
{ "level": 1, "format": 1, "title": "...",
  "intervals_days": [1, 3, 7, 14, 30],              // the spaced-review schedule, ascending
  "items": [ { "id": "rb-001", "module": 1, "page": "...", "domains": ["AS3"],
               "stem": "scenario and question",
               "options": { "a": "...", "b": "...", "c": "...", "d": "..." },
               "key": "c",
               "explanation": "why the key is best" } ] }
```

A bank item obeys the quiz wording rules that a script can check (parallel options, a key that does not echo the stem, a key
no more than 1.3 times the mean length of the others, no doubled or cut-off words), must not repeat a quiz stem, and the keys
are spread over the four letters. Every module 1 to 11 and every Associate domain has a minimum number of cards and items. The
learner's spaced review is the schedule in `intervals_days`: an item moves to the next interval when answered correctly and
back to the first when it is not.

### Practices

A practice names its folder under `exercises/`, links its statement, and lists its cases by id. A code
practice exists in every language of the course that the module supports; the page shows the statement
once and the reader picks a language.
