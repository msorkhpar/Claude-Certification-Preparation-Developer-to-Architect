public class Mcp {
    public static String names() throws Exception {
        return Class.forName("io.modelcontextprotocol.server.McpServer").getSimpleName() + " "
             + Class.forName("io.modelcontextprotocol.client.McpClient").getSimpleName();
    }
}
