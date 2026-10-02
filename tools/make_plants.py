#!/usr/bin/env python3
"""Derive the planted wrong solutions of the prompt-builder practice from its reference solutions.

Each plant is the reference with one exact replacement per file; the script fails if a replacement
does not change the file (so a plant can never silently equal the reference).
usage: tools/make_plants.py
"""
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
P = ROOT / "exercises/06-prompting-fundamentals/unit-01/practice-1"

# language -> (reference dir, main file, {plant: [(old, new), ...]})
PLANTS = {
    "python": ("prompt_builder.py", {
        "wrong-task-first": [("    parts = []\n", '    parts = [_block("task", _fill(task, variables))]\n'),
                              ('    parts.append(_block("task", _fill(task, variables)))\n', "")],
        "wrong-empty-sections": [('    if _present(spec.get("role")):', '    if "role" in spec:')],
        "wrong-missing-variable-silent": [('            raise ValueError(f"missing variable: {name}")', "            return match.group(0)")],
        "wrong-blank-task-accepted": [("    if not _present(task):", "    if task is None:")],
        "wrong-no-escape": [('{_escape(doc["text"])}', '{doc["text"]}')],
        "wrong-fill-documents": [('{_escape(doc["text"])}', '{_escape(_fill(doc["text"], variables))}')],
    }),
    "typescript": ("promptBuilder.ts", {
        "wrong-task-first": [("  const parts: string[] = [];\n", '  const parts: string[] = [block("task", fill(spec.task, variables))];\n'),
                              ('  parts.push(block("task", fill(spec.task, variables)));\n', "")],
        "wrong-empty-sections": [("  if (present(spec.role)) parts.push(", "  if (spec.role !== undefined && spec.role !== null) parts.push(")],
        "wrong-missing-variable-silent": [("if (!(name in variables)) throw new Error(`missing variable: ${name}`);", "if (!(name in variables)) return _m;")],
        "wrong-blank-task-accepted": [("  if (!present(spec.task)) throw new Error", "  if (spec.task === null || spec.task === undefined) throw new Error")],
        "wrong-no-escape": [("${escapeText(doc.text)}", "${doc.text}")],
        "wrong-fill-documents": [("${escapeText(doc.text)}", "${escapeText(fill(doc.text, variables))}")],
    }),
    "java": ("PromptBuilder.java", {
        "wrong-task-first": [("        List<String> parts = new ArrayList<>();\n", '        List<String> parts = new ArrayList<>();\n        parts.add(block("task", fill(spec.task(), variables)));\n'),
                              ('        parts.add(block("task", fill(spec.task(), variables)));\n        return', "        return")],
        "wrong-empty-sections": [("        if (present(spec.role())) parts.add(", "        if (spec.role() != null) parts.add(")],
        "wrong-missing-variable-silent": [('if (!variables.containsKey(name)) throw new IllegalArgumentException("missing variable: " + name);',
                                           "if (!variables.containsKey(name)) { m.appendReplacement(out, Matcher.quoteReplacement(m.group())); continue; }")],
        "wrong-blank-task-accepted": [("if (!present(spec.task())) throw", "if (spec.task() == null) throw")],
        "wrong-no-escape": [('+ escape(doc.text()) +', '+ doc.text() +')],
        "wrong-fill-documents": [('+ escape(doc.text()) +', '+ escape(fill(doc.text(), variables)) +')],
    }),
    "kotlin": ("PromptBuilder.kt", {
        "wrong-task-first": [("    val parts = mutableListOf<String>()\n", '    val parts = mutableListOf(block("task", fill(spec.task!!, variables)))\n'),
                              ('    parts += block("task", fill(spec.task!!, variables))\n', "")],
        "wrong-empty-sections": [("    if (present(spec.role)) parts +=", "    if (spec.role != null) parts +=")],
        "wrong-missing-variable-silent": [('        require(name in variables) { "missing variable: $name" }\n        variables.getValue(name)', "        variables[name] ?: m.value")],
        "wrong-blank-task-accepted": [("require(present(spec.task))", "require(spec.task != null)")],
        "wrong-no-escape": [("${escape(doc.text)}", "${doc.text}")],
        "wrong-fill-documents": [("${escape(doc.text)}", "${escape(fill(doc.text, variables))}")],
    }),
}


def main():
    made = 0
    for lang, (fname, plants) in PLANTS.items():
        ref = P / lang / "reference"
        for name, edits in plants.items():
            dest = P / lang / name
            if dest.exists():
                shutil.rmtree(dest)
            shutil.copytree(ref, dest)
            f = dest / fname
            text = f.read_text()
            for old, new in edits:
                if old not in text:
                    sys.exit(f"{lang}/{name}: pattern not found: {old!r}")
                text = text.replace(old, new, 1)
            if text == (ref / fname).read_text():
                sys.exit(f"{lang}/{name}: plant equals the reference")
            f.write_text(text)
            made += 1
    print(f"made {made} planted wrong solutions")


if __name__ == "__main__":
    main()
