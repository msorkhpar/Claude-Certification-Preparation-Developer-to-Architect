// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { checkDeployment, pickDeployment } from "./dataPolicy.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// The deployment the team runs, and what the requirements ask of it (the tests' compliant pair).
const config = { platform: "api", zdr: true, hipaa_baa: false, model: "claude-sonnet-5-5", inference_geo: "us", region: null,
  tenancy: "workspace-per-tenant", pii_handling: "tokenise", audit: { store_prompts: false, retain_days: 365 } };
const req = { residency: "us", phi: false, zdr_required: true, multi_tenant: true, audit_min_days: 180, audit_max_days: 400 };
console.log("compliant:", checkDeployment(config, req));

// The same deployment checked against stricter requirements: EU residency, and patient data.
const strict = { ...req, residency: "eu", phi: true };
console.log("strict:", checkDeployment(config, strict));

// Which deployment may serve a user in the EU.
const deployments = [{ name: "us-api", residency: "us" }, { name: "eu-api", residency: "eu" }, { name: "global-api", residency: "global" }];
console.log("deployment for an EU user:", pickDeployment("eu", deployments));
