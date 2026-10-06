import asyncio
import os
import sys
from pathlib import Path

from mcp import Client
from mcp.client.stdio import StdioServerParameters

SOLUTION = Path(str(Path(__file__).resolve().parent)) / "notes_server.py"


def session(steps):
    """Start the solution as a server over stdio, connect the SDK client to it and run steps(client) -> value.
    Everything the tests check is returned and asserted after the session closed (an assertion inside the
    session would reach pytest wrapped in the task group's exception group)."""
    async def go():
        params = StdioServerParameters(command=sys.executable, args=[str(SOLUTION)], env=dict(os.environ))
        async with Client(params) as client:
            return await steps(client)
    return asyncio.run(asyncio.wait_for(go(), 60))


async def attempt(coro):
    """The result of an awaitable, or the exception it raised."""
    try:
        return await coro
    except Exception as e:  # noqa: BLE001 - the tests look at what the server sent back
        return e


def text_of(result):
    return "".join(getattr(c, "text", "") for c in getattr(result, "content", []) or [])


def add(client, title, text="body"):
    return attempt(client.call_tool("add_note", {"title": title, "text": text}))


def test_m1_a_client_can_save_a_note_find_it_and_read_it_back():
    async def steps(c):
        saved = await add(c, "Plan", "ship it")
        found = await attempt(c.call_tool("search_notes", {"query": "ship"}))
        read = await attempt(c.read_resource("notes://note/1"))
        return c.server_info, saved, found, read
    info, saved, found, read = session(steps)
    assert (info.name, info.version) == ("notes", "1.0.0")
    assert text_of(saved) == "Saved note 1: Plan" and not saved.is_error
    assert text_of(found) == "1. Plan"
    assert not isinstance(read, Exception) and read.contents[0].text == "Plan\n\nship it"


def test_e1_the_server_declares_tools_resources_and_prompts_and_names_its_tools():
    async def steps(c):
        return c.server_capabilities, await attempt(c.list_tools())
    caps, listed = session(steps)
    assert caps.tools is not None and caps.resources is not None and caps.prompts is not None
    assert not isinstance(listed, Exception)
    tools = {t.name: t for t in listed.tools}
    assert sorted(tools) == ["add_note", "search_notes"]
    assert all(t.description for t in tools.values())
    assert tools["add_note"].input_schema["required"] == ["title", "text"]
    assert tools["search_notes"].input_schema["required"] == ["query"]
    limit = tools["search_notes"].input_schema["properties"]["limit"]
    assert limit["type"] == "integer" and limit["default"] == 5


def test_e2_bad_input_comes_back_as_a_tool_error_the_model_can_read():
    async def steps(c):
        return [await add(c, "  ", "x"), await add(c, "T", "   "), await add(c, "T", "x" * 501), await add(c, "T", "x" * 500),
                await attempt(c.call_tool("search_notes", {"query": " "})), await attempt(c.call_tool("search_notes", {"query": "x", "limit": 0})),
                await attempt(c.call_tool("search_notes", {"query": "x", "limit": 21}))]
    blank_title, blank_text, long_text, edge_text, blank_query, low, high = session(steps)
    for result, message in ((blank_title, "title is required"), (blank_text, "text is required"), (long_text, "text is too long (max 500)"),
                            (blank_query, "query is required"), (low, "limit must be between 1 and 20"), (high, "limit must be between 1 and 20")):
        assert not isinstance(result, Exception), result
        assert result.is_error is True and message in text_of(result)
    assert not isinstance(edge_text, Exception) and edge_text.is_error is False and text_of(edge_text) == "Saved note 1: T"


def test_e3_search_ignores_case_keeps_id_order_honours_the_limit_and_says_when_nothing_matches():
    async def steps(c):
        for title, text in (("Alpha", "first"), ("beta", "ALPHA again"), ("Gamma", "third"), ("alphabet", "soup")):
            await add(c, title, text)
        return [await attempt(c.call_tool("search_notes", {"query": q, **extra})) for q, extra in (("ALPHA", {}), ("alpha", {"limit": 2}), ("  zeta ", {}), ("third", {"limit": 20}))]
    all_hits, limited, none, one = session(steps)
    assert text_of(all_hits) == "1. Alpha\n2. beta\n4. alphabet"
    assert text_of(limited) == "1. Alpha\n2. beta"
    assert text_of(none) == 'No notes match "zeta"' and not none.is_error
    assert text_of(one) == "3. Gamma"


def test_e4_tool_annotations_tell_a_client_which_tool_only_reads():
    async def steps(c):
        return await attempt(c.list_tools())
    listed = session(steps)
    assert not isinstance(listed, Exception)
    tools = {t.name: t.annotations for t in listed.tools}
    assert tools.get("search_notes") is not None and tools["search_notes"].read_only_hint is True
    assert tools.get("add_note") is not None
    assert tools["add_note"].read_only_hint is False and tools["add_note"].destructive_hint is False and tools["add_note"].idempotent_hint is False


def test_e5_resources_give_the_count_a_note_by_id_and_an_error_for_a_missing_one():
    async def steps(c):
        out = {"templates": await attempt(c.list_resource_templates()), "resources": await attempt(c.list_resources())}
        out["zero"] = await attempt(c.read_resource("notes://count"))
        await add(c, "One")
        out["one"] = await attempt(c.read_resource("notes://count"))
        await add(c, "Two")
        out["two"] = await attempt(c.read_resource("notes://count"))
        out["second"] = await attempt(c.read_resource("notes://note/2"))
        out["missing"] = await attempt(c.read_resource("notes://note/3"))
        out["zero_id"] = await attempt(c.read_resource("notes://note/0"))
        out["word"] = await attempt(c.read_resource("notes://note/abc"))
        return out
    o = session(steps)
    assert not isinstance(o["resources"], Exception) and [r.uri for r in o["resources"].resources] == ["notes://count"]
    assert not isinstance(o["templates"], Exception) and [t.uri_template for t in o["templates"].resource_templates] == ["notes://note/{id}"]
    assert [o[k].contents[0].text if not isinstance(o[k], Exception) else o[k] for k in ("zero", "one", "two")] == ["0 notes", "1 note", "2 notes"]
    assert not isinstance(o["second"], Exception) and o["second"].contents[0].text == "Two\n\nbody"
    for key in ("missing", "zero_id", "word"):
        assert isinstance(o[key], Exception) and "No note" in str(o[key]), key


def test_e6_the_prompt_lists_the_notes_and_defaults_the_tone():
    async def steps(c):
        out = {"listed": await attempt(c.list_prompts()), "empty": await attempt(c.get_prompt("review_notes", {}))}
        await add(c, "Alpha")
        await add(c, "Beta")
        out["default"] = await attempt(c.get_prompt("review_notes", {}))
        out["formal"] = await attempt(c.get_prompt("review_notes", {"tone": "formal"}))
        return out
    o = session(steps)
    assert not isinstance(o["listed"], Exception) and [p.name for p in o["listed"].prompts] == ["review_notes"]
    argument = o["listed"].prompts[0].arguments[0]
    assert argument.name == "tone" and not argument.required
    assert not isinstance(o["empty"], Exception) and o["empty"].messages[0].content.text == "There are no notes to review."
    assert not isinstance(o["default"], Exception) and len(o["default"].messages) == 1 and o["default"].messages[0].role == "user"
    assert o["default"].messages[0].content.text == "Review these notes in a brief tone:\n- Alpha\n- Beta"
    assert not isinstance(o["formal"], Exception) and "in a formal tone" in o["formal"].messages[0].content.text


def test_e7_ids_are_sequential_and_a_failed_call_does_not_use_one():
    async def steps(c):
        return [await add(c, "  First  ", "a"), await add(c, "", "b"), await add(c, "First", "c"), await add(c, "Third", "   "), await add(c, "Fourth", "d"),
                await attempt(c.read_resource("notes://note/1")), await attempt(c.read_resource("notes://count"))]
    first, failed, again, empty, fourth, one, count = session(steps)
    assert text_of(first) == "Saved note 1: First" and failed.is_error is True
    assert text_of(again) == "Saved note 2: First" and empty.is_error is True
    assert text_of(fourth) == "Saved note 3: Fourth"
    assert not isinstance(one, Exception) and one.contents[0].text == "First\n\na"
    assert not isinstance(count, Exception) and count.contents[0].text == "3 notes"
