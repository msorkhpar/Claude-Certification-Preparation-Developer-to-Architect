import { test } from "node:test";
import assert from "node:assert/strict";
import { commandName, forkAgent, invocation, parse, preApproved, removed, render, toolStatus, winner } from "./skillModel.ts";

const META = { "allowed-tools": "Bash(git tag *) Bash(git push origin *)", "disallowed-tools": "Edit Write(src/**)" };

test("a command and a skill with one name create one slash command", () => {
  assert.equal(commandName(".claude/commands/deploy.md", {}), "deploy");
  assert.equal(commandName(".claude/skills/deploy/SKILL.md", {}), "deploy");
  assert.equal(commandName(".claude/skills/deploy/SKILL.md", { name: "ship" }), "ship");
});

test("the higher level wins a shared name", () => {
  assert.equal(winner({ project: "p", personal: "u", enterprise: "e" }), "e");
  assert.equal(winner({ project: "p", personal: "u" }), "u");
  assert.equal(winner({ project: "p" }), "p");
  assert.equal(winner({}), null);
});

test("who can start a skill", () => {
  assert.deepEqual(invocation({}), { you: true, claude: true, description_in_context: true });
  assert.deepEqual(invocation({ "disable-model-invocation": true }), { you: true, claude: false, description_in_context: false });
  assert.deepEqual(invocation({ "user-invocable": false }), { you: false, claude: true, description_in_context: true });
});

test("allowed tools pre-approves patterns and a bare name approves everything", () => {
  assert.ok(preApproved(META, "Bash", "git tag v1") && preApproved(META, "Bash", "git push origin v1"));
  assert.ok(!preApproved(META, "Bash", "git push --force") && !preApproved(META, "Bash", "rm -rf build"));
  assert.ok(preApproved({ "allowed-tools": "Bash" }, "Bash", "rm -rf build") && preApproved({ "allowed-tools": "Read, Grep" }, "Grep"));
});

test("only a bare disallowed name removes a tool", () => {
  assert.ok(removed(META, "Edit") && !removed(META, "Write") && !removed({ "disallowed-tools": "Edit(src/**)" }, "Edit"));
  assert.equal(toolStatus(META, "Edit"), "removed");
  assert.equal(toolStatus(META, "Bash", "git tag v1"), "pre-approved");
  assert.equal(toolStatus(META, "Read"), "permission settings decide");
});

test("arguments fill placeholders in shell style", () => {
  assert.equal(render("pr $0 by $1", "123 ana"), "pr 123 by ana");
  assert.equal(render("first=$ARGUMENTS[0] all=$ARGUMENTS", '"hello world" second'), 'first=hello world all="hello world" second');
  assert.equal(render("tag $version on $branch", "v2 main", ["version", "branch"]), "tag v2 on main");
  assert.equal(render("only $1", "a"), "only $1");
  assert.equal(render("tag $version on $branch", "v2", ["version", "branch"]), "tag v2 on ");
});

test("input that no placeholder receives is appended", () => {
  assert.equal(render("Review the change.\n", "123"), "Review the change.\nARGUMENTS: 123\n");
  assert.equal(render("Review the change.\n", ""), "Review the change.\n");
  assert.equal(render("Tag $version\n", "v1", ["version"]), "Tag v1\n");
});

test("a forked skill runs in a subagent that defaults to general purpose", () => {
  assert.equal(forkAgent({ context: "fork" }), "general-purpose");
  assert.equal(forkAgent({ context: "fork", agent: "Explore" }), "Explore");
  assert.equal(forkAgent({}), null);
});

test("the frontmatter is split from the body", () => {
  const [meta, body] = parse("---\nname: x\narguments: [a, b]\n---\nHello $a\n");
  assert.deepEqual(meta, { name: "x", arguments: ["a", "b"] });
  assert.equal(body, "Hello $a\n");
});
