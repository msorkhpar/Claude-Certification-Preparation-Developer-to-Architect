"""Hand-written, illustrative scripted model: replies in the Messages API shape."""
import copy


def text(t):
    return {"type": "text", "text": t}


def tool_use(id_, name, **inp):
    return {"type": "tool_use", "id": id_, "name": name, "input": inp}


def reply(blocks, stop_reason):
    return {"id": "msg_illustrative", "type": "message", "role": "assistant",
            "model": "illustrative", "content": blocks, "stop_reason": stop_reason,
            "usage": {"input_tokens": 0, "output_tokens": 0}}


class ScriptedModel:
    def __init__(self, *replies):
        self.replies = list(replies)
        self.seen = []  # a deep copy of the history at each call

    def __call__(self, messages):
        self.seen.append(copy.deepcopy(messages))
        if not self.replies:
            raise AssertionError("scripted model ran out of replies")
        return self.replies.pop(0)
