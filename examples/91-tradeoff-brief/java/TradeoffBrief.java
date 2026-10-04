import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One decision told to two audiences: the figures an engineer needs, the same figures in the words a sponsor decides with, and an honest check of each service level at its exact edge.
 *
 * The figures are invented for a utility's billing-dispute assistant; the break-even rule is the one of module 79. Nothing here calls a model.
 */
public class TradeoffBrief {
    record Sla(String name, int limit, String direction, String unit) {}

    record Segment(String name, int right, int total, int errorCost) {}

    record Measured(Sla sla, int value) {}

    static String group(long n) {
        return String.format(java.util.Locale.US, "%,d", n);
    }

    /** Whole percent, halves rounded up, and 0 for no cases. */
    static int pct(int right, int total) {
        return total == 0 ? 0 : (200 * right + total) / (2 * total);
    }

    /** The accuracy, in whole percent, at or above which a check no longer pays: (1 - accuracy) x error cost <= review cost. */
    static int breakEven(int errorCost, int reviewCost) {
        return 100 - (100 * reviewCost + errorCost - 1) / errorCost;
    }

    /** A service level is met at its limit exactly, and a miss says by how much. */
    static String slaLine(Sla sla, int measured) {
        boolean met = sla.direction().equals("max") ? measured <= sla.limit() : measured >= sla.limit();
        String verdict = met ? "met" : "missed by " + Math.abs(measured - sla.limit()) + " " + sla.unit();
        String word = sla.direction().equals("max") ? "limit" : "floor";
        return sla.name() + ": " + measured + " " + sla.unit() + " against a " + word + " of " + sla.limit() + " " + sla.unit() + ": " + verdict;
    }

    /** The costliest segment first, with its accuracy and whether a person checks it. */
    static List<String> segmentReport(List<Segment> segments, int reviewCost) {
        List<Segment> sorted = new ArrayList<>(segments);
        sorted.sort(Comparator.comparingInt((Segment s) -> -s.errorCost()).thenComparing(Segment::name));
        List<String> lines = new ArrayList<>();
        for (Segment s : sorted) {
            int floor = breakEven(s.errorCost(), reviewCost);
            String handling = pct(s.right(), s.total()) >= floor ? "auto" : "reviewed";
            lines.add(s.name() + ": " + pct(s.right(), s.total()) + " percent right, error cost " + s.errorCost() + ", " + handling + " (break-even " + floor + ")");
        }
        return lines;
    }

    /** The same facts for a sponsor (money, risk and one decision) or for an engineer (the numbers that produced them). */
    static String brief(String audience, String design, int cost, int baseline, Segment weakest, String ask) {
        if (audience.equals("sponsor")) {
            return design + " costs " + group(cost) + " a month against " + group(baseline) + " for people alone, a saving of " + group(baseline - cost) + ". "
                + "The weakest answers are " + weakest.name() + ": " + pct(weakest.right(), weakest.total()) + " in 100 are right and each wrong one costs " + weakest.errorCost() + ", "
                + "so a person decides them. Decision asked: " + ask + ".";
        }
        return "design=" + design + "; cost=" + cost + "; baseline=" + baseline + "; saving=" + (baseline - cost) + "; weakest=" + weakest.name() + " " + pct(weakest.right(), weakest.total()) + "% at " + weakest.errorCost() + " an error";
    }

    public static void main(String[] args) {
        Sla latency = new Sla("p95 latency", 2000, "max", "ms");
        Sla availability = new Sla("availability", 995, "min", "per mille");
        List<Measured> slas = List.of(new Measured(latency, 1800), new Measured(latency, 2000), new Measured(latency, 2150), new Measured(availability, 997), new Measured(availability, 990));
        for (Measured m : slas) System.out.println(slaLine(m.sla(), m.value()));
        List<Segment> segments = List.of(new Segment("status", 98, 100, 12), new Segment("credit", 63, 100, 250), new Segment("complaint", 91, 100, 60));
        for (String line : segmentReport(segments, 5)) System.out.println(line);
        Segment weakest = segments.get(0);
        for (Segment s : segments) if (pct(s.right(), s.total()) < pct(weakest.right(), weakest.total())) weakest = s;
        for (String audience : List.of("sponsor", "engineer")) System.out.println(audience + ": " + brief(audience, "Routing by confidence", 80000, 315000, weakest, "approve the pilot"));
    }
}
