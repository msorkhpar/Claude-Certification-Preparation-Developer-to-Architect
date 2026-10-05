# Case lists of module 83-deployment-architecture-and-data-handling: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/83-deployment-architecture-and-data-handling/unit-01/practice-1"] = {
    "name": "data_policy", "suite": "DataPolicyTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a compliant deployment has no findings"),
        ("e1", "edge", "residency is pinned by the request or by the region the platform allows"),
        ("e2", "edge", "zero data retention is an arrangement of the providers own platforms and not of every model"),
        ("e3", "edge", "protected health information needs a covered platform an agreement and deidentified input"),
        ("e4", "edge", "a multi tenant service needs a workspace for each tenant"),
        ("e5", "edge", "an audit log must not store prompts that hold sensitive data"),
        ("e6", "edge", "audit retention stays between the minimum and the maximum"),
        ("e7", "edge", "purge only what is past the retention limit and not on hold"),
        ("e8", "edge", "a request is served only by a deployment that keeps its data in the region"),
    ],
    "plants": {
        "wrong-geo-unset-passes": (["e1"], "treats a request with no inference geo as pinned to the US"),
        "wrong-eu-pin-allowed": (["e1"], "accepts a European residency requirement on the first-party API"),
        "wrong-any-region-for-us": (["e1"], "accepts any cloud region for a US residency requirement"),
        "wrong-cloud-zdr-ok": (["e2"], "treats the zero data retention flag as valid on a cloud provider's platform"),
        "wrong-fable-zdr-ok": (["e2"], "ignores that the top-tier model needs 30-day retention"),
        "wrong-baa-optional": (["e3"], "does not ask for a signed agreement for protected health information on the API"),
        "wrong-aws-hipaa-ok": (["e3"], "accepts protected health information on a platform without HIPAA readiness"),
        "wrong-redact-not-safe": (["e3"], "accepts only tokenising and rejects redaction as de-identification"),
        "wrong-tenancy-ignored": (["e4"], "does not check the isolation of tenants"),
        "wrong-audit-phi-only": (["e5"], "flags stored prompts only when health data is in scope and not for other identifiers"),
        "wrong-retention-long-inclusive": (["e6"], "flags a retention equal to the maximum as too long"),
        "wrong-retention-short-inclusive": (["e6"], "flags a retention equal to the minimum as too short"),
        "wrong-purge-held": (["e7"], "purges an entry that is on legal hold"),
        "wrong-purge-boundary": (["e7"], "purges an entry that is exactly at the limit"),
        "wrong-global-fallback": (["e8"], "serves a request from a global deployment when the regional one is missing"),
        "wrong-pick-first-listed": (["e8"], "picks the first listed deployment and not the first by name"),
    },
}
