"""Build a structured prompt from a spec. Reference solution."""
import re

_VAR = re.compile(r"\{\{(\w+)\}\}")


def _fill(text, variables):
    def sub(match):
        name = match.group(1)
        if name not in variables:
            raise ValueError(f"missing variable: {name}")
        return str(variables[name])

    return _VAR.sub(sub, text)


def _escape(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def _block(tag, body):
    return f"<{tag}>\n{body}\n</{tag}>"


def _present(value):
    return value is not None and value.strip() != ""


def build_prompt(spec, variables=None):
    variables = variables or {}
    task = spec.get("task")
    if not _present(task):
        raise ValueError("task is required")
    parts = []
    if _present(spec.get("role")):
        parts.append(_block("role", _fill(spec["role"], variables)))
    documents = spec.get("documents") or []
    if documents:
        rendered = []
        for i, doc in enumerate(documents, start=1):
            name = _escape(doc["name"]).replace('"', "&quot;")
            rendered.append(
                f'<document index="{i}" name="{name}">\n{_escape(doc["text"])}\n</document>'
            )
        parts.append(_block("documents", "\n".join(rendered)))
    if _present(spec.get("context")):
        parts.append(_block("context", _fill(spec["context"], variables)))
    examples = spec.get("examples") or []
    if examples:
        rendered = []
        for i, ex in enumerate(examples, start=1):
            rendered.append(
                f'<example index="{i}">\n'
                f'{_block("input", _fill(ex["input"], variables))}\n'
                f'{_block("output", _fill(ex["output"], variables))}\n'
                "</example>"
            )
        parts.append(_block("examples", "\n".join(rendered)))
    constraints = spec.get("constraints") or []
    if constraints:
        lines = "\n".join("- " + _fill(c, variables) for c in constraints)
        parts.append(_block("constraints", lines))
    if _present(spec.get("output_format")):
        parts.append(_block("output_format", _fill(spec["output_format"], variables)))
    parts.append(_block("task", _fill(task, variables)))
    return "\n\n".join(parts)
