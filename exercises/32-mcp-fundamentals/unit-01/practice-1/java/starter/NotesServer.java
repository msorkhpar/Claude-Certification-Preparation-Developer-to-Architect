// A notes server for the Model Context Protocol, over stdio. See ../../statement.md.
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;

public class NotesServer {
    public static void main(String[] args) throws InterruptedException {
        // TODO: name the server "notes" (version "1.0.0"), keep the notes in memory, and register the tools, resources and prompt of the statement.
        McpServer.sync(new StdioServerTransportProvider(McpJsonDefaults.getMapper())).serverInfo("server", "0.0.0").build();
        Thread.currentThread().join(); // the transport reads stdin on its own threads; keep the process alive until the client closes it
    }
}
