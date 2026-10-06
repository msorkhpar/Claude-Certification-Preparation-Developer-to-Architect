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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MarketplaceSetupTest {
    // config.dir is the marketplace folder (it holds .claude-plugin/marketplace.json): starter, reference or a planted wrong solution.
    private static final Path ROOT = Path.of(System.getProperty("config.dir", "starter"));
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PLUGIN = "plugins/standards-kit";
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final Pattern SEMVER = Pattern.compile("\\d+\\.\\d+\\.\\d+");
    private static final Pattern SHA = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern TAG = Pattern.compile("v\\d+\\.\\d+\\.\\d+");
    private static final Pattern REPO = Pattern.compile("[\\w.-]+/[\\w.-]+");
    private static final Set<String> RESERVED = Set.of("claude-code-marketplace", "claude-code-plugins", "claude-plugins-official", "anthropic-marketplace",
        "anthropic-plugins", "agent-skills", "anthropic-agent-skills", "life-sciences", "knowledge-work-plugins", "claude-for-legal", "claude-for-financial-services",
        "financial-services-plugins", "first-party-plugins", "claude-tag-plugins", "claude-community", "claude-plugins-community", "healthcare",
        "anthropic-plugin-directory", "claude-plugin-directory", "inline", "builtin", "skills-dir", "synced", "claude-plugin-test", "npm", "pip", "uv", "cargo", "github", "gh");
    private static final Set<String> SOURCE_TYPES = Set.of("github", "url", "git-subdir", "npm", "archive", "command");

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
        try {
            return JSON.readTree(read(rel));
        } catch (IOException error) {
            throw new AssertionError(rel + " is not valid JSON: " + error.getMessage());
        }
    }

    private static JsonNode jsonText(String text) {
        try {
            return JSON.readTree(text);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode market() {
        JsonNode data = readJson(".claude-plugin/marketplace.json");
        assertTrue(data.isObject(), "marketplace.json must hold an object");
        return data;
    }

    private static List<JsonNode> entries() {
        JsonNode plugins = market().path("plugins");
        assertTrue(plugins.isArray() && plugins.size() > 0, "plugins must be a non-empty list");
        List<JsonNode> out = new ArrayList<>();
        plugins.forEach(p -> {
            assertTrue(p.isObject(), "every plugin entry is an object");
            out.add(p);
        });
        return out;
    }

    private static JsonNode entry(String name) {
        for (JsonNode p : entries()) if (name.equals(p.path("name").asText(null))) return p;
        throw new AssertionError("no entry named " + name);
    }

    private static JsonNode manifest() {
        JsonNode data = readJson(PLUGIN + "/.claude-plugin/plugin.json");
        assertTrue(data.isObject(), "plugin.json must hold an object");
        return data;
    }

    private static List<String> names(JsonNode object) {
        List<String> out = new ArrayList<>();
        object.fieldNames().forEachRemaining(out::add);
        return out;
    }

    /** The front matter fields of a Markdown file (key: value lines) and its body. */
    private static Map<String, String> front(String rel, String[] body) {
        Matcher m = Pattern.compile("^---\\n(.*?)\\n---\\n?(.*)$", Pattern.DOTALL).matcher(read(rel));
        assertTrue(m.matches(), rel + " has no front matter");
        Map<String, String> fields = new LinkedHashMap<>();
        for (String line : m.group(1).split("\n")) {
            int at = line.indexOf(':');
            if (at >= 0) fields.put(line.substring(0, at).strip(), line.substring(at + 1).strip());
        }
        body[0] = m.group(2);
        return fields;
    }

    private static boolean mentionsAnthropic(String low) {
        return low.contains("claude") || low.contains("anthropic");
    }

    @Test
    void m1_theMarketplaceFileNamesAnOwnerAndListsEachPluginByASourceThatResolves() throws IOException {
        JsonNode data = market();
        assertTrue(data.path("name").isTextual() && !data.path("name").asText().isBlank(), "a marketplace needs a name");
        assertTrue(data.path("owner").isObject() && !data.path("owner").path("name").asText("").isBlank(), "the owner needs a name");
        assertFalse(data.path("description").asText("").isBlank(), "describe the marketplace");
        List<String> names = new ArrayList<>();
        for (JsonNode p : entries()) {
            assertTrue(p.path("name").isTextual() && !p.path("name").asText().isEmpty(), "every entry needs a name");
            names.add(p.path("name").asText());
        }
        assertEquals(names.size(), new HashSet<>(names).size(), "entry names are unique");
        for (JsonNode p : entries()) {
            String name = p.path("name").asText();
            assertTrue(p.has("source"), name + " needs a source");
            JsonNode source = p.get("source");
            if (source.isTextual()) {
                String s = source.asText();
                assertTrue(s.startsWith("./") && !s.contains("..") && !s.contains("\\"), name + ": a relative path starts with ./ and stays inside the marketplace");
                assertTrue(Files.isDirectory(ROOT.resolve(s)), name + ": " + s + " is not a folder of the marketplace");
                JsonNode inner = JSON.readTree(Files.readString(ROOT.resolve(s).resolve(".claude-plugin/plugin.json")));
                assertEquals(name, inner.path("name").asText(null), name + ": the entry name and the manifest name must be the same");
            } else {
                assertTrue(source.isObject() && SOURCE_TYPES.contains(source.path("source").asText("")), name + ": a source object names a type");
            }
        }
    }

    @Test
    void e1_namesAreValidInAPluginIdAndAreNotReservedOrMistakenForOfficialOnes() {
        String name = market().path("name").asText("");
        assertEquals("example-org-tools", name, "call the marketplace example-org-tools");
        assertTrue(ID.matcher(name).matches() && !name.contains(".."));
        assertTrue(!RESERVED.contains(name) && !name.startsWith("claudeai-"));
        assertFalse(mentionsAnthropic(name.toLowerCase()), "a name that mentions claude or anthropic risks being refused as an impersonation");
        List<String> pluginNames = new ArrayList<>();
        for (JsonNode p : entries()) pluginNames.add(p.path("name").asText());
        pluginNames.sort(null);
        assertEquals(List.of("db-tools", "lint-helper", "standards-kit"), pluginNames, "list standards-kit, lint-helper and db-tools");
        for (String n : pluginNames) {
            String low = n.toLowerCase();
            assertTrue(ID.matcher(n).matches(), n + ": letters, digits, dots, underscores and hyphens only");
            assertTrue(Stream.of("claude-", "anthropic-", "anthropics-", "cc-plugin-").noneMatch(low::startsWith) && !List.of("claude", "anthropic", "anthropics", "claude-code", "claude-mods").contains(low), n + " passes as an Anthropic plugin");
            assertFalse(low.contains("official") && mentionsAnthropic(low), n + " passes as an official plugin");
        }
        assertEquals("standards-kit", manifest().path("name").asText(null));
    }

    @Test
    void e2_thePluginKeepsOnlyItsManifestInTheManifestFolderAndFindsItsFilesThroughThePluginRoot() throws IOException {
        List<String> inManifestFolder = new ArrayList<>();
        try (Stream<Path> s = Files.list(ROOT.resolve(PLUGIN).resolve(".claude-plugin"))) {
            s.forEach(p -> inManifestFolder.add(p.getFileName().toString()));
        }
        assertEquals(List.of("plugin.json"), inManifestFolder, "only plugin.json goes inside .claude-plugin");
        assertFalse(Files.exists(ROOT.resolve(PLUGIN).resolve("CLAUDE.md")), "an instruction file at the plugin root is not loaded as context");
        String[] body = new String[1];
        Map<String, String> fields = front(PLUGIN + "/skills/changelog/SKILL.md", body);
        assertTrue("changelog".equals(fields.get("name")) && fields.getOrDefault("description", "").contains("Use when") && !body[0].isBlank() && !body[0].contains("TODO"));
        JsonNode hooks = readJson(PLUGIN + "/hooks/hooks.json").path("hooks");
        assertTrue(hooks.isObject(), "hooks.json wraps the event map in a top-level hooks key");
        JsonNode groups = hooks.path("PreToolUse");
        assertTrue(groups.size() == 1 && "Edit|Write".equals(groups.get(0).path("matcher").asText(null)), "one PreToolUse group for Edit|Write");
        JsonNode handler = groups.get(0).path("hooks").path(0);
        assertTrue("command".equals(handler.path("type").asText(null)) && handler.path("command").asText("").contains("${CLAUDE_PLUGIN_ROOT}"), "reach the script through ${CLAUDE_PLUGIN_ROOT}");
        assertTrue(Files.isRegularFile(ROOT.resolve(PLUGIN).resolve("scripts/protect.sh")));
        JsonNode servers = readJson(PLUGIN + "/.mcp.json").path("mcpServers");
        assertTrue(servers.size() > 0, "declare the ticket server");
        for (JsonNode server : servers) {
            List<String> args = new ArrayList<>();
            server.path("args").forEach(a -> args.add(a.asText()));
            assertTrue(String.join(" ", args).contains("${CLAUDE_PLUGIN_ROOT}"), "every server reaches its code through ${CLAUDE_PLUGIN_ROOT}");
        }
        for (String key : Arrays.asList("skills", "commands", "agents", "hooks", "mcpServers")) {
            JsonNode value = manifest().path(key);
            List<String> paths = new ArrayList<>();
            if (value.isTextual()) paths.add(value.asText());
            else if (value.isArray()) value.forEach(v -> paths.add(v.asText()));
            for (String path : paths) assertTrue(path.startsWith("./"), "a component path in plugin.json starts with ./ (" + key + ")");
        }
    }

    @Test
    void e3_theVersionLivesInTheManifestAloneAndIsSemantic() {
        JsonNode data = manifest();
        assertTrue(SEMVER.matcher(data.path("version").asText("")).matches(), "give the plugin a semantic version in plugin.json");
        assertFalse(data.path("description").asText("").isBlank());
        assertFalse(entry("standards-kit").has("version"), "set the version in plugin.json or in the entry, not both");
    }

    @Test
    void e4_eachSourceKindIsWrittenInItsOwnFormAndPinnedToATagAndACommit() {
        assertEquals("./plugins/standards-kit", entry("standards-kit").path("source").asText(null));
        JsonNode helper = entry("lint-helper").path("source");
        assertTrue("github".equals(helper.path("source").asText(null)) && REPO.matcher(helper.path("repo").asText("")).matches(), "a github source takes owner/repo");
        JsonNode sub = entry("db-tools").path("source");
        assertTrue("git-subdir".equals(sub.path("source").asText(null)) && "tools/db-tools".equals(sub.path("path").asText(null)) && !sub.path("url").asText("").isEmpty(), "a git-subdir source takes a url and a path");
        for (JsonNode p : entries()) {
            JsonNode source = p.get("source");
            if (source.isTextual()) continue;
            String name = p.path("name").asText(), type = source.path("source").asText("");
            if (type.equals("url")) assertTrue(source.path("url").asText("").matches("(https?://|file://|git@).*"), name + ": a url source takes a full git url, not owner/repo");
            if (List.of("github", "url", "git-subdir").contains(type)) {
                assertTrue(TAG.matcher(source.path("ref").asText("")).matches(), name + ": pin ref to a release tag such as v1.2.0");
                assertTrue(SHA.matcher(source.path("sha").asText("")).matches(), name + ": pin sha to a full 40-character lowercase commit");
            }
            if (type.equals("archive")) assertTrue(source.path("sha256").asText("").matches("[0-9a-fA-F]{64}"), name + ": pin an archive with sha256");
        }
    }

    @Test
    void e5_aDependencyCarriesARangeAndOneFromAnotherMarketplaceIsAllowedByName() {
        assertEquals(jsonText("[{\"name\": \"lint-helper\", \"version\": \"~1.2.0\"}]"), manifest().get("dependencies"), "depend on lint-helper ~1.2.0");
        assertEquals(jsonText("[{\"name\": \"audit-logger\", \"marketplace\": \"shared-tools\"}]"), entry("db-tools").get("dependencies"), "db-tools depends on audit-logger from shared-tools");
        assertEquals(jsonText("[\"shared-tools\"]"), market().get("allowCrossMarketplaceDependenciesOn"), "the root marketplace must allow shared-tools");
    }

    @Test
    void e6_theTeamSettingsRegisterTheMarketplaceAndEnableOnlyWhatInstallsFromIt() {
        JsonNode settings = readJson(".claude/settings.json");
        JsonNode markets = settings.path("extraKnownMarketplaces");
        assertEquals(List.of(market().path("name").asText(null)), names(markets), "register the marketplace under its own name");
        JsonNode source = markets.get(market().path("name").asText()).path("source");
        assertTrue("github".equals(source.path("source").asText(null)) && REPO.matcher(source.path("repo").asText("")).matches(), "a github source with an owner/name repo");
        JsonNode enabled = settings.path("enabledPlugins");
        assertTrue(enabled.size() > 0);
        for (String id : names(enabled)) {
            assertTrue(enabled.get(id).isBoolean() && enabled.get(id).asBoolean(), id + " must be true");
            int at = id.indexOf('@');
            String name = at < 0 ? id : id.substring(0, at), marketplace = at < 0 ? "" : id.substring(at + 1);
            assertEquals(market().path("name").asText(), marketplace, id + ": enable plugins from the marketplace you register");
            assertTrue(entry(name).path("source").isTextual(), id + ": a plugin from an external source is not installed by the repository settings alone");
        }
        assertTrue(enabled.has("standards-kit@example-org-tools"));
    }

    @Test
    void e7_renamesLeadEveryFormerNameToACurrentPluginOrToNull() {
        JsonNode renames = market().path("renames");
        assertTrue(renames.isObject() && "standards-kit".equals(renames.path("std-kit").asText(null)) && renames.has("old-linter") && renames.get("old-linter").isNull());
        Set<String> current = new HashSet<>();
        for (JsonNode p : entries()) current.add(p.path("name").asText());
        for (String old : names(renames)) {
            assertFalse(current.contains(old), old + " is still listed as a plugin");
            Set<String> seen = new HashSet<>(Set.of(old));
            JsonNode step = renames.get(old);
            while (!step.isNull() && !current.contains(step.asText())) {
                assertTrue(renames.has(step.asText()) && !seen.contains(step.asText()), "the rename of " + old + " does not end at a plugin or at null");
                seen.add(step.asText());
                step = renames.get(step.asText());
            }
        }
    }

    @Test
    void e8_noFileIsLeftUnfinishedOrHoldsAPersonalPathAnAddressOrAKey() throws IOException {
        List<Path> all = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(ROOT)) {
            walk.filter(Files::isRegularFile).sorted().forEach(all::add);
        }
        Pattern personal = Pattern.compile("/home/|/Users/|[A-Za-z]:\\\\Users|sk-ant-");
        Pattern address = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
        for (Path path : all) {
            String text = Files.readString(path), rel = ROOT.relativize(path).toString();
            assertFalse(text.contains("TODO"), rel + " still has a TODO");
            assertFalse(personal.matcher(text).find(), rel + " holds a personal path or a key");
            Matcher m = address.matcher(text);
            while (m.find()) assertTrue(m.group().endsWith("@example.com"), rel + " holds an address that is not a placeholder");
        }
        assertTrue(all.size() >= 8);
    }
}
