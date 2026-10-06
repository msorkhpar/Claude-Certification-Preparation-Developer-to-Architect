"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from data_policy import check_deployment, pick_deployment

# The deployment the team runs, and what the requirements ask of it (the tests' compliant pair).
config = {"platform": "api", "zdr": True, "hipaa_baa": False, "model": "claude-sonnet-5-5", "inference_geo": "us", "region": None,
          "tenancy": "workspace-per-tenant", "pii_handling": "tokenise", "audit": {"store_prompts": False, "retain_days": 365}}
req = {"residency": "us", "phi": False, "zdr_required": True, "multi_tenant": True, "audit_min_days": 180, "audit_max_days": 400}
print("compliant:", check_deployment(config, req))

# The same deployment checked against stricter requirements: EU residency, and patient data.
strict = {**req, "residency": "eu", "phi": True}
print("strict:", check_deployment(config, strict))

# Which deployment may serve a user in the EU.
deployments = [{"name": "us-api", "residency": "us"}, {"name": "eu-api", "residency": "eu"}, {"name": "global-api", "residency": "global"}]
print("deployment for an EU user:", pick_deployment("eu", deployments))
