/** A tool refuses or fails; its message goes back to the model as an error result. */
class ToolError extends RuntimeException {
    ToolError(String message) {
        super(message);
    }
}
