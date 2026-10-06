import java.util.Map;

/** A retry policy for API calls. See ../../statement.md for the contract. */
final class Retry {
    private static final System.Logger LOG = System.getLogger(Retry.class.getName());

    private Retry() {}

    private static Map<?, ?> error(Response response) {
        return response.body() != null && response.body().get("error") instanceof Map<?, ?> e ? e : Map.of();
    }

    private static boolean statusRetryable(int s) {
        return s == 408 || s == 409 || s == 429 || s >= 500;
    }

    private static boolean spendCap(Response response) {
        return error(response).get("details") instanceof Map<?, ?> d && "enforced_spend_limit_reached".equals(d.get("error_code"));
    }

    private static boolean retryable(Response response) {
        return statusRetryable(response.status()) && !spendCap(response);
    }

    private static String errorType(Response response) {
        Object type = error(response).get("type");
        return type == null ? "unknown" : String.valueOf(type);
    }

    private static String requestId(Response response) {
        return response.headers().get("request-id");
    }

    private static CallFailed failure(Response response, int attempts) {
        return new CallFailed(response.status(), errorType(response), attempts, requestId(response));
    }

    private static CallFailed connectionFailure(int attempts) {
        return new CallFailed(0, "connection_error", attempts, null);
    }

    private static double retryAfter(Response response) {
        try {
            return Math.max(0.0, Double.parseDouble(response.headers().get("retry-after")));
        } catch (NullPointerException | NumberFormatException e) {
            return 0.0;
        }
    }

    private static double delayFor(int attempt, Policy policy) {
        return policy.jitter().applyAsDouble(Math.min(policy.cap(), policy.baseDelay() * Math.pow(2, attempt - 1)));
    }

    static Response callWithRetry(Send send, Sleep sleep, Policy policy) {
        LOG.log(System.Logger.Level.DEBUG, "callWithRetry input: {0} {1} {2}", policy.maxAttempts(), policy.baseDelay(), policy.cap());
        for (int attempt = 1; ; attempt++) {
            double wait = 0.0;
            try {
                Response response = send.send();
                if (response.status() < 400) return response;
                if (!retryable(response) || attempt >= policy.maxAttempts()) throw failure(response, attempt);
                wait = retryAfter(response);
            } catch (TransportError e) {
                if (attempt >= policy.maxAttempts()) throw connectionFailure(attempt);
            }
            sleep.sleep(Math.max(delayFor(attempt, policy), wait));
        }
    }
}
