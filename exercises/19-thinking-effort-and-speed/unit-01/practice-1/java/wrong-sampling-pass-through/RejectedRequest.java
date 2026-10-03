/** The API would answer 400. param() names the offending parameter. */
class RejectedRequest extends RuntimeException {
    private final String param;

    RejectedRequest(String param, String reason) {
        super(param + ": " + reason);
        this.param = param;
    }

    String param() {
        return param;
    }
}
