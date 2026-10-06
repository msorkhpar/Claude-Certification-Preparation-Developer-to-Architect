import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PersonalSetupTest {
    // config.dir is the folder that holds home/ (the user's ~) and project/ (a repository): starter, reference or a planted wrong solution.
    private static final Path ROOT = Path.of(System.getProperty("config.dir", "starter"));
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HOME = "home/.claude";
    private static final String PROJECT = "project/.claude";
    private static final Set<String> BUILTIN_STYLES = Set.of("Proactive", "Concise", "Explanatory", "Learning");
    private static final Set<String> STYLE_FIELDS = Set.of("name", "description", "keep-coding-instructions", "force-for-plugin");
    private static final Set<String> CONTEXTS = Set.of("Global", "Chat", "Autocomplete", "Settings", "Confirmation", "Tabs", "Help", "Transcript", "HistorySearch", "Task", "ThemePicker",
        "Attachments", "Footer", "MessageSelector", "DiffDialog", "DiffPanel", "ModelPicker", "EffortSlider", "Select", "Plugin", "Pane", "PaneField", "Agents", "Scroll");
    private static final Map<String, String> MODIFIERS = Map.ofEntries(Map.entry("ctrl", "ctrl"), Map.entry("control", "ctrl"), Map.entry("shift", "shift"), Map.entry("alt", "alt"),
        Map.entry("opt", "alt"), Map.entry("option", "alt"), Map.entry("meta", "alt"), Map.entry("cmd", "cmd"), Map.entry("command", "cmd"), Map.entry("super", "cmd"), Map.entry("win", "cmd"));
    private static final Set<String> NAMED_KEYS = Set.of("escape", "esc", "enter", "return", "tab", "space", "up", "down", "left", "right", "pageup", "pagedown", "home", "end",
        "backspace", "delete", "wheelup", "wheeldown");
    private static final Set<String> RESERVED = Set.of("ctrl+c", "ctrl+d", "ctrl+m", "ctrl+[", "ctrl+i", "ctrl+h");
    private static final Pattern ACTION = Pattern.compile("^[a-z][A-Za-z]*:[A-Za-z]+$");
    private static final Set<String> STATUS_FIELDS = Set.of("model.id", "model.display_name", "cwd", "workspace.current_dir", "workspace.project_dir", "workspace.added_dirs",
        "workspace.git_worktree", "cost.total_cost_usd", "cost.total_duration_ms", "cost.total_api_duration_ms", "cost.total_lines_added", "cost.total_lines_removed",
        "context_window.total_input_tokens", "context_window.total_output_tokens", "context_window.context_window_size", "context_window.used_percentage",
        "context_window.remaining_percentage", "exceeds_200k_tokens", "fast_mode", "effort.level", "thinking.enabled", "rate_limits.five_hour.used_percentage",
        "rate_limits.seven_day.used_percentage", "session_id", "session_name", "transcript_path", "version", "output_style.name", "vim.mode", "agent.name", "pr.number", "pr.url",
        "pr.review_state", "worktree.name", "worktree.path", "worktree.branch");
    private static final Pattern PERSONAL = Pattern.compile("/home/|/Users/|C:\\\\Users|[\\w.+-]+@[\\w-]+\\.[\\w.-]+|sk-ant|TODO|FIXME");

    private static String read(String rel) {
        Path path = ROOT.resolve(rel);
        assertTrue(Files.isRegularFile(path), rel + " is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode readJson(String rel) {
        JsonNode data;
        try {
            data = JSON.readTree(read(rel));
        } catch (IOException error) {
            throw new AssertionError(rel + " is not valid JSON: " + error.getMessage());
        }
        assertTrue(data.isObject(), rel + " must hold an object");
        return data;
    }

    /** The front matter fields of a Markdown file (key: value lines) and its body. */
    private static Map<String, String> front(String rel, String[] body) {
        Matcher m = Pattern.compile("^---\\n(.*?)\\n---\\n?(.*)$", Pattern.DOTALL).matcher(read(rel));
        assertTrue(m.matches(), rel + " has no front matter");
        Map<String, String> fields = new LinkedHashMap<>();
        for (String line : m.group(1).split("\n")) {
            int at = line.indexOf(':');
            if (at > 0 && !line.substring(0, at).isBlank()) fields.put(line.substring(0, at).strip(), line.substring(at + 1).strip());
        }
        body[0] = m.group(2);
        return fields;
    }

    private static List<String> styleFiles() throws IOException {
        Path folder = ROOT.resolve(HOME).resolve("output-styles");
        if (!Files.isDirectory(folder)) return List.of();
        try (Stream<Path> s = Files.list(folder)) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".md")).sorted().toList();
        }
    }

    private static Set<String> customStyles() throws IOException {
        Set<String> names = new HashSet<>();
        for (String file : styleFiles()) {
            String name = front(HOME + "/output-styles/" + file, new String[1]).get("name");
            names.add(name != null && !name.isEmpty() ? name : file.substring(0, file.length() - 3));
        }
        return names;
    }

    private static boolean knownStyle(JsonNode name) throws IOException {
        return name.isTextual() && !name.asText().isEmpty() && (BUILTIN_STYLES.contains(name.asText()) || customStyles().contains(name.asText()));
    }

    private static String keyProblem(String keystroke) {
        for (String part : keystroke.trim().split("\\s+")) {
            if (part.isEmpty()) continue;
            String[] pieces = part.split("\\+", -1);
            String key = pieces[pieces.length - 1].toLowerCase();
            if (!(key.length() == 1 || NAMED_KEYS.contains(key))) return part + ": unknown key " + key;
            for (int i = 0; i < pieces.length - 1; i++) if (!MODIFIERS.containsKey(pieces[i].toLowerCase())) return part + ": unknown modifier " + pieces[i];
        }
        return null;
    }

    private static String normal(String keystroke) {
        String[] parts = keystroke.toLowerCase().split("\\+", -1);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < parts.length - 1; i++) out.add(MODIFIERS.getOrDefault(parts[i], parts[i]));
        out.add(parts[parts.length - 1]);
        return String.join("+", out);
    }

    @Test
    void m1_theUserLevelHoldsAStyleAStatusLineAndAKeybindingsFileThatFitTogether() throws IOException {
        JsonNode settings = readJson(HOME + "/settings.json");
        assertTrue(settings.path("outputStyle").isTextual() && !settings.path("outputStyle").asText().isEmpty(), "the user settings select an output style");
        assertTrue(knownStyle(settings.path("outputStyle")), "outputStyle " + settings.path("outputStyle").asText() + " names no built-in style and no style file");
        JsonNode line = settings.path("statusLine");
        assertTrue(line.isObject() && "command".equals(line.path("type").asText(null)), "statusLine is an object of type command");
        String command = line.path("command").asText("");
        assertTrue(command.startsWith("~/.claude/"), "the command points at a script in the user's .claude folder");
        assertTrue(Files.isRegularFile(ROOT.resolve("home/.claude").resolve(command.substring("~/.claude/".length()))), command + " is not a file");
        JsonNode bindings = readJson(HOME + "/keybindings.json").path("bindings");
        assertTrue(bindings.isArray() && bindings.size() > 0, "keybindings.json holds a non-empty bindings array");
        for (JsonNode b : bindings) assertTrue(b.isObject() && b.path("context").isTextual() && b.path("bindings").isObject(), "each block has a context and a bindings object");
    }

    @Test
    void e1_theStyleFileKeepsTheCodingInstructionsAndCarriesOnlyDocumentedFields() throws IOException {
        List<String> files = styleFiles();
        assertFalse(files.isEmpty(), "an output style file is needed");
        for (String file : files) {
            String[] body = new String[1];
            Map<String, String> fields = front(HOME + "/output-styles/" + file, body);
            Set<String> unknown = new TreeSet<>(fields.keySet());
            unknown.removeAll(STYLE_FIELDS);
            assertTrue(unknown.isEmpty(), file + ": unknown front matter fields " + unknown + " (a misspelled field is ignored without an error)");
            assertFalse(fields.getOrDefault("name", "").isEmpty(), file + ": name the style");
            assertFalse(fields.getOrDefault("description", "").isEmpty(), file + ": describe the style");
            assertEquals("true", fields.get("keep-coding-instructions"), file + ": a style that only changes communication keeps the coding instructions");
            assertTrue(Arrays.stream(body[0].split("\n")).filter(l -> !l.isBlank()).count() >= 3, file + ": the instructions are too thin");
        }
    }

    @Test
    void e2_theStatusLineReadsOnlyFieldsTheSessionSendsAndRefreshesNoFasterThanEverySecond() {
        JsonNode line = readJson(HOME + "/settings.json").path("statusLine");
        assertTrue(line.isObject(), "statusLine is an object");
        if (line.has("padding")) assertTrue(line.get("padding").isIntegralNumber() && line.get("padding").asInt() >= 0, "padding is a number of characters, 0 or more");
        if (line.has("refreshInterval")) assertTrue(line.get("refreshInterval").isIntegralNumber() && line.get("refreshInterval").asInt() >= 1, "refreshInterval is in seconds, 1 or more");
        String command = line.path("command").asText("");
        assertTrue(command.startsWith("~/.claude/"), "the command points at a script in the user's .claude folder");
        String script = read(HOME + "/" + command.substring("~/.claude/".length()));
        assertTrue(script.startsWith("#!/bin/sh") || script.startsWith("#!/usr/bin/env bash") || script.startsWith("#!/bin/bash"), "the script starts with a shebang line");
        Set<String> used = new TreeSet<>();
        Matcher exprs = Pattern.compile("jq -r '([^']*)'").matcher(script);
        while (exprs.find()) {
            Matcher fields = Pattern.compile("(?<![\\w$\"])\\.([A-Za-z_][\\w.]*)").matcher(exprs.group(1));
            while (fields.find()) used.add(fields.group(1));
        }
        assertFalse(used.isEmpty(), "the script reads at least one field of the session JSON with jq");
        Set<String> unknown = new TreeSet<>(used);
        unknown.removeAll(STATUS_FIELDS);
        assertTrue(unknown.isEmpty(), "fields the session does not send: " + unknown);
    }

    @Test
    void e3_theKeybindingsUseRealContextsAndActionsFreeKeysAndANullToUnbind() {
        JsonNode blocks = readJson(HOME + "/keybindings.json").path("bindings");
        assertTrue(blocks.isArray() && blocks.size() > 0, "the file holds a bindings array");
        int rebound = 0;
        int unbound = 0;
        for (JsonNode block : blocks) {
            assertTrue(CONTEXTS.contains(block.path("context").asText("")), block.path("context").asText() + " is not a context (names are case sensitive)");
            List<String> keys = new ArrayList<>();
            block.path("bindings").fieldNames().forEachRemaining(keys::add);
            for (String keystroke : keys) {
                String problem = keyProblem(keystroke);
                assertNull(problem, String.valueOf(problem));
                assertFalse(RESERVED.contains(normal(keystroke)), keystroke + " is reserved and cannot be rebound");
                JsonNode action = block.path("bindings").get(keystroke);
                if (action.isNull()) unbound++;
                else {
                    assertTrue(action.isTextual() && ACTION.matcher(action.asText()).matches(), action + " is not a namespace:action name");
                    rebound++;
                }
            }
        }
        assertTrue(rebound >= 1 && unbound >= 1, "rebind one key to an action and unbind another with null");
    }

    @Test
    void e4_personalSettingsStayInTheUserFilesAndTheProjectFileStaysShared() throws IOException {
        JsonNode shared = readJson(PROJECT + "/settings.json");
        for (String key : List.of("outputStyle", "statusLine")) assertFalse(shared.has(key), key + " is personal: a shared project file would override every teammate's own choice");
        JsonNode allow = shared.path("permissions").path("allow");
        assertTrue(allow.isArray() && allow.size() > 0, "the shared file carries the team's permission rules");
        JsonNode local = readJson(PROJECT + "/settings.local.json");
        assertTrue(knownStyle(local.path("outputStyle")), "the local file overrides the style for this project with an exact style name");
    }

    @Test
    void e5_theLocalSettingsFileIsKeptOutOfGit() {
        List<String> lines = Arrays.stream(read("project/.gitignore").split("\n")).map(String::strip).toList();
        assertTrue(lines.contains(".claude/settings.local.json") || lines.contains("**/.claude/settings.local.json"), "list the local settings file in the ignore file");
    }

    @Test
    void e6_noFileHoldsAPersonalPathAnAddressAKeyOrAnUnfinishedMarker() throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(ROOT)) {
            files = s.filter(Files::isRegularFile).toList();
        }
        assertFalse(files.isEmpty(), "there are no files");
        for (Path path : files) {
            Matcher m = PERSONAL.matcher(Files.readString(path));
            assertFalse(m.find(), ROOT.relativize(path) + " holds " + (m.hitEnd() ? "" : m.group()));
        }
    }
}
