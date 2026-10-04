import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** A toy next-token sampler. It is not Claude: it only shows what temperature does. */
public final class Sampler {
    static final String[] TOKENS = {"blue", " clear", " falling", "green"};
    static final double[] LOGITS = {4.0, 2.5, 1.0, -1.0};

    /** Turn scores into probabilities. Lower temperature sharpens, higher flattens. */
    static double[] softmax(double[] logits, double temperature) {
        double[] scaled = new double[logits.length];
        double top = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < logits.length; i++) {
            scaled[i] = logits[i] / temperature;
            top = Math.max(top, scaled[i]);
        }
        double total = 0;
        double[] exps = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exps[i] = Math.exp(scaled[i] - top);
            total += exps[i];
        }
        for (int i = 0; i < exps.length; i++) exps[i] /= total;
        return exps;
    }

    /** A tiny seeded random generator, the same in every language of this course. */
    static final class Lcg {
        private long state;

        Lcg(long seed) {
            state = Math.floorMod(seed, 1L << 32);
        }

        double next() {
            state = (state * 1664525L + 1013904223L) & 0xFFFFFFFFL;
            return state / 4294967296.0;
        }
    }

    static int sample(double[] probs, Lcg rng) {
        double u = rng.next();
        double acc = 0.0;
        for (int i = 0; i < probs.length; i++) {
            acc += probs[i];
            if (u < acc) return i;
        }
        return probs.length - 1;
    }

    static int greedy(double[] probs) {
        int best = 0;
        for (int i = 1; i < probs.length; i++) if (probs[i] > probs[best]) best = i;
        return best;
    }

    public static void main(String[] args) {
        for (double t : new double[] {0.5, 1.0, 2.0}) {
            double[] probs = softmax(LOGITS, t);
            List<String> cells = new ArrayList<>();
            for (int i = 0; i < probs.length; i++) cells.add(String.format(Locale.ROOT, "%s=%.3f", TOKENS[i].strip(), probs[i]));
            System.out.println("T=" + t + ": " + String.join("  ", cells));
        }
        System.out.println("greedy: " + TOKENS[greedy(softmax(LOGITS, 1.0))]);
        for (double t : new double[] {0.2, 1.0, 2.0}) {
            double[] probs = softmax(LOGITS, t);
            Lcg rng = new Lcg(7);
            List<String> picks = new ArrayList<>();
            for (int n = 0; n < 10; n++) picks.add(TOKENS[sample(probs, rng)].strip());
            System.out.println("T=" + t + " ten draws: " + picks.stream().collect(Collectors.joining(" ")));
        }
    }
}
