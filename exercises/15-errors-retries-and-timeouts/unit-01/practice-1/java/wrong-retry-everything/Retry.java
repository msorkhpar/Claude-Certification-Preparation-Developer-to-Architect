import java.util.Map;

/** A retry policy for API calls. See ../../statement.md for the contract. */
final class Retry {
    private Retry() {}

    private static Map<?, ?> error(Response response) {
        return response.body() != null && response.body().get("error") instanceof Map<?, ?> e ? e : Map.of();
    }

    private static boolean retryable(Response response) {
        int s = response.status();
        if (s >= 400) {
            return !(error(response).get("details") instanceof Map<?, ?> d && "enforced_spend_limit_reached".equals(d.get("error_code")));
        }
        return false;
    }

    private static CallFailed failure(Response response, int attempts) {
        Object type = error(response).get("type");
        return new CallFailed(response.status(), type == null ? "unknown" : String.valueOf(type), attempts, response.headers().get("request-id"));
    }

    private static double retryAfter(Response response) {
        try {
            return Math.max(0.0, Double.parseDouble(response.headers().get("retry-after")));
        } catch (NullPointerException | NumberFormatException e) {
            return 0.0;
        }
    }

    static Response callWithRetry(Send send, Sleep sleep, Policy policy) {
        for (int attempt = 1; ; attempt++) {
            double wait = 0.0;
            try {
                Response response = send.send();
                if (response.status() < 400) return response;
                if (!retryable(response) || attempt >= policy.maxAttempts()) throw failure(response, attempt);
                wait = retryAfter(response);
            } catch (TransportError e) {
                if (attempt >= policy.maxAttempts()) throw new CallFailed(0, "connection_error", attempt, null);
            }
            double delay = policy.jitter().applyAsDouble(Math.min(policy.cap(), policy.baseDelay() * Math.pow(2, attempt - 1)));
            sleep.sleep(Math.max(delay, wait));
        }
    }
}
