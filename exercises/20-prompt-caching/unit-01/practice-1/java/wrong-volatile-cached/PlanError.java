/** The request cannot be cached as asked (the API would answer 400, or the plan can never hit). */
class PlanError extends RuntimeException {
    PlanError(String message) {
        super(message);
    }
}
