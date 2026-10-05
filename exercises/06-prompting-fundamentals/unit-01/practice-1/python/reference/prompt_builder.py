"""Build a structured prompt from a spec. See ../../statement.md for the exact format."""
import logging
import re

log = logging.getLogger(__name__)

_VAR = re.compile(r"\{\{(\w+)\}\}")


def _fill(text, variables):
    """Replace every {{name}} in one pass; a value is inserted as it is, even when it holds another {{placeholder}}."""
    return _VAR.sub(lambda match: _lookup(match.group(1), variables), text)


def _block(tag, body):
    return f"<{tag}>\n{body}\n</{tag}>"


def _join(parts):
    return "\n\n".join(parts)


def _present(value):
    return value is not None and value.strip() != ""


def _lookup(name, variables):
    if name not in variables:
        raise ValueError(f"missing variable: {name}")
    return str(variables[name])


def _escape(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def _render_document(index, doc):
    name = _escape(doc["name"]).replace('"', "&quot;")
    return f'<document index="{index}" name="{name}">\n{_escape(doc["text"])}\n</document>'


def _render_example(index, example, variables):
    return (
        f'<example index="{index}">\n'
        f'{_block("input", _fill(example["input"], variables))}\n'
        f'{_block("output", _fill(example["output"], variables))}\n'
        "</example>"
    )


def _constraint_lines(constraints, variables):
    return "\n".join("- " + _fill(c, variables) for c in constraints)


def _check_task(task):
    if not _present(task):
        raise ValueError("task is required")


def build_prompt(spec, variables=None):
    log.debug("build_prompt input: %r %r", spec, variables)
    variables = variables or {}
    task = spec.get("task")
    _check_task(task)
    parts = []
    if _present(spec.get("role")):
        parts.append(_block("role", _fill(spec["role"], variables)))
    documents = spec.get("documents") or []
    if documents:
        rendered = [_render_document(i, doc) for i, doc in enumerate(documents, start=1)]
        parts.append(_block("documents", "\n".join(rendered)))
    if _present(spec.get("context")):
        parts.append(_block("context", _fill(spec["context"], variables)))
    examples = spec.get("examples") or []
    if examples:
        rendered = [_render_example(i, ex, variables) for i, ex in enumerate(examples, start=1)]
        parts.append(_block("examples", "\n".join(rendered)))
    constraints = spec.get("constraints") or []
    if constraints:
        parts.append(_block("constraints", _constraint_lines(constraints, variables)))
    if _present(spec.get("output_format")):
        parts.append(_block("output_format", _fill(spec["output_format"], variables)))
    parts.append(_block("task", _fill(task or "", variables)))
    return _join(parts)
