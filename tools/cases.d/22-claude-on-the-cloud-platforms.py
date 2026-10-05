# Case lists of module 22-claude-on-the-cloud-platforms: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/22-claude-on-the-cloud-platforms/unit-01/practice-1"] = {
    "name": "platforms", "suite": "PlatformsTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the same message takes three shapes"),
        ("e1", "edge", "model ids change with the platform"),
        ("e2", "edge", "vertex moves the model into the url and the version into the body"),
        ("e3", "edge", "vertex endpoints global multi region and regional"),
        ("e4", "edge", "a platform serves only its own models"),
        ("e5", "edge", "each platform lacks its own features"),
        ("e6", "edge", "a request needs the place it is sent to"),
    ],
    "plants": {
        "wrong-vertex-model-in-body": (["e2"], "leaves the model in the Vertex body"),
        "wrong-vertex-version-header": (["m1", "e2"], "sends the Vertex version as the anthropic-version header instead of a body field"),
        "wrong-bedrock-direct-id": (["e1"], "sends the direct API model id to Bedrock"),
        "wrong-regional-any": (["e3"], "allows any model on a specific Vertex region"),
        "wrong-batches-everywhere": (["e5"], "treats the Message Batches API as available on both clouds"),
        "wrong-body-mutated": (["e2"], "removes the model from the caller's own body"),
    },
}
