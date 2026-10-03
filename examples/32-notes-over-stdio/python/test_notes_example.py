import asyncio

from mcp import Client

from notes_example import build_server, words


def run(steps):
    async def go():
        async with Client(build_server()) as client:  # the SDK connects a client to a server object in the same process
            return await steps(client)
    return asyncio.run(go())


def test_a_blank_title_is_a_tool_error_and_does_not_use_an_id():
    async def steps(c):
        return [await c.call_tool("add_note", {"title": " ", "text": "x"}), await c.call_tool("add_note", {"title": "A", "text": "x"})]
    bad, good = run(steps)
    assert bad.is_error is True and "title is required" in words(bad)
    assert good.is_error is False and words(good) == "Saved note 1: A"


def test_search_is_case_insensitive_and_says_when_nothing_matches():
    async def steps(c):
        await c.call_tool("add_note", {"title": "Plan", "text": "x"})
        return [words(await c.call_tool("search_notes", {"query": "PLAN"})), words(await c.call_tool("search_notes", {"query": "zzz"}))]
    assert run(steps) == ["1. Plan", 'No notes match "zzz"']


def test_the_count_resource_and_the_prompt_follow_the_notes():
    async def steps(c):
        before = (await c.read_resource("notes://count")).contents[0].text
        await c.call_tool("add_note", {"title": "A", "text": "x"})
        return before, (await c.read_resource("notes://count")).contents[0].text, (await c.get_prompt("review_notes", {"tone": "formal"})).messages[0].content.text
    assert run(steps) == ("0 notes", "1 note", "Review these notes in a formal tone:\n- A")
