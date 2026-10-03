import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class HarnessTest {
    @SuppressWarnings("unchecked")
    private static Map<String, Object> obj(String json) {
        return (Map<String, Object>) Json.parse(json);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> cases(String json) {
        return (List<Map<String, Object>>) Json.parse(json);
    }

    private static Map<String, Object> mk(String checkJson) {
        return obj("{\"id\":\"c\",\"input\":\"x\",\"check\":" + checkJson + "}");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : new LinkedHashMap<>();
    }

    private static List<?> asList(Object o) {
        return o instanceof List<?> l ? l : List.of();
    }

    /** "passed|reason" of one grading. */
    private static String v(String checkJson, String output, Function<String, String> judge) {
        Map<String, Object> r = Harness.grade(mk(checkJson), output, judge);
        return r == null ? "null" : r.get("passed") + "|" + r.get("reason");
    }

    private static Function<String, String> reply(String text, List<String> seen) {
        return prompt -> {
            seen.add(prompt);
            return text;
        };
    }

    private static String results(Map<String, Object> report) {
        StringBuilder sb = new StringBuilder();
        for (Object o : asList(report.get("results"))) {
            Map<String, Object> r = asMap(o);
            sb.append(r.get("id")).append(':').append(r.get("passed")).append(':').append(r.get("reason")).append(':').append(r.get("flaky")).append(' ');
        }
        return sb.toString().trim();
    }

    private static double num(Object o) {
        return o instanceof Number n ? n.doubleValue() : Double.NaN;
    }

    @Test
    void m1_aRunGradesEveryCaseWithItsOwnCheckAndReportsThePassRate() {
        List<Map<String, Object>> cs = cases("[{\"id\":\"c1\",\"input\":\"I love it\",\"tags\":[\"core\"],\"check\":{\"type\":\"exact\",\"expected\":\"positive\"}},"
            + "{\"id\":\"c2\",\"input\":\"awful\",\"tags\":[\"core\"],\"check\":{\"type\":\"exact\",\"expected\":\"negative\"}},"
            + "{\"id\":\"c3\",\"input\":\"order 7\",\"tags\":[\"extract\"],\"check\":{\"type\":\"regex\",\"pattern\":\"ORD-\\\\d{4}\"}},"
            + "{\"id\":\"c4\",\"input\":\"meh\",\"tags\":[\"core\",\"edge\"],\"check\":{\"type\":\"exact\",\"expected\":\"neutral\"}}]");
        Map<String, String> answers = Map.of("I love it", "positive", "awful", "negative", "order 7", "The order is ORD-0007.", "meh", "positive");
        Map<String, Object> report = Harness.runEval(cs, answers::get, null, 1);
        assertEquals(4.0, num(report.get("total")));
        assertEquals(3.0, num(report.get("passed")));
        assertEquals(0.75, num(report.get("pass_rate")), 1e-9);
        assertEquals("c1:true:ok:false c2:true:ok:false c3:true:ok:false c4:false:mismatch:false", results(report));
        Map<String, Object> empty = Harness.runEval(new ArrayList<>(), t -> t, null, 1);
        assertEquals(0.0, num(empty.get("total")));
        assertEquals(0.0, num(empty.get("pass_rate")), 1e-9);
        assertTrue(asList(empty.get("results")).isEmpty());
    }

    @Test
    void e1_anExactCheckIgnoresCaseAndSpacingButNothingElse() {
        String positive = "{\"type\":\"exact\",\"expected\":\"positive\"}";
        assertEquals("true|ok", v(positive, "  Positive \n", null));
        assertEquals("true|ok", v("{\"type\":\"exact\",\"expected\":\"not  enough info\"}", "Not enough\ninfo", null));
        assertEquals("false|mismatch", v(positive, "positively", null));
        assertEquals("false|mismatch", v(positive, "negative", null));
        assertEquals("false|mismatch", v(positive, "", null));
        assertEquals("false|mismatch", v("{\"type\":\"regex\",\"pattern\":\"^ORD-\\\\d{4}$\"}", "ORD-12345", null));
    }

    @Test
    void e2_aJsonFieldCheckNeedsAJsonObjectWithTheFieldAndTheSameTypedValue() {
        String spam = "{\"type\":\"json_field\",\"field\":\"label\",\"equals\":\"spam\"}";
        assertEquals("true|ok", v(spam, "{\"label\":\"spam\",\"score\":0.9}", null));
        assertEquals("true|ok", v(spam, " \n{\"label\": \"spam\"}\n", null));
        assertEquals("false|mismatch", v(spam, "{\"label\":\"ham\"}", null));
        assertEquals("false|missing field", v(spam, "{\"score\":1}", null));
        assertEquals("false|not json", v(spam, "Sure! {\"label\":\"spam\"}", null));
        assertEquals("false|not json", v(spam, "```json\n{\"label\":\"spam\"}\n```", null));
        assertEquals("false|not json", v(spam, "[\"spam\"]", null));
        String count = "{\"type\":\"json_field\",\"field\":\"count\",\"equals\":3}";
        assertEquals("true|ok", v(count, "{\"count\":3}", null));
        assertEquals("false|mismatch", v(count, "{\"count\":\"3\"}", null));
        assertEquals("false|mismatch", v("{\"type\":\"json_field\",\"field\":\"ok\",\"equals\":true}", "{\"ok\":1}", null));
    }

    @Test
    void e3_aJudgeCheckSendsTheRubricPromptAndAcceptsOnlyABareScoreAtTheThreshold() {
        String check = "{\"type\":\"judge\",\"criterion\":\"empathetic\",\"threshold\":4}";
        List<String> seen = new ArrayList<>();
        Map<String, Object> r = Harness.grade(mk(check), "We are sorry.", reply("5", seen));
        assertEquals("true|ok|5", r.get("passed") + "|" + r.get("reason") + "|" + r.get("score"));
        assertEquals(List.of("Rate this response on a scale of 1-5 for empathetic:\n<response>We are sorry.</response>\n1: Not at all empathetic\n5: Perfectly empathetic\nOutput only the number."), seen);
        assertEquals("true|ok", v(check, "x", reply(" 4\n", seen)));
        assertEquals("false|below threshold", v(check, "x", reply("3", seen)));
        String calm = "{\"type\":\"judge\",\"criterion\":\"calm\"}";
        assertEquals("true|ok", v(calm, "x", reply("4", seen)));
        assertEquals("false|below threshold", v(calm, "x", reply("3", seen)));
        for (String bad : List.of("Score: 4", "I'd say 4 or 5", "6", "0", "", "4.5")) assertEquals("false|ungradable", v(check, "x", reply(bad, seen)), bad);
        assertEquals("false|ungradable", v(check, "x", null));
        assertEquals("false|ungradable", v(check, "x", p -> { throw new IllegalStateException("judge down"); }));
        int[] calls = {0};
        Harness.runEval(cases("[{\"id\":\"a\",\"input\":\"q\",\"check\":{\"type\":\"exact\",\"expected\":\"y\"}}]"), t -> "y", p -> { calls[0]++; return "5"; }, 1);
        assertEquals(0, calls[0]);
    }

    @Test
    void e4_aModelThatFailsOnOneCaseDoesNotStopTheRun() {
        Function<String, String> model = text -> {
            if (text.equals("boom")) throw new IllegalStateException("503 from upstream");
            return "ok";
        };
        List<Map<String, Object>> cs = cases("[{\"id\":\"a\",\"input\":\"a\",\"check\":{\"type\":\"exact\",\"expected\":\"ok\"}},"
            + "{\"id\":\"boom\",\"input\":\"boom\",\"check\":{\"type\":\"exact\",\"expected\":\"ok\"}},"
            + "{\"id\":\"c\",\"input\":\"c\",\"check\":{\"type\":\"exact\",\"expected\":\"ok\"}}]");
        Map<String, Object> report = Harness.runEval(cs, model, null, 1);
        assertEquals(3.0, num(report.get("total")));
        assertEquals(2.0, num(report.get("passed")));
        assertEquals("a:true:ok:false boom:false:model error:false c:true:ok:false", results(report));
    }

    @Test
    void e5_tagsReportTheirOwnRatesAndSuccessCriteriaJudgeEachDimension() {
        List<Map<String, Object>> cs = cases("[{\"id\":\"a\",\"input\":\"1\",\"tags\":[\"core\"],\"check\":{\"type\":\"exact\",\"expected\":\"1\"}},"
            + "{\"id\":\"b\",\"input\":\"2\",\"tags\":[\"core\",\"edge\"],\"check\":{\"type\":\"exact\",\"expected\":\"2\"}},"
            + "{\"id\":\"c\",\"input\":\"3\",\"tags\":[\"edge\"],\"check\":{\"type\":\"exact\",\"expected\":\"x\"}}]");
        Map<String, Object> byTag = asMap(Harness.runEval(cs, t -> t, null, 1).get("by_tag"));
        assertEquals("[core, edge]", String.valueOf(byTag.keySet()));
        assertEquals(2.0, num(asMap(byTag.get("core")).get("passed")));
        assertEquals(2.0, num(asMap(byTag.get("core")).get("total")));
        assertEquals(1.0, num(asMap(byTag.get("edge")).get("passed")));
        assertEquals(2.0, num(asMap(byTag.get("edge")).get("total")));
        String rep = "{\"total\":10,\"passed\":8,\"pass_rate\":0.8,\"results\":[],\"flaky\":[],\"by_tag\":{\"core\":{\"passed\":6,\"total\":6},\"edge\":{\"passed\":2,\"total\":4}}}";
        String flaky = rep.replace("\"flaky\":[]", "\"flaky\":[\"c3\"]");
        assertEquals("{met=true, failures=[]}", String.valueOf(Harness.meets(obj(rep), obj("{\"min_pass_rate\":0.8,\"tags\":{\"edge\":0.5}}"))));
        assertEquals("{met=true, failures=[]}", String.valueOf(Harness.meets(obj(rep), obj("{}"))));
        assertEquals("{met=false, failures=[overall, tag:edge, tag:rare]}",
            String.valueOf(Harness.meets(obj(rep), obj("{\"min_pass_rate\":0.85,\"tags\":{\"edge\":0.75,\"core\":1.0,\"rare\":0.5}}"))));
        assertEquals("{met=false, failures=[flaky]}", String.valueOf(Harness.meets(obj(flaky), obj("{\"max_flaky\":0}"))));
        assertEquals("{met=true, failures=[]}", String.valueOf(Harness.meets(obj(flaky), obj("{\"max_flaky\":1}"))));
    }

    private static Map<String, Object> report(double rate, String rows) {
        StringBuilder results = new StringBuilder("[");
        for (String row : rows.split(" ")) {
            String[] p = row.split(":");
            if (results.length() > 1) results.append(',');
            results.append("{\"id\":\"").append(p[0]).append("\",\"passed\":").append(p[1].equals("T")).append('}');
        }
        return obj("{\"pass_rate\":" + rate + ",\"results\":" + results + "]}");
    }

    @Test
    void e6_aRegressionRunNamesWhatBrokeWhatWasFixedAndWhatWentMissing() {
        Map<String, Object> base = report(0.75, "a:T b:T c:F d:T");
        Map<String, Object> diff = Harness.compare(base, report(0.75, "a:T b:F c:T e:T"));
        assertEquals("[b]|[c]|[e]|[d]", diff.get("regressions") + "|" + diff.get("fixed") + "|" + diff.get("added") + "|" + diff.get("removed"));
        assertEquals(0.0, num(diff.get("pass_rate_delta")), 1e-9);
        assertEquals(false, diff.get("ok"));
        Map<String, Object> better = Harness.compare(report(0.5, "a:T b:T c:F d:F"), report(0.75, "a:T b:F c:T d:T"));
        assertEquals("[b]", String.valueOf(better.get("regressions")));
        assertEquals(0.25, num(better.get("pass_rate_delta")), 1e-9);
        assertEquals(false, better.get("ok"));
        Map<String, Object> same = Harness.compare(base, report(1.0, "a:T b:T c:T d:T"));
        assertEquals("[]|[c]|true", same.get("regressions") + "|" + same.get("fixed") + "|" + same.get("ok"));
        Map<String, Object> dropped = Harness.compare(report(0.5, "a:T d:F"), report(1.0, "a:T"));
        assertEquals("[]|[d]|false", dropped.get("regressions") + "|" + dropped.get("removed") + "|" + dropped.get("ok"));
    }

    @Test
    void e7_repeatedRunsExposeFlakyCasesAndACasePassesOnlyIfEveryRunDoes() {
        Map<String, List<String>> outputs = Map.of("q", List.of("yes", "yes", "no"), "r", List.of("yes", "yes", "yes"), "s", List.of("no", "no", "no"));
        Map<String, Integer> counts = new LinkedHashMap<>();
        int[] calls = {0};
        Function<String, String> model = text -> {
            calls[0]++;
            int n = counts.merge(text, 1, Integer::sum) - 1;
            return outputs.get(text).get(n);
        };
        List<Map<String, Object>> cs = cases("[{\"id\":\"q\",\"input\":\"q\",\"check\":{\"type\":\"exact\",\"expected\":\"yes\"}},"
            + "{\"id\":\"r\",\"input\":\"r\",\"check\":{\"type\":\"exact\",\"expected\":\"yes\"}},"
            + "{\"id\":\"s\",\"input\":\"s\",\"check\":{\"type\":\"exact\",\"expected\":\"yes\"}}]");
        Map<String, Object> report = Harness.runEval(cs, model, null, 3);
        assertEquals(9, calls[0]);
        assertEquals("q:false:mismatch:true r:true:ok:false s:false:mismatch:false", results(report));
        assertEquals("[q]", String.valueOf(report.get("flaky")));
        assertEquals(1.0, num(report.get("passed")));
        Map<String, Object> single = Harness.runEval(cs.subList(0, 1), t -> "yes", null, 1);
        assertEquals("q:true:ok:false", results(single));
        assertTrue(asList(single.get("flaky")).isEmpty());
    }
}
