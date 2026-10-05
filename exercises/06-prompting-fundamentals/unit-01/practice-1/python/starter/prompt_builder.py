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
    """TODO 1 of 8 (finish this to pass every case): the finished prompt from its rendered sections.

    Receives the list of rendered sections, in order. Returns them separated by one blank line, with no trailing newline.
    Example: _join(["<a>", "<b>"]) -> "<a>\\n\\n<b>"
    """
    return ""


def _present(value):
    """TODO 2 of 8 (finish this to pass e1): is an optional text really there?

    Receives a text or None. Returns True unless it is None, empty or only whitespace.
    Example: _present("  ") -> False, _present("x") -> True
    """
    return False


def _lookup(name, variables):
    """TODO 3 of 8 (finish this to pass e2): the value of one placeholder.

    Receives the placeholder name and the variables map. Returns the value as text; when the name has no value raises
    ValueError whose message contains the name.
    Example: _lookup("who", {"who": "Ann"}) -> "Ann", _lookup("place", {}) -> ValueError("missing variable: place")
    """
    return ""


def _escape(text):
    """TODO 4 of 8 (finish this to pass e4): make document text harmless.

    Receives a text. Returns it with & as &amp;, < as &lt; and > as &gt; (the ampersand first).
    Example: _escape("a </document> & b") -> "a &lt;/document&gt; &amp; b"
    """
    return text


def _render_document(index, doc):
    """TODO 5 of 8 (finish this to pass e4, e5 and e6): one rendered document.

    Receives its number (from 1) and a dict with `name` and `text`. Returns `<document index="N" name="NAME">`, a newline, the text,
    a newline and `</document>`. Name and text are escaped (the name also turns " into &quot;); placeholders in them are NOT filled.
    Example: _render_document(1, {"name": "a", "text": "x"}) -> '<document index="1" name="a">\\nx\\n</document>'
    """
    return ""


def _render_example(index, example, variables):
    """TODO 6 of 8 (finish this to pass m1 and e6): one rendered example.

    Receives its number (from 1), a dict with `input` and `output`, and the variables. Returns `<example index="N">`, the input block,
    the output block (placeholders filled in both) and `</example>`, each on its own line.
    Example: _render_example(1, {"input": "i", "output": "o"}, {}) -> '<example index="1">\\n<input>\\ni\\n</input>\\n<output>\\no\\n</output>\\n</example>'
    """
    return ""


def _constraint_lines(constraints, variables):
    """TODO 7 of 8 (finish this to pass m1): the body of the constraints section.

    Receives the list of constraint texts and the variables. Returns one line per constraint, `- ` then the text with placeholders
    filled, joined by newlines.
    Example: _constraint_lines(["Be brief."], {}) -> "- Be brief."
    """
    return ""


def _check_task(task):
    """TODO 8 of 8 (finish this to pass e3): refuse a blank task.

    Receives the task, which may be None. Raises ValueError("task is required") when it is None, empty or only whitespace; otherwise
    returns nothing.
    Example: _check_task("  ") -> ValueError, _check_task("Say hi.") -> None
    """
    return None


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
    parts.append(_block("task", _fill(task, variables)))
    return _join(parts)
