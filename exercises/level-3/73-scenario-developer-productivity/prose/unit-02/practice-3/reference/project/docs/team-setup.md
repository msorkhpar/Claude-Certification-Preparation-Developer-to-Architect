# Team setup

## Environment variables

Set these in your shell before you start Claude Code. Never commit a value.

- `TICKETS_URL`: the ticket server's address (optional, `.mcp.json` sets a default)
- `TICKETS_TOKEN`: your own access token for the ticket server

## Which session to start

| Situation | Session |
|---|---|
| Continue yesterday's migration; the files are as they were | resume |
| Compare two designs that start from the same analysis | fork |
| The code was rewritten since the last session | fresh |
