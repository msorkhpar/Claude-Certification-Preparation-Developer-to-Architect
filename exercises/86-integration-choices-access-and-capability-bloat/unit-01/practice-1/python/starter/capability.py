"""Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md."""
RISKY = {"money", "destroy"}


def audit(agent, catalog):
    # TODO: the tools to remove (held, not needed), the risky ones among them, the needed tools the agent lacks and the held, needed tools nobody used.
    return None


def plan_loading(tools, usage, keep=4, search_tokens=350):
    # TODO: load everything for a small set, otherwise keep the most used tools loaded and defer the rest behind a search tool.
    return None


def choose_mechanism(consumers, counterpart, path):
    # TODO: agent-to-agent, direct call in code, MCP server or custom tool.
    return None


def authorize(tool, user_scopes, agent_scopes, required):
    # TODO: allow, or deny with the reason, when the user's rights and the agent's rights do not both cover the tool.
    return None


def gateway(request, policy):
    # TODO: authenticate, check the model, the tool and the rate in that order, route an allowed request and keep a record of every decision.
    return None
