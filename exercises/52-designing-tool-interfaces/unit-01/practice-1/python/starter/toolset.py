"""Tool interfaces graded on rules: lint a tool and a tool set, page large results, and weigh a tool's annotations. See ../../statement.md."""


def lint_tool(tool):
    # TODO: the sorted list of rule ids this tool breaks.
    return None


def lint_tool_set(tools, max_tools=20):
    # TODO: sorted [tool name, rule] pairs for the whole set.
    return None


def page_results(items, cursor=None, limit=10, max_chars=2000):
    # TODO: {"items", "next_cursor", "truncated", "note"}.
    return None


def effective_hints(tool, trusted_server):
    # TODO: the four hints a client acts on.
    return None


def parallel_safe(tools, trusted_servers):
    # TODO: names of the tools that may run beside other read-only tools.
    return None
