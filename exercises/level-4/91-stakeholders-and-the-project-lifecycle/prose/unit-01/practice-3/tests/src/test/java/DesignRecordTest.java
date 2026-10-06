import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DesignRecordTest {
    // the folder that holds docs/design-record.md: the starter, the reference or a planted wrong solution
    private static final Path ROOT = Path.of(System.getProperty("solution.dir", "starter"));

    private static final List<String> SECTIONS = List.of("Summary for the sponsor", "Decision statement", "Options considered", "Recommendation and trade-offs", "Accuracy by segment", "Service levels", "Pilot to scale", "Hand-off and monitoring");
    private static final String ROLE = "billing operations manager";
    private static final int ERROR_COST = 250;
    private static final int REVIEW_COST = 5;
    private static final int MAX_LATENCY_MS = 2000;
    private static final double MIN_AVAILABILITY = 99.5;
    private static final Set<String> BAD_OWNERS = Set.of("", "tbd", "everyone", "team", "n/a", "none");
    private static final Set<String> SHAPES = Set.of("wrong amount", "outdated figure", "refusal", "made-up clause", "omitted exception");
    private static final Map<String, Integer> ERROR_COSTS = Map.of("credit", 250, "complaint", 60, "status", 12);

    private static String record() {
        Path path = ROOT.resolve("docs/design-record.md");
        assertTrue(Files.isRegularFile(path), "docs/design-record.md is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** title to body for every level 2 heading, in order. */
    private static Map<String, String> sections() {
        Map<String, String> out = new LinkedHashMap<>();
        Matcher m = Pattern.compile("^## (.*)$", Pattern.MULTILINE).matcher(record());
        List<int[]> spans = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (m.find()) {
            titles.add(m.group(1).strip());
            spans.add(new int[] {m.start(), m.end()});
        }
        String text = record();
        for (int i = 0; i < titles.size(); i++) {
            int end = i + 1 < titles.size() ? spans.get(i + 1)[0] : text.length();
            out.put(titles.get(i), text.substring(spans.get(i)[1], end).strip());
        }
        return out;
    }

    private static String body(String title) {
        String found = sections().get(title);
        assertNotNull(found, "the section '" + title + "' is missing");
        return found;
    }

    /** The rows of the first table in a section, without the header and the separator. */
    private static List<List<String>> table(String text) {
        List<List<String>> rows = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.startsWith("|")) {
                List<String> cells = new ArrayList<>();
                for (String c : line.strip().replaceAll("^\\||\\|$", "").split("\\|", -1)) cells.add(c.strip());
                if (!cells.stream().allMatch(c -> c.matches("[-: ]*"))) rows.add(cells);
            }
        }
        return rows.subList(1, rows.size());
    }

    private static double number(String cell) {
        Matcher m = Pattern.compile("\\d[\\d,]*\\.?\\d*").matcher(cell);
        assertTrue(m.find(), "no number in '" + cell + "'");
        return Double.parseDouble(m.group().replace(",", ""));
    }

    private static int words(String text) {
        String t = text.strip();
        return t.isEmpty() ? 0 : t.split("\\s+").length;
    }

    private static String fmt(double n) {
        return String.format(java.util.Locale.US, "%,d", (long) n);
    }

    private static List<List<String>> recommended() {
        return table(body("Options considered")).stream().filter(r -> r.size() == 5 && r.get(3).equals("recommended")).toList();
    }

    @Test
    void m1_theEightSectionsArePresentInOrderAndFilled() {
        assertEquals(SECTIONS, new ArrayList<>(sections().keySet()), "the sections are the eight headings, in order");
        sections().forEach((name, text) -> assertTrue(words(text) >= 15, "'" + name + "' says too little"));
        assertFalse(record().contains("TODO"), "replace every TODO");
    }

    @Test
    void e1_theDecisionStatementCarriesTheVolumeTheLatencyTheErrorCostAndTheAccountableRole() {
        String text = body("Decision statement").toLowerCase();
        for (String needed : List.of("3,000", "2 seconds", ROLE)) assertTrue(text.contains(needed), "the decision statement does not say '" + needed + "'");
        for (int value : List.of(ERROR_COST, REVIEW_COST)) assertTrue(Pattern.compile("\\b" + value + "\\b").matcher(text).find(), "the decision statement does not give the cost " + value);
    }

    @Test
    void e2_theOptionsIncludeARecommendedOneThatIsTheCheapestToMeetTheServiceLevelsAndAReasonForEachRejection() {
        List<List<String>> rows = table(body("Options considered"));
        assertTrue(rows.size() >= 4, "compare at least four options");
        for (List<String> row : rows) {
            assertTrue(row.size() == 5 && List.of("recommended", "alternative", "rejected").contains(row.get(3)), row + ": status is recommended, alternative or rejected");
            if (row.get(3).equals("rejected")) assertTrue(words(row.get(4)) >= 5, row.get(0) + ": give a reason for the rejection");
        }
        List<List<String>> chosen = rows.stream().filter(r -> r.get(3).equals("recommended")).toList();
        assertEquals(1, chosen.size(), "exactly one option is recommended");
        assertEquals("yes", chosen.get(0).get(2).toLowerCase(), "the recommended option meets the service levels");
        double cheapest = rows.stream().filter(r -> r.get(2).equalsIgnoreCase("yes")).mapToDouble(r -> number(r.get(1))).min().orElse(-1);
        assertEquals(cheapest, number(chosen.get(0).get(1)), "the recommended option is the cheapest one that meets the service levels");
    }

    @Test
    void e3_theBreakEvenAccuracyIs98PercentAndTheRecommendationStatesTheCost() {
        String text = body("Recommendation and trade-offs");
        int expected = 100 - (int) Math.ceil(100.0 * REVIEW_COST / ERROR_COST);
        Matcher found = Pattern.compile("at or above (\\d+) percent").matcher(text);
        assertTrue(found.find() && Integer.parseInt(found.group(1)) == expected, "state the rule 'at or above " + expected + " percent'");
        List<List<String>> chosen = recommended();
        assertFalse(chosen.isEmpty(), "recommend one option");
        assertTrue(text.contains(fmt(number(chosen.get(0).get(1)))), "the recommendation states the monthly cost of the recommended option");
    }

    @Test
    void e4_eachServiceLevelHasATargetWithinTheCaseLimitsAndANamedOwner() {
        List<List<String>> rows = table(body("Service levels")).stream().filter(r -> r.size() == 4).toList();
        List<String> latency = rows.stream().filter(r -> r.get(0).toLowerCase().contains("latency")).findFirst().orElse(null);
        List<String> availability = rows.stream().filter(r -> r.get(0).toLowerCase().contains("availability")).findFirst().orElse(null);
        List<String> accuracy = rows.stream().filter(r -> r.get(0).toLowerCase().contains("accuracy")).findFirst().orElse(null);
        assertTrue(latency != null && availability != null && accuracy != null, "list latency, availability and accuracy");
        assertTrue(latency.get(1).contains("ms") && number(latency.get(1)) <= MAX_LATENCY_MS, "the latency target is at most " + MAX_LATENCY_MS + " ms");
        assertTrue(availability.get(1).contains("percent") && number(availability.get(1)) >= MIN_AVAILABILITY, "the availability target is at least " + MIN_AVAILABILITY + " percent");
        assertTrue(accuracy.get(1).toLowerCase().contains("credit"), "the accuracy target is stated for the credit segment");
        for (List<String> row : List.of(latency, availability, accuracy)) assertTrue(!row.get(2).isEmpty() && !BAD_OWNERS.contains(row.get(3).toLowerCase()), row.get(0) + ": say how it is measured and who owns it");
    }

    @Test
    void e5_segmentsAreListedByErrorCostAndHandledByTheBreakEven() {
        List<List<String>> rows = table(body("Accuracy by segment"));
        assertEquals(new TreeSet<>(ERROR_COSTS.keySet()), new TreeSet<>(rows.stream().map(r -> r.get(0)).toList()), "list the credit, complaint and status segments");
        List<Double> costs = new ArrayList<>();
        for (List<String> row : rows) {
            assertEquals(6, row.size(), row + " is missing a column");
            String segment = row.get(0);
            double accuracy = number(row.get(2));
            assertTrue(accuracy >= 0 && accuracy <= 100 && SHAPES.contains(row.get(3)), segment + ": give an accuracy and one of the failure shapes");
            assertEquals((double) ERROR_COSTS.get(segment), number(row.get(4)), segment + ": the cost per error is " + ERROR_COSTS.get(segment));
            assertEquals(accuracy >= 98 ? "auto" : "reviewed", row.get(5), segment + ": handling follows the break-even accuracy of 98 percent");
            costs.add(number(row.get(4)));
        }
        List<Double> sorted = new ArrayList<>(costs);
        sorted.sort(Comparator.reverseOrder());
        assertEquals(sorted, costs, "list the costliest segment first");
    }

    @Test
    void e6_thePilotToScaleTableHasFourAssumptionsEachWithATestAndAStopTriggerWithANumber() {
        List<List<String>> rows = table(body("Pilot to scale"));
        assertTrue(rows.size() >= 4, "name at least four assumptions of the pilot");
        for (List<String> row : rows) {
            assertTrue(row.size() == 3 && row.stream().noneMatch(String::isEmpty), row + ": give the assumption, how to test it and what stops the roll-out");
            assertTrue(Pattern.compile("\\d").matcher(row.get(2)).find(), row.get(0) + ": the stop trigger needs a number");
        }
    }

    @Test
    void e7_theHandOffNamesAnOwnerARunbookARollbackAndMonitors() {
        String text = body("Hand-off and monitoring").toLowerCase();
        for (String needed : List.of("owner", "runbook", "rollback", "previous model")) assertTrue(text.contains(needed), "the hand-off does not mention '" + needed + "'");
        assertTrue(Stream.of("refusals", "tokens per answer", "flagged", "latency").filter(text::contains).count() >= 2, "name at least two monitors");
    }

    @Test
    void e8_theSponsorSummaryHasAtMost80WordsAndStatesTheCostAndTheDecisionAndNoFileHoldsPersonalData() throws IOException {
        String summary = body("Summary for the sponsor");
        assertTrue(words(summary) <= 80, "the sponsor summary has at most 80 words");
        List<List<String>> chosen = recommended();
        assertFalse(chosen.isEmpty(), "recommend one option");
        assertTrue(summary.contains(fmt(number(chosen.get(0).get(1)))), "the summary states the monthly cost");
        assertTrue(summary.toLowerCase().contains("decision") && !summary.contains("`"), "the summary asks for a decision, in plain words");
        List<String> hits = new ArrayList<>();
        Pattern home = Pattern.compile("(/home/\\w+|/Users/\\w+|C:\\\\Users)");
        Pattern email = Pattern.compile("[\\w.+-]+@(?!example\\.(com|invalid))[\\w-]+\\.[\\w.]+");
        try (Stream<Path> s = Files.walk(ROOT)) {
            for (Path p : s.filter(Files::isRegularFile).sorted().toList()) {
                String text = Files.readString(p);
                String rel = ROOT.relativize(p).toString();
                if (home.matcher(text).find()) hits.add(rel + ": home path");
                if (email.matcher(text).find()) hits.add(rel + ": email address");
            }
        }
        assertEquals(List.of(), hits);
    }
}
