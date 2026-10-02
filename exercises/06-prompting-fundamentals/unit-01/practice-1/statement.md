# Practice: a prompt template builder, graded on structure

Write `build_prompt` (Python), `buildPrompt` (TypeScript), `PromptBuilder.build` (Java) or `buildPrompt`
(Kotlin): it turns a prompt **spec** into one prompt string. Pick your language folder, open `starter/` and
edit the file there. The tests check the structure of the string, exactly, so the format below is the
contract.

## The spec

| Field | Type | Meaning |
|---|---|---|
| `task` | text | The ask. **Required.** |
| `role` | text, optional | Who Claude is for this task |
| `context` | text, optional | Why and for whom |
| `documents` | list of `name` and `text`, optional | Source material (untrusted data) |
| `examples` | list of `input` and `output`, optional | Worked examples |
| `constraints` | list of text, optional | Rules the answer must obey |
| `output_format` | text, optional | The shape of the answer |

Plus a `variables` map from names to text. (TypeScript, Java and Kotlin name the field `outputFormat`.)

## The output

Sections appear in this order, separated by one blank line, with no trailing newline. A section is
**omitted entirely** when its field is absent, `null`, empty or only whitespace; `task` is never omitted.

```text
<role>
{role}
</role>

<documents>
<document index="1" name="{name}">
{text}
</document>
<document index="2" name="{name}">
{text}
</document>
</documents>

<context>
{context}
</context>

<examples>
<example index="1">
<input>
{input}
</input>
<output>
{output}
</output>
</example>
</examples>

<constraints>
- {first constraint}
- {second constraint}
</constraints>

<output_format>
{output_format}
</output_format>

<task>
{task}
</task>
```

Documents and examples keep the order they were given, numbered from 1.

## Rules

1. **Variables.** `{{name}}` in `role`, `context`, `constraints`, `output_format`, `task`, and the `input`
   and `output` of examples is replaced by the variable's value, in one pass: a value that itself contains
   `{{other}}` is inserted as it is. A placeholder with no value is an error that names the variable
   (Python `ValueError`, TypeScript `Error`, Java and Kotlin `IllegalArgumentException`; the message
   contains the variable's name).
2. **Documents are data.** In a document's `text` and `name`, replace `&` with `&amp;`, `<` with `&lt;` and
   `>` with `&gt;`, and in the `name` also `"` with `&quot;`, so a document can never close its own tag.
   Placeholders inside documents are **not** replaced.
3. **A blank task is refused** with the same error type: missing, empty or only whitespace.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A full spec renders every section in the order above |
| `e1` | Absent, empty and whitespace-only optional sections are omitted, not rendered empty |
| `e2` | Variables are filled in one pass; a missing variable is named in the error |
| `e3` | A missing, empty or whitespace-only task is refused |
| `e4` | Document text and names are escaped, so a document cannot close its own tag |
| `e5` | Placeholders inside document text stay literal |
| `e6` | Documents and examples keep their order and their numbering |

Run the tests with the command in the language folder's `run.sh`.
