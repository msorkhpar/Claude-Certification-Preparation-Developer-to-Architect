import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main(args: Array<String>) {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }


    // The tool functions of the server are plain functions, so you can call them here without starting a client.
    // (The tests start the server as a separate process and connect the SDK's client to it.)
    fun textOf(result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult) = result.content.filterIsInstance<TextContent>().joinToString("") { it.text }

    val saved = addNote(buildJsonObject { put("title", "Plan"); put("text", "ship it") })
    println("add_note: ${textOf(saved)}")
    val found = searchNotes(buildJsonObject { put("query", "ship") })
    println("search_notes: ${textOf(found)}")
    println("count resource: ${countText(notes.size)}")
    println("note 1 resource: ${noteText("1")}")
}
