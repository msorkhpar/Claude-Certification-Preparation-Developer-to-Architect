/** No JSON object could be read from the model's text. */
class ParseError extends RuntimeException {
    ParseError(String message) {
        super(message);
    }
}
