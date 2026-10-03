// A notes server for the Model Context Protocol, over stdio. See ../../statement.md.
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

// TODO: name the server "notes" (version "1.0.0"), keep the notes in memory, and register the tools, resources and prompt of the statement.
const server = new McpServer({ name: "server", version: "0.0.0" });

await server.connect(new StdioServerTransport());
