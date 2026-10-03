"""A notes server for the Model Context Protocol, over stdio. See ../../statement.md."""
from mcp.server.mcpserver import MCPServer

# TODO: name the server "notes" (version "1.0.0"), keep the notes in memory, and register the tools, resources and prompt of the statement.
server = MCPServer("server")

if __name__ == "__main__":
    server.run()
