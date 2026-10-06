import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ToolsetTest {
    private static final String ORDER_TEXT = "Looks up one order by its id and returns its status, items and total in cents. Use when the customer gives an order id such as A-1042 "
        + "or asks where an order is. Do not use it to find a customer by name; use get_customer instead of this tool for that. It returns no payment details.";
    private static final String CUSTOMER_TEXT = "Finds one customer by email address and returns the customer id, name and plan. Use when the person gives an email or asks about their account. "
        + "Do not use it for orders; call lookup_order instead of this tool for those. It returns no payment details.";

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> prop(String type, String description) {
        return map("type", type, "description", description);
    }

    private static Map<String, Object> prop(String type, String description, List<String> enumValues) {
        return map("type", type, "description", description, "enum", enumValues);
    }

    private static Map<String, Object> orderProps() {
        return map("order_id", prop("string", "The order id, for example A-1042."));
    }

    private static Map<String, Object> tool(String name, String description, Map<String, Object> properties, List<String> required) {
        return map("name", name, "description", description, "input_schema", map("type", "object", "properties", properties, "required", required));
    }

    private static Map<String, Object> tool() {
        return tool("lookup_order", ORDER_TEXT, orderProps(), List.of("order_id"));
    }

    private static Map<String, Object> named(String name) {
        return tool(name, ORDER_TEXT, orderProps(), List.of("order_id"));
    }

    private static Map<String, Object> described(String name, String description) {
        return tool(name, description, orderProps(), List.of("order_id"));
    }

    private static Map<String, Object> with(Map<String, Object> tool, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(tool);
        copy.put(key, value);
        return copy;
    }

    private static Map<String, Object> props(Map<String, Object> properties, List<String> required) {
        return tool("lookup_order", ORDER_TEXT, properties, required);
    }

    private static Map<String, Object> plus(Map<String, Object> base, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(base);
        copy.put(key, value);
        return copy;
    }

    private static List<String> rules(Map<String, Object> tool) {
        List<String> found = Toolset.lintTool(tool);
        assertNotNull(found, "lintTool returned null");
        return found;
    }

    private static List<List<String>> pairs(List<Map<String, Object>> tools) {
        List<List<String>> found = Toolset.lintToolSet(tools);
        assertNotNull(found, "lintToolSet returned null");
        return found;
    }

    private static List<List<String>> pairs(List<Map<String, Object>> tools, int maxTools) {
        List<List<String>> found = Toolset.lintToolSet(tools, maxTools);
        assertNotNull(found, "lintToolSet returned null");
        return found;
    }

    @Test
    void m1_aWellMadeToolLintsCleanAndAPoorOneIsNamedForEveryRuleItBreaks() {
        Map<String, Object> good = with(with(tool(), "input_examples", List.of(map("order_id", "A-1042"))), "annotations", map("readOnlyHint", true));
        assertEquals(List.of(), rules(good));
        Map<String, Object> poor = tool("helper", "Gets stuff.", map("q", map("type", "string")), List.of("q"));
        assertEquals(List.of("no-boundary", "no-use-when", "param-undescribed", "short-description", "vague-name"), rules(poor));
    }

    @Test
    void e1_namesMustMatchThePatternAndAVagueNameIsFlagged() {
        for (String bad : List.of("get order", "get.order", "a".repeat(129), "order#1")) assertEquals(List.of("bad-name"), rules(named(bad)), bad);
        for (String good : List.of("a".repeat(128), "look-up-order", "run_report")) assertEquals(List.of(), rules(named(good)), good);
        for (String vague : List.of("run", "Run", "helper", "query")) assertEquals(List.of("vague-name"), rules(named(vague)), vague);
    }

    @Test
    void e2_aDescriptionNeedsThreeSentencesAWhenToUsePhraseAndABoundaryAgainstTheNeighbour() {
        assertEquals(List.of("short-description"), rules(described("lookup_order", "Looks up one order by id. Use when the customer gives an order id, not for customers.")));
        assertEquals(List.of("no-use-when"), rules(described("lookup_order", "Looks up one order by id. It returns the status and total. Do not use it for customers.")));
        assertEquals(List.of("no-boundary"), rules(described("lookup_order", "Looks up one order by id. Use when the customer gives an order id. It returns the status and total.")));
        assertEquals(List.of(), rules(described("lookup_order", "Looks up one order by id. USE WHEN the customer gives an id! Prefer it instead of get_customer for orders.")));
        assertEquals(List.of("no-boundary", "no-use-when", "short-description"), rules(described("lookup_order", "")));
    }

    @Test
    void e3_parametersAreDescribedAndRequiredNamesExistAndClosedSetsAreEnumsAndExamplesFitTheSchema() {
        assertEquals(List.of("param-undescribed"), rules(props(map("order_id", prop("string", "  ")), List.of("order_id"))));
        assertEquals(List.of("required-unknown"), rules(props(orderProps(), List.of("order_id", "missing"))));
        Map<String, Object> status = prop("string", "One of open, shipped or closed.");
        Map<String, Object> orderId = prop("string", "The order id.");
        assertEquals(List.of("open-set"), rules(props(map("order_id", orderId, "status", status), List.of("order_id"))));
        assertEquals(List.of("open-set"), rules(props(map("order_id", orderId, "status", prop("string", "Either open or closed.")), List.of("order_id"))));
        assertEquals(List.of(), rules(props(map("order_id", orderId, "status", prop("string", "One of open, shipped or closed.", List.of("open", "shipped", "closed"))), List.of("order_id"))));
        assertEquals(List.of("reasoning-param"), rules(props(map("order_id", orderId, "reasoning", prop("string", "Why.")), List.of("order_id"))));
        assertEquals(List.of("reasoning-param"), rules(props(map("order_id", orderId, "note", prop("string", "Your step by step thinking.")), List.of("order_id"))));
        assertEquals(List.of(), rules(props(map("order_id", orderId, "note", prop("string", "A short explanation of why the call is made.")), List.of("order_id"))));
        Map<String, Object> properties = map("order_id", orderId, "status", prop("string", "The status.", List.of("open", "closed")), "limit", prop("integer", "Page size."));
        Map<String, Object> base = props(properties, List.of("order_id"));
        assertEquals(List.of(), rules(plus(base, "input_examples", List.of(map("order_id", "A", "status", "open", "limit", 5), map("order_id", "B")))));
        List<Map<String, Object>> bad = List.of(map("order_id", 5), map(), map("order_id", "A", "extra", 1), map("order_id", "A", "status", "lost"), map("order_id", "A", "limit", true), map("order_id", "A", "limit", "5"));
        for (Map<String, Object> example : bad) assertEquals(List.of("bad-example"), rules(plus(base, "input_examples", List.of(example))), example.toString());
    }

    @Test
    void e4_aListToolNeedsALimitAndACursorAndAHintMayNotContradictTheName() {
        Map<String, Object> query = map("query", prop("string", "Search text."));
        Map<String, Object> limit = prop("integer", "Page size.");
        Map<String, Object> cursor = prop("string", "Opaque cursor from the last page.");
        Map<String, Object> withLimit = map("query", query.get("query"), "limit", limit);
        Map<String, Object> withBoth = map("query", query.get("query"), "limit", limit, "cursor", cursor);
        assertEquals(List.of("list-unbounded"), rules(tool("search_orders", ORDER_TEXT, query, List.of())));
        assertEquals(List.of("list-unbounded"), rules(tool("search_orders", ORDER_TEXT, withLimit, List.of())));
        assertEquals(List.of("list-unbounded"), rules(tool("list_orders", ORDER_TEXT, map(), List.of())));
        assertEquals(List.of(), rules(tool("find_customer", ORDER_TEXT, withBoth, List.of())));
        assertEquals(List.of(), rules(tool("get_order", ORDER_TEXT, query, List.of())));
        assertEquals(List.of("hint-contradicts-name"), rules(with(named("delete_order"), "annotations", map("readOnlyHint", true))));
        assertEquals(List.of("hint-contradicts-name"), rules(with(named("delete_order"), "annotations", map("destructiveHint", false))));
        assertEquals(List.of("hint-contradicts-name"), rules(with(named("remove_item"), "annotations", map("destructiveHint", false))));
        assertEquals(List.of("hint-contradicts-name"), rules(with(named("send_receipt"), "annotations", map("readOnlyHint", true))));
        for (Map<String, Object> ok : List.of(with(named("delete_order"), "annotations", map("destructiveHint", true)), with(named("get_order"), "annotations", map("readOnlyHint", true)), with(named("create_order"), "annotations", map("readOnlyHint", false)))) {
            assertEquals(List.of(), rules(ok));
        }
    }

    @Test
    void e5_aSetIsGradedForDuplicateNamesOverlappingDescriptionsAndSize() {
        assertEquals(List.of(List.of("get_order_status", "overlap:lookup_order"), List.of("lookup_order", "overlap:get_order_status")), pairs(List.of(tool(), named("get_order_status"))));
        Map<String, Object> customer = described("get_customer", CUSTOMER_TEXT);
        assertEquals(List.of(), pairs(List.of(tool(), customer)));
        assertEquals(List.of(List.of("lookup_order", "duplicate-name")), pairs(List.of(tool(), described("lookup_order", CUSTOMER_TEXT))));
        assertEquals(List.of(List.of("a_tool", "overlap:b_tool"), List.of("b_tool", "overlap:a_tool")), overlap("alpha beta gamma delta", "alpha beta gamma epsilon"));
        assertEquals(List.of(), overlap("alpha beta gamma delta epsilon", "alpha beta gamma zeta eta"));
        assertEquals(List.of(List.of("*", "too-many-tools")), pairs(List.of(tool(), customer), 1));
        assertEquals(List.of(), pairs(List.of(tool(), customer), 2));
    }

    private static List<List<String>> overlap(String a, String b) {
        return pairs(List.of(described("a_tool", a), described("b_tool", b))).stream().filter(p -> p.get(1).startsWith("overlap:")).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static List<String> itemsOf(Map<String, Object> page) {
        return (List<String>) page.get("items");
    }

    private static List<String> rows(int n, String format) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(String.format(format, i));
        return out;
    }

    @Test
    void e6_pagesCarryAnOpaqueCursorAndAClampedLimitAndANote() {
        List<String> rows = rows(25, "row-%02d");
        Map<String, Object> first = Toolset.pageResults(rows);
        assertNotNull(first);
        assertEquals(rows.subList(0, 10), itemsOf(first));
        assertEquals(false, first.get("truncated"));
        assertEquals("Showing 10 of 25 results; pass next_cursor to continue, or narrow the query with a filter.", first.get("note"));
        String cursor = (String) first.get("next_cursor");
        assertNotNull(cursor);
        assertFalse(cursor.contains("10") || cursor.contains("offset"));
        Map<String, Object> second = Toolset.pageResults(rows, cursor);
        assertEquals(rows.subList(10, 20), itemsOf(second));
        assertNotNull(second.get("next_cursor"));
        Map<String, Object> last = Toolset.pageResults(rows, (String) second.get("next_cursor"));
        assertEquals(rows.subList(20, 25), itemsOf(last));
        assertNull(last.get("next_cursor"));
        assertNull(last.get("note"));
        assertEquals(50, itemsOf(Toolset.pageResults(rows(120, "r%03d"), null, 500)).size());
        assertEquals(rows.subList(0, 3), itemsOf(Toolset.pageResults(rows, null, 3)));
        for (int bad : new int[] {0, -1}) assertThrows(IllegalArgumentException.class, () -> Toolset.pageResults(rows, null, bad), "limit " + bad + " was accepted");
        for (String bad : new String[] {"not a cursor!", ""}) assertThrows(IllegalArgumentException.class, () -> Toolset.pageResults(rows, bad), "cursor " + bad + " was accepted");
    }

    @Test
    void e7_aPageStopsAtTheSizeCapAndSaysSoButAlwaysCarriesOneItem() {
        List<String> ten = List.of("0123456789", "0123456789", "0123456789", "0123456789", "0123456789");
        Map<String, Object> cut = Toolset.pageResults(ten, null, 10, 25);
        assertNotNull(cut);
        assertEquals(ten.subList(0, 2), itemsOf(cut));
        assertEquals(true, cut.get("truncated"));
        assertNotNull(cut.get("next_cursor"));
        assertTrue(String.valueOf(cut.get("note")).startsWith("Showing 2 of 5 results"));
        assertEquals(2, itemsOf(Toolset.pageResults(ten, null, 10, 20)).size());
        Map<String, Object> big = Toolset.pageResults(List.of("x".repeat(100), "y"), null, 10, 10);
        assertEquals(List.of("x".repeat(100)), itemsOf(big));
        assertEquals(true, big.get("truncated"));
        Map<String, Object> whole = Toolset.pageResults(ten, null, 10, 1000);
        assertEquals(ten, itemsOf(whole));
        assertEquals(false, whole.get("truncated"));
        assertNull(whole.get("next_cursor"));
    }

    @Test
    void e8_aToolsOwnHintsAreTrustedOnlyFromATrustedServerAndTheDefaultsApplyOtherwise() {
        Map<String, Boolean> defaults = new LinkedHashMap<>();
        defaults.put("readOnlyHint", false);
        defaults.put("destructiveHint", true);
        defaults.put("idempotentHint", false);
        defaults.put("openWorldHint", true);
        Map<String, Object> reader = map("name", "get_order", "server", "orders", "annotations", map("readOnlyHint", true, "idempotentHint", true));
        Map<String, Boolean> trusted = new LinkedHashMap<>(defaults);
        trusted.put("readOnlyHint", true);
        trusted.put("idempotentHint", true);
        assertEquals(trusted, Toolset.effectiveHints(reader, true));
        assertEquals(defaults, Toolset.effectiveHints(reader, false));
        assertEquals(defaults, Toolset.effectiveHints(map("name", "x", "annotations", map("readOnlyHint", "yes")), true));
        assertEquals(defaults, Toolset.effectiveHints(map("name", "x"), true));
        Map<String, Object> writer = map("name", "update_order", "server", "orders", "annotations", map("readOnlyHint", false));
        Map<String, Object> stranger = map("name", "get_notes", "server", "unknown", "annotations", map("readOnlyHint", true));
        assertEquals(List.of("get_order"), Toolset.parallelSafe(List.of(reader, writer, stranger), Set.of("orders")));
        assertEquals(List.of("get_order", "get_notes"), Toolset.parallelSafe(List.of(reader, writer, stranger), Set.of("orders", "unknown")));
        assertEquals(List.of(), Toolset.parallelSafe(List.of(reader), Set.of()));
    }
}
