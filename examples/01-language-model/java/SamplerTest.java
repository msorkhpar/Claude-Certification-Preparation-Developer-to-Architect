import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SamplerTest {
    private static int[] draws(long seed, double[] probs) {
        Sampler.Lcg rng = new Sampler.Lcg(seed);
        int[] out = new int[10];
        for (int i = 0; i < out.length; i++) out[i] = Sampler.sample(probs, rng);
        return out;
    }

    @Test
    void probabilitiesSumToOneAtAnyTemperature() {
        for (double t : new double[] {0.1, 1.0, 5.0}) {
            assertEquals(1.0, Arrays.stream(Sampler.softmax(Sampler.LOGITS, t)).sum(), 1e-9);
        }
    }

    @Test
    void lowTemperatureSharpensAndHighFlattens() {
        double low = Sampler.softmax(Sampler.LOGITS, 0.5)[0];
        double mid = Sampler.softmax(Sampler.LOGITS, 1.0)[0];
        double high = Sampler.softmax(Sampler.LOGITS, 2.0)[0];
        assertTrue(low > mid && mid > high);
    }

    @Test
    void greedyIgnoresTemperature() {
        assertEquals(0, Sampler.greedy(Sampler.softmax(Sampler.LOGITS, 0.5)));
        assertEquals(0, Sampler.greedy(Sampler.softmax(Sampler.LOGITS, 2.0)));
    }

    @Test
    void sameSeedSameDrawsDifferentSeedMayDiffer() {
        double[] probs = Sampler.softmax(Sampler.LOGITS, 2.0);
        assertArrayEquals(draws(7, probs), draws(7, probs));
        assertFalse(Arrays.equals(draws(7, probs), draws(8, probs)));
    }
}
