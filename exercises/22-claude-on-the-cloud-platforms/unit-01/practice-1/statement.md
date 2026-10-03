# Practice: one request, three front doors

The same Messages request can go to the direct API, to Claude in Amazon Bedrock or to Claude on Google Vertex AI. The
body is nearly the same; the URL, the model id, where the version goes and the features available are not. Write the
function that builds the request for each platform and the function that tells a team which features it would lose. Pick
your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there. The shapes come
from the Claude documentation pages for Amazon Bedrock and Google Cloud, read on 2026-10-02; the lesson pages explain
them. Nothing here signs a request or touches the network: authentication (AWS SigV4, a Google access token or an API key)
is added by an SDK or a proxy and is not part of what you build.

## The given data

`build_request(platform, model, body, config)` takes `platform` (`anthropic`, `bedrock` or `vertex`), `model` (the id on the
direct API, for example `claude-opus-5-5` or `claude-haiku-4-5-20251001`), `body` (a Messages request body) and `config`
(a map with `region` for Bedrock, `project` and `endpoint` for Vertex). It returns a map with `method`, `url`, `headers`
(without any credential) and `body`, and raises `PlatformError` (`field` names the offending part: `platform`, `model`,
`config`, `endpoint` or `feature`). The four course models and `claude-sonnet-4-6` are the only models to handle.

## `build_request`

1. **Direct API:** `POST https://api.anthropic.com/v1/messages`, headers `anthropic-version: 2023-06-01` and
   `content-type: application/json`, the model in the body.
2. **Bedrock:** `POST https://bedrock-mantle.<region>.api.aws/anthropic/v1/messages`, the same two headers, and the model
   in the body as the Bedrock id: `anthropic.` followed by the model name without a date (`anthropic.claude-opus-5-5`,
   `anthropic.claude-haiku-4-5` for both `claude-haiku-4-5-20251001` and `claude-haiku-4-5`). Bedrock's Messages endpoint
   serves the Fable 5.1, Opus 5.5, Sonnet 5.5 and Haiku 4.5 models here; `claude-sonnet-4-6` is `model`. A missing or empty
   `region` is `config`.
3. **Vertex:** the model is **not** in the body but in the URL, and the version is **not** a header but a body field
   `anthropic_version` with the value `vertex-2023-10-16`; the only header is `content-type`. The URL is
   `https://<host>/v1/projects/<project>/locations/<endpoint>/publishers/anthropic/models/<model id>:rawPredict`. The
   endpoint (default `global`) picks the host: `global` is `aiplatform.googleapis.com`; the multi-region endpoints `us` and
   `eu` are `aiplatform.us.rep.googleapis.com` and `aiplatform.eu.rep.googleapis.com`; any other value is a specific
   region and the host is `<region>-aiplatform.googleapis.com`, which serves `claude-sonnet-4-6` only (any other model
   with a specific region is `endpoint`). The Vertex model id is the model name, with `claude-haiku-4-5@20251001` for
   Haiku 4.5. A missing or empty `project` is `config`. The five models above are served; any other is `model`.
4. An unknown platform is `platform`. Never change the `body` you were given.

## `unsupported_features(platform, features)` (TypeScript `unsupportedFeatures`, Java `Platforms.unsupportedFeatures`, Kotlin `unsupportedFeatures`)

Return the features of the list that the platform lacks, in the order given; an unknown platform or feature is a
`PlatformError`. The direct API lacks none. Bedrock lacks `batches` (the Message Batches API), `fast_mode`, `files_api`,
`web_search`, `web_fetch`, `code_execution`, `mcp_connector`, `structured_outputs`, `skills` and `managed_agents`. Vertex
lacks `batches`, `fast_mode`, `files_api`, `web_fetch`, `code_execution`, `mcp_connector`, `skills` and `managed_agents`.
The other names (`prompt_caching`, `thinking`, `tool_use`, `citations`) are available everywhere.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The same message takes three shapes: URL, headers and model place for each platform |
| `e1` | Model ids change with the platform |
| `e2` | Vertex moves the model into the URL and the version into the body, and the caller's body is never changed |
| `e3` | Vertex global, multi-region and regional endpoints, and which models a regional endpoint serves |
| `e4` | A platform serves only its own models |
| `e5` | Each platform lacks its own features |
| `e6` | A request needs the place it is sent to (region or project) |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
