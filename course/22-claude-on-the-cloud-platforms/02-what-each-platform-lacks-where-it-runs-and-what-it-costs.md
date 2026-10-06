# What each platform lacks, where it runs and what it costs

**Level:** Developer · **Module 22:** Claude on the cloud platforms · **Page 2 of 2**
**Exams:** DV2

**After this page you can** list the features Bedrock and Google Cloud do not offer, choose between a global, multi-region and regional
endpoint, say what the regional choice costs, and write the function that builds a platform's request and reports the features a team
would lose.

Checked against the Claude API documentation (Claude in Amazon Bedrock, Claude on Google Cloud, Prompt caching, Fast mode) on
2026-10-02, and by running the practice offline in the course container in Python, TypeScript, Java and Kotlin.

## Why it matters

A cloud platform is chosen for its security and billing, and the feature list is discovered later, usually in the middle of a
build. A team that planned on batches, structured outputs or a server-side tool finds out on the first call. The exam asks which
platform lacks which feature and what the endpoint choice does to price and data location.

## The idea

### What Bedrock does not offer

The Bedrock page lists the gaps under "Features not supported". These are the entries that matter for this course:

- Structured outputs.
- Input sources: "URL sources for images and documents, Files API".
- "Server-side tools (code execution, web search, web fetch, advisor)".
- Agent infrastructure: "Agent Skills, MCP connector, programmatic tool calling".
- "API endpoints (Message Batches, Models, Admin, Compliance, Usage and Cost)".
- Claude Managed Agents.
- Server-side fallback: the page says to use the client-side pattern instead.

What it does offer, in the page's highlight list: the Messages API at `/anthropic/v1/messages`, prompt caching, thinking, tool use with the
bash, computer use, memory and text editor tools, and citations. The batch gap matters most for cost: the 50 percent discount of
module 21 belongs to the Message Batches API, and Bedrock has no such endpoint on this integration.

### What Google Cloud does not offer

The Google Cloud page has a shorter list. It lacks "Input sources (URL sources for images and documents, Files API)", the server-side
tools "code execution, web fetch, advisor", the agent infrastructure, the "API endpoints (Message Batches, Models, Admin, Compliance, Usage and
Cost)" and Claude Managed Agents. It does offer more than Bedrock in two places: the page lists "Structured outputs" and the "Web
search tool" among its supported feature highlights, next to prompt caching, thinking, tool use and citations.

One more gap spans the partner platforms. The fast mode of module 19 "is available on the Claude API (first-party) only; it is not
available on Claude Platform on AWS or partner-operated cloud platforms". The practice lumps it with the other gaps.

| Feature | Direct API | Bedrock | Google Cloud |
|---|---|---|---|
| Messages, prompt caching, thinking, tool use, citations | yes | yes | yes |
| Message Batches API | yes | no | no |
| Fast mode | yes | no | no |
| Files API | yes | no | no |
| Web search | yes | no | yes |
| Structured outputs | yes | no | yes |
| Code execution, web fetch | yes | no | no |
| MCP connector, Agent Skills | yes | no | no |

### Where it runs: endpoints and price

Google Cloud offers three endpoint types: "Global endpoints: Dynamic routing for maximum availability", "Multi-region endpoints" that route
within a geography (currently `us` and `eu`), and "Regional endpoints" that guarantee routing through specific regions. The price rule is
one sentence: "Regional and multi-region endpoints include a 10% pricing premium over global endpoints." The page recommends the global
kind. Its summary of when to use each:

- Global: "Dynamically route requests to regions with available capacity" and "No pricing premium". It "only supports pay-as-you-go traffic
  (provisioned throughput requires regional endpoints)".
- Multi-region: data residency within a broad geography, with higher availability than one region, at the premium. It also supports
  pay-as-you-go traffic only.
- Regional: "Required for single-region data residency, strict compliance mandates, or provisioned throughput", at the premium.

A detail the practice enforces: the specific regional endpoints serve only older models. The page's own comment reads "Specific regional
endpoints support Claude Sonnet 4.6 and earlier; newer models use the global or multi-region endpoints".

Bedrock has the same idea with different words. A global endpoint gives "dynamic routing across all available regions for maximum availability.
No pricing premium." A regional one resolves to a single AWS region for data residency, and "Regional endpoints carry a 10% pricing premium over
global endpoints." To route across several regions of a geography, use an inference profile (US, EU, JP or AU). The region you name in the
Bedrock URL, as in `bedrock-mantle.us-east-1.api.aws`, is part of the request, and the practice treats a missing one as an error.

### Quotas, limits and records

Two practical figures: Bedrock's "Default quota is 2 million input tokens per minute (TPM)." and "You can request up to 5 million input TPM and
500,000 output TPM without additional Anthropic approval." The page adds that "AWS enforces requests-per-minute (RPM) limits on the Bedrock side".
Google Cloud "limits request payloads to 30 MB", which a team with many images can reach before the token limit. Both pages recommend
logging activity "on at least a 30-day rolling basis", through CloudWatch and CloudTrail on AWS and request-response logging on
Google Cloud.

Caching differs in one place. The prompt caching page says caches are isolated per workspace on the Claude API but that "Bedrock and Google Cloud
maintain organization-level cache isolation". The prompt caching page lists the one-hour lifetime as available
on both Amazon Bedrock and Google Cloud, and sends Bedrock readers to AWS for the per-model minimums.

## The practice: one request, three front doors

You write `build_request(platform, model, body, config)` and `unsupported_features(platform, features)`. The first returns the method, URL, headers
(no credential) and body for the platform, and raises `PlatformError` with a `field` that names the offending part: `platform`, `model`,
`config`, `endpoint` or `feature`. The rules are the ones above: on Vertex the model goes in the URL and the version in the body field
`anthropic_version`; on Bedrock the model gets its `anthropic.` prefix and a region is required. Dated direct ids map to the platform forms:
`claude-haiku-4-5-20251001` becomes `anthropic.claude-haiku-4-5` on Bedrock and `claude-haiku-4-5@20251001` on Vertex. An unsupported model and
endpoint pair is a `PlatformError`: the practice says a specific region serves `claude-sonnet-4-6` only. The statement is in `exercises/22-claude-on-the-cloud-platforms/unit-01/practice-1/statement.md`. Each language
folder has a `starter`, the tests and a build file, and the starter fails every test. Nothing is signed and nothing leaves the container.

| Id | What it checks |
|---|---|
| `m1` | The same message takes three shapes: URL, headers and model place for each platform |
| `e1` | Model ids change with the platform |
| `e2` | Vertex moves the model into the URL and the version into the body, and the caller's body is never changed |
| `e3` | Vertex global, multi-region and regional endpoints, and which models a regional endpoint serves |
| `e4` | A platform serves only its own models |
| `e5` | Each platform lacks its own features |
| `e6` | A request needs the place it is sent to (region or project) |

Case `e2` includes the check that the caller's body is untouched: a builder that deletes `model` from the body it was given to prepare a
Vertex request damages the next call that reuses it.

## Traps

1. **Planning on batches, files or web fetch before checking the platform.** The gaps above are on the platform's own page, and a
   team finds them on the first call if it has not read them.
2. **Reading the global endpoint's low price as the only difference.** The regional kind costs 10 percent more, and it is
   also the only one with provisioned throughput.
3. **Assuming Bedrock and Google Cloud have the same feature set.** Web search and structured outputs are on Google Cloud and not on Bedrock.

## Quiz

1. A team on Amazon Bedrock plans nightly bulk jobs through the Message Batches API at half price. What should it budget?
   - **a**: Batch jobs on Bedrock, at the same half price as on the direct API
   - **b**: Ordinary one-by-one calls, at the usual per-token rate with no discount
   - **c**: Ordinary one-by-one calls, at half price once a global endpoint is chosen
   - **d**: Batch jobs on Bedrock, with the inputs uploaded through the Files API

2. A company must keep its Claude traffic on Google Cloud inside one named region and also wants provisioned throughput. Which
   type of address fits?
   - **a**: A regional kind for residency, with the global kind for throughput
   - **b**: The global kind, since it routes to wherever capacity is free today
   - **c**: The multi-region kind, since it supports provisioned throughput as well
   - **d**: The regional kind, since it serves both of the needs at a 10% premium

3. A team must pick between the two partner platforms and needs structured outputs and web search from it. What do the pages show?
   - **a**: Bedrock offers web search only, while Google Cloud offers both of them
   - **b**: Bedrock offers neither one, while Google Cloud offers both of them
   - **c**: Both offer the two features, since each takes the same request body
   - **d**: Google Cloud offers structured outputs only, while Bedrock offers both

<details>
<summary>Answer key</summary>

1. **b**. The Bedrock page lists "API endpoints (Message Batches, Models, Admin, Compliance, Usage and Cost)" under features not supported, and the discount "belongs to the Message Batches API". *a* is ruled out because "Bedrock has no such endpoint on this integration". *c* is ruled out because the global kind removes only the premium, not half the price: "dynamic routing across all available regions for maximum availability. No pricing premium." *d* is ruled out because the Files API is itself on the list: "URL sources for images and documents, Files API".
2. **d**. The page says regional endpoints are "Required for single-region data residency, strict compliance mandates, or provisioned throughput", at the premium. *b* is ruled out because the global kind will "Dynamically route requests to regions with available capacity", so traffic leaves the one region. *c* is ruled out because the multi-region kind also "supports pay-as-you-go traffic only". *a* is ruled out because the global kind "only supports pay-as-you-go traffic (provisioned throughput requires regional endpoints)", so the reserved capacity cannot sit there.
3. **b**. The page says "Web search and structured outputs are on Google Cloud and not on Bedrock." *a* is ruled out because Bedrock's list has "Server-side tools (code execution, web search, web fetch, advisor)" as missing. *c* is ruled out because a shared body does not mean shared features: Google Cloud "does offer more than Bedrock in two places". *d* is ruled out because the Google Cloud page lists both "among its supported feature highlights", and Bedrock lists structured outputs under its gaps.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team moves its Bedrock traffic from the global endpoint to a regional one for data residency. The same tokens cost 1,000
   dollars a month before. What should it budget now?
   - **a**: About 1,000, since the same models serve both of the routes
   - **b**: About 1,100, since that route is priced above the dynamic one
   - **c**: About 900, since the global route was the one with the premium
   - **d**: About 500, since that route runs on spare capacity at half price

2. build_request is called for Vertex with claude-opus-5-5 and a single named region. What happens?
   - **a**: It raises PlatformError for the endpoint field, as newer models use other routes
   - **b**: It builds the URL with the region host, as every model is served by those hosts
   - **c**: It falls back to the worldwide host without complaint and returns the request
   - **d**: It raises PlatformError for the model field, as that model id is not known

3. A team on the direct API keeps two products in separate workspaces so that their prompt caches never mix, and now plans to
   move both to Bedrock. What should it expect there?
   - **a**: Isolation is per organisation there, so the split stops holding them apart
   - **b**: Caching is missing there, so each call pays the full input price once more
   - **c**: Isolation stays per workspace there, just as it is on the first-party door
   - **d**: Isolation is unchanged, but entries lapse sooner without the one-hour lifetime

4. A team on Bedrock must keep its traffic in Japan, and the outage of a single AWS region must not stop it. Which route fits?
   - **a**: A global endpoint, which routes to whichever region has capacity
   - **b**: A regional endpoint, which resolves to one AWS location for residency
   - **c**: A JP inference profile, which spreads calls across that geography
   - **d**: A multi-region endpoint, which spans several regions of an area

<details>
<summary>Answer key</summary>

1. **b**. The page says "Regional endpoints carry a 10% pricing premium over global endpoints.", so 1,000 becomes about 1,100. *a* is ruled out because "Bedrock has the same idea with different words" as Google Cloud, where regional endpoints "include a 10% pricing premium over global endpoints". *c* is ruled out because a global endpoint gives "dynamic routing across all available regions for maximum availability" with no premium. *d* is ruled out because half price is a different route: "the 50 percent discount of module 21 belongs to the Message Batches API".
2. **a**. The page's comment is "Specific regional endpoints support Claude Sonnet 4.6 and earlier; newer models use the global or multi-region endpoints", and the practice says a specific region serves `claude-sonnet-4-6` only. *b* is ruled out because "the specific regional endpoints serve only older models". *c* is ruled out because the practice states "An unsupported model and endpoint pair is a `PlatformError`". *d* is ruled out because "a specific region serves `claude-sonnet-4-6` only", so the model is known and the endpoint is the fault.
3. **a**. The page says caches are isolated per workspace on the Claude API but that "Bedrock and Google Cloud maintain organization-level cache isolation", so the workspace split no longer keeps the two caches apart. *b* is ruled out because Bedrock's highlight list includes "prompt caching, thinking, tool use with the bash" tools. *c* is ruled out because per-workspace isolation is named for the direct API only: "caches are isolated per workspace on the Claude API". *d* is ruled out because the caching page "lists the one-hour lifetime as available" on both Amazon Bedrock and Google Cloud.
4. **c**. The page says "To route across several regions of a geography, use an inference profile (US, EU, JP or AU)", so a JP profile keeps the calls in Japan and survives the loss of one region. *a* is ruled out because a global endpoint gives "dynamic routing across all available regions for maximum availability", so calls leave Japan. *b* is ruled out because a regional endpoint "resolves to a single AWS region for data residency", so one outage stops it. *d* is ruled out because multi-region endpoints are the Google Cloud term, and "Bedrock has the same idea with different words".

</details>
