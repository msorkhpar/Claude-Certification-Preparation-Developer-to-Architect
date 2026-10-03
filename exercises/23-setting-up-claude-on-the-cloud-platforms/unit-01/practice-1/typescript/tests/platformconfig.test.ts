import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution: the folder that holds the three config files.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");

const INVOKE = new Set(["bedrock-mantle:CreateInference", "bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream"]);
// the AWS regions listed for Claude in Amazon Bedrock on 2026-10-02
const REGIONS = new Set(["af-south-1", "ap-northeast-1", "ap-northeast-2", "ap-northeast-3", "ap-south-1", "ap-south-2", "ap-southeast-1", "ap-southeast-2",
  "ap-southeast-3", "ap-southeast-4", "ca-central-1", "ca-west-1", "eu-central-1", "eu-central-2", "eu-north-1", "eu-south-1",
  "eu-south-2", "eu-west-1", "eu-west-2", "eu-west-3", "il-central-1", "me-central-1", "sa-east-1", "us-east-1", "us-east-2",
  "us-west-1", "us-west-2"]);
const VERTEX_MODELS = new Set(["claude-fable-5-1", "claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-4-5@20251001", "claude-sonnet-4-6"]);
const WANTED_ARN = "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5";

const load = (name: string): any => JSON.parse(readFileSync(resolve(dir, name), "utf8"));
const asList = (value: any): any[] => (Array.isArray(value) ? value : value === undefined ? [] : [value]);
const statements = (): any[] => asList(load("bedrock-policy.json").Statement);
const actions = (): string[] => statements().flatMap((s) => asList(s.Action));
const resources = (): string[] => statements().flatMap((s) => asList(s.Resource));

test("m1 the policy allows invoking one model in one region and nothing else", () => {
  const stmts = statements();
  assert.equal(stmts.length, 1);
  assert.equal(stmts[0].Effect, "Allow");
  assert.ok(actions().length > 0 && actions().every((a) => INVOKE.has(a)), `actions: ${actions()}`);
  assert.deepEqual(resources(), [WANTED_ARN]);
});

test("e1 no action is a wildcard and every action is an invoke action", () => {
  assert.ok(actions().length > 0, "the policy has no action");
  for (const action of actions()) {
    assert.ok(!action.includes("*"), action);
    assert.ok(INVOKE.has(action), action);
  }
});

test("e2 every resource ARN names one documented region and one model", () => {
  assert.ok(resources().length > 0, "the policy has no resource");
  for (const arn of resources()) {
    assert.ok(arn !== "*" && !arn.includes("*"), arn);
    const parts = arn.split(":");
    assert.ok(parts.length === 6 && parts[0] === "arn" && parts[1] === "aws" && parts[2] === "bedrock", arn);
    assert.ok(REGIONS.has(parts[3]), arn);
    assert.ok(parts[5].startsWith("foundation-model/anthropic.claude-"), arn);
  }
});

test("e3 every statement allows and the policy uses the current version", () => {
  assert.equal(load("bedrock-policy.json").Version, "2012-10-17");
  assert.ok(statements().length > 0);
  for (const s of statements()) {
    assert.equal(s.Effect, "Allow");
    assert.ok(!("NotAction" in s) && !("NotResource" in s));
  }
});

test("e4 the Vertex role is a custom role that can only predict", () => {
  const role = load("vertex.json").role ?? {};
  assert.ok(["projects", "organizations"].includes(String(role.id ?? "").split("/")[0]), String(role.id));
  assert.deepEqual(role.permissions, ["aiplatform.endpoints.predict"]);
});

test("e5 the Vertex endpoint keeps the data where residency says and serves the model", () => {
  const { endpoint, residency, model } = load("vertex.json");
  assert.ok(VERTEX_MODELS.has(model), String(model));
  if (residency === "eu") assert.ok(endpoint === "eu" || String(endpoint).startsWith("europe-"), String(endpoint));
  if (residency === "us") assert.ok(endpoint === "us" || String(endpoint).startsWith("us-"), String(endpoint));
  if (!["global", "us", "eu"].includes(endpoint)) assert.equal(model, "claude-sonnet-4-6", `${model} is not served on a specific region`);
});

test("e6 model ids use each platforms own form", () => {
  for (const arn of resources()) assert.ok(arn.split("/").at(-1)!.startsWith("anthropic.claude-"), arn);
  const model = String(load("vertex.json").model);
  assert.ok(!model.startsWith("anthropic."), model);
  if (model.startsWith("claude-haiku-4-5")) assert.equal(model, "claude-haiku-4-5@20251001");
});

test("e7 the quota request stays under the self service ceiling", () => {
  const quota = load("quotas.json");
  assert.ok(quota.input_tpm > 0 && quota.output_tpm > 0);
  if (!quota.anthropic_approval) {
    assert.ok(quota.input_tpm <= 5_000_000, String(quota.input_tpm));
    assert.ok(quota.output_tpm <= 500_000, String(quota.output_tpm));
  }
});
