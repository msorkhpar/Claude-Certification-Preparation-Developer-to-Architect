# Case lists of module 41-security-and-safety: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/41-security-and-safety/unit-01/practice-1"] = {
    "name": "gate", "suite": "GateTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "untrusted text reaches the model only as one json string that says where it came from"),
        ("e1", "edge", "a screen names injection signals and a flagged result is withheld with an error"),
        ("e2", "edge", "reads and writes stay inside the project and away from secrets and protected folders"),
        ("e3", "edge", "bash is limited to a few read only commands and dangerous or chained ones are refused"),
        ("e4", "edge", "fetch and email obey the host and domain lists and refuse credentials in a url"),
        ("e5", "edge", "once untrusted content is in the session anything that changes things asks or is refused"),
        ("e6", "edge", "secrets card numbers and addresses are redacted in text and in the audit"),
        ("e7", "edge", "the hook answer follows the documented shapes and repeated denials raise an alert"),
    ],
    "plants": {
        "wrong-concatenate": (["m1"], "joins the source and the text into one string instead of encoding them as JSON"),
        "wrong-screen-case-sensitive": (["e1"], "matches the override signal only in the exact letter case"),
        "wrong-screen-leaks": (["e1"], "puts the start of the flagged text into the error that withholds it"),
        "wrong-prefix-sibling": (["e2"], "treats a folder whose name merely starts like the project folder as inside it"),
        "wrong-env-variants": (["e2"], "protects .env but not .env.local or the other .env files"),
        "wrong-chain-allowed": (["e3"], "checks the first word only, so a chained or redirected command passes"),
        "wrong-host-suffix": (["e4"], "matches a host by the end of its name without the dot, so a lookalike host passes"),
        "wrong-email-tainted-allowed": (["e5"], "lets an email go out after untrusted content entered the session"),
        "wrong-luhn-skip": (["e6"], "redacts any long number as a card without the Luhn check"),
        "wrong-audit-raw": (["e6"], "writes the arguments to the audit unredacted"),
        "wrong-hook-exit2": (["e7"], "answers a denial with exit code 2 and not with the documented JSON and exit code 0"),
        "wrong-alert-asks": (["e7"], "counts questions as denials when it raises an alert"),
    },
}
