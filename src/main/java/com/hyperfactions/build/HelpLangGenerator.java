package com.hyperfactions.build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Build-time tool that converts help markdown files into .lang translation files
 * and a help-manifest.json for the HyperFactions help system.
 *
 * <p>Usage: {@code java HelpLangGenerator <langDir> <outputDir>}
 *
 * <p>Reads {@code Server/Languages/{locale}/help/{category}/{topic}.md} and produces:
 * <ul>
 *   <li>{@code {outputDir}/Server/Languages/{locale}/hyperfactions_help.lang}</li>
 *   <li>{@code {outputDir}/help-manifest.json} (generated from en-US only)</li>
 * </ul>
 *
 * <h3>Supported Markdown Syntax</h3>
 * <pre>
 * Plain text              → TEXT
 * ## Heading              → HEADING
 * `command`               → COMMAND
 * **bold text**           → BOLD
 * *italic text*           → ITALIC
 * - list item             → LIST
 * 1. numbered item        → LIST
 * ---                     → SEPARATOR
 * [#RRGGBB] text          → TEXT + color
 * !warning text           → TEXT + #FF5555
 * !success text           → TEXT + #55FF55
 * !note text              → TEXT + #55AAFF
 * !muted text             → TEXT + #888888
 * > tip text              → CALLOUT + #55FF55
 * >[!TIP] text            → CALLOUT + #55FF55
 * >[!WARNING] text        → CALLOUT + #FF5555
 * >[!INFO] text           → CALLOUT + #55AAFF
 * >[!NOTE] text           → CALLOUT + #FFAA55
 * >[!SUCCESS] text        → CALLOUT + #55FF55
 * blank line              → SPACER
 * </pre>
 */
public class HelpLangGenerator {

    /** Fixed category processing order. */
    private static final List<String> CATEGORY_ORDER = List.of(
            "welcome", "your_faction", "power_land", "diplomacy", "combat", "economy", "quick_ref"
    );

    /** Pattern for inline hex color: [#RRGGBB] text */
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^\\[#([0-9A-Fa-f]{6})]\\s*(.+)$");

    /** Pattern for callout with type: >[!TYPE] text */
    private static final Pattern CALLOUT_TYPE_PATTERN = Pattern.compile("^>\\[!([A-Z]+)]\\s*(.+)$");

    /** Pattern for numbered list: 1. text, 2. text, etc. */
    private static final Pattern NUMBERED_LIST_PATTERN = Pattern.compile("^\\d+\\.\\s+(.+)$");

    /** Pattern for horizontal rule: 3+ dashes on a line */
    private static final Pattern HR_PATTERN = Pattern.compile("^-{3,}$");

    /** Named color shortcuts */
    private static final Map<String, String> NAMED_COLORS = Map.of(
            "warning", "#FF5555",
            "success", "#55FF55",
            "note", "#55AAFF",
            "muted", "#888888"
    );

    /** Callout type colors */
    private static final Map<String, String> CALLOUT_COLORS = Map.of(
            "TIP", "#55FF55",
            "WARNING", "#FF5555",
            "INFO", "#55AAFF",
            "NOTE", "#FFAA55",
            "SUCCESS", "#55FF55"
    );

    // ── Data structures ──────────────────────────────────────────────────

    /** A single parsed entry from a markdown topic file. */
    record Entry(String type, String key, String color) {
        Entry(String type, String key) {
            this(type, key, null);
        }
    }

    /** A fully parsed topic ready for manifest / lang output. */
    record Topic(
            String id,
            String category,
            String topic,
            String titleKey,
            String titleText,
            List<String> commands,
            List<Entry> entries,
            List<String> entryTexts
    ) {}

    // ── Entry point ──────────────────────────────────────────────────────

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: HelpLangGenerator <langDir> <outputDir>");
            System.exit(1);
        }

        Path langDir = Paths.get(args[0]);
        Path outputDir = Paths.get(args[1]);

        if (!Files.isDirectory(langDir)) {
            System.err.println("Languages directory not found: " + langDir);
            System.exit(1);
        }

        try {
            // Find locales that have a help/ subdirectory
            List<String> locales = listSortedDirectories(langDir).stream()
                    .filter(d -> Files.isDirectory(langDir.resolve(d).resolve("help")))
                    .toList();
            if (locales.isEmpty()) {
                System.err.println("No locale directories with help/ found under " + langDir);
                System.exit(1);
            }

            System.out.println("Found locales with help content: " + locales);

            for (String locale : locales) {
                Path helpDir = langDir.resolve(locale).resolve("help");
                List<Topic> topics = parseLocale(helpDir);
                writeLangFile(outputDir, locale, topics);

                if ("en-US".equals(locale)) {
                    writeManifest(outputDir, topics);
                }
            }

            System.out.println("Help language generation complete.");
        } catch (IOException e) {
            System.err.println("Error generating help lang files: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // ── Locale parsing ───────────────────────────────────────────────────

    private static List<Topic> parseLocale(Path localeDir) throws IOException {
        List<Topic> topics = new ArrayList<>();

        // Process categories in defined order, skip any that don't exist
        for (String category : CATEGORY_ORDER) {
            Path categoryDir = localeDir.resolve(category);
            if (!Files.isDirectory(categoryDir)) {
                continue;
            }

            List<Path> mdFiles = listMarkdownFiles(categoryDir);
            for (Path mdFile : mdFiles) {
                Topic topic = parseTopic(category, mdFile);
                if (topic != null) {
                    topics.add(topic);
                    System.out.println("  Parsed: " + category + "/" + mdFile.getFileName());
                }
            }
        }

        return topics;
    }

    // ── Markdown parsing ─────────────────────────────────────────────────

    private static Topic parseTopic(String category, Path mdFile) throws IOException {
        String filename = mdFile.getFileName().toString();
        String topicName = filename.substring(0, filename.length() - 3); // strip .md

        List<String> lines = Files.readAllLines(mdFile);

        // Parse frontmatter
        String id = null;
        List<String> commands = new ArrayList<>();
        int contentStart = 0;
        boolean inFrontmatter = false;

        if (!lines.isEmpty() && "---".equals(lines.get(0).trim())) {
            inFrontmatter = true;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if ("---".equals(line)) {
                    contentStart = i + 1;
                    inFrontmatter = false;
                    break;
                }
                if (line.startsWith("id:")) {
                    id = line.substring(3).trim();
                } else if (line.startsWith("commands:")) {
                    String commandStr = line.substring(9).trim();
                    for (String cmd : commandStr.split(",")) {
                        String trimmed = cmd.trim();
                        if (!trimmed.isEmpty()) {
                            commands.add(trimmed);
                        }
                    }
                }
            }
        }

        if (id == null) {
            id = category + "_" + topicName;
        }

        // Parse content lines
        String titleText = null;
        boolean foundFirstContent = false;
        String keyPrefix = category + "." + topicName;
        List<Entry> entries = new ArrayList<>();
        List<String> entryTexts = new ArrayList<>();
        int lineCounter = 0;

        for (int i = contentStart; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();

            // Skip blank lines before the title is found
            if (trimmed.isEmpty() && titleText == null) {
                continue;
            }

            if (trimmed.startsWith("# ") && titleText == null) {
                // First H1 → title
                titleText = trimmed.substring(2).trim();
                continue;
            }

            // Skip blank lines between title and first content
            if (trimmed.isEmpty() && !foundFirstContent) {
                continue;
            }

            if (trimmed.isEmpty()) {
                // Blank line → SPACER (only after first content line)
                entries.add(new Entry("SPACER", null));
                entryTexts.add(null);
                continue;
            }

            foundFirstContent = true;

            // ── Order matters: check specific patterns before plain text ──

            // 1. Horizontal rule: --- (3+ dashes, not in frontmatter context)
            if (HR_PATTERN.matcher(trimmed).matches()) {
                entries.add(new Entry("SEPARATOR", null));
                entryTexts.add(null);
                continue;
            }

            // 2. Callout with explicit type: >[!WARNING] text, >[!TIP] text, etc.
            Matcher calloutMatcher = CALLOUT_TYPE_PATTERN.matcher(trimmed);
            if (calloutMatcher.matches()) {
                String calloutType = calloutMatcher.group(1);
                String text = calloutMatcher.group(2).trim();
                String color = CALLOUT_COLORS.getOrDefault(calloutType, "#55FF55");
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                entries.add(new Entry("CALLOUT", key, color));
                entryTexts.add(text);
                continue;
            }

            // 3. Simple blockquote → CALLOUT (tip shorthand, green)
            if (trimmed.startsWith("> ")) {
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                String text = trimmed.substring(2).trim();
                entries.add(new Entry("CALLOUT", key, "#55FF55"));
                entryTexts.add(text);
                continue;
            }

            // 4. Inline hex color: [#RRGGBB] text
            Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(trimmed);
            if (hexMatcher.matches()) {
                String color = "#" + hexMatcher.group(1);
                String text = hexMatcher.group(2).trim();
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                entries.add(new Entry("TEXT", key, color));
                entryTexts.add(text);
                continue;
            }

            // 5. Named color shortcuts: !warning, !success, !note, !muted
            if (trimmed.startsWith("!")) {
                String rest = trimmed.substring(1);
                int spaceIdx = rest.indexOf(' ');
                if (spaceIdx > 0) {
                    String colorName = rest.substring(0, spaceIdx).toLowerCase();
                    String color = NAMED_COLORS.get(colorName);
                    if (color != null) {
                        String text = rest.substring(spaceIdx + 1).trim();
                        lineCounter++;
                        String key = keyPrefix + ".line." + lineCounter;
                        entries.add(new Entry("TEXT", key, color));
                        entryTexts.add(text);
                        continue;
                    }
                }
            }

            // 6. Bold: **text** (whole line wrapped)
            if (trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length() > 4) {
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                String text = trimmed.substring(2, trimmed.length() - 2);
                entries.add(new Entry("BOLD", key));
                entryTexts.add(text);
                continue;
            }

            // 7. Italic: *text* (whole line wrapped, but not bold **)
            if (trimmed.startsWith("*") && trimmed.endsWith("*") && !trimmed.startsWith("**") && trimmed.length() > 2) {
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                String text = trimmed.substring(1, trimmed.length() - 1);
                entries.add(new Entry("ITALIC", key));
                entryTexts.add(text);
                continue;
            }

            // 8. Bullet list: - text
            if (trimmed.startsWith("- ")) {
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                String text = trimmed.substring(2).trim();
                entries.add(new Entry("LIST", key));
                entryTexts.add(text);
                continue;
            }

            // 9. Numbered list: 1. text, 2. text, etc.
            Matcher numberedMatcher = NUMBERED_LIST_PATTERN.matcher(trimmed);
            if (numberedMatcher.matches()) {
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                // Preserve the number prefix as part of the text
                entries.add(new Entry("LIST", key));
                entryTexts.add(trimmed);
                continue;
            }

            // 10. H2 → HEADING
            if (trimmed.startsWith("## ")) {
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                String text = trimmed.substring(3).trim();
                entries.add(new Entry("HEADING", key));
                entryTexts.add(text);
                continue;
            }

            // 11. Command line (backtick-wrapped)
            if (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length() > 2) {
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                String text = trimmed.substring(1, trimmed.length() - 1);
                entries.add(new Entry("COMMAND", key));
                entryTexts.add(text);
                continue;
            }

            // 12. Plain text → TEXT
            lineCounter++;
            String key = keyPrefix + ".line." + lineCounter;
            entries.add(new Entry("TEXT", key));
            entryTexts.add(trimmed);
        }

        if (titleText == null) {
            titleText = topicName.replace('_', ' ');
        }

        return new Topic(id, category, topicName, keyPrefix + ".title", titleText, commands, entries, entryTexts);
    }

    // ── .lang file output ────────────────────────────────────────────────

    private static void writeLangFile(Path outputDir, String locale, List<Topic> topics) throws IOException {
        Path langDir = outputDir.resolve("Server").resolve("Languages").resolve(locale);
        Files.createDirectories(langDir);
        Path langFile = langDir.resolve("hyperfactions_help.lang");

        StringBuilder sb = new StringBuilder();
        sb.append("# HyperFactions Help System - ").append(locale).append("\n");
        sb.append("# AUTO-GENERATED by HelpLangGenerator — do not edit manually\n\n");

        for (Topic topic : topics) {
            sb.append("# AUTO-GENERATED from Server/Languages/")
                    .append(locale).append("/help/")
                    .append(topic.category()).append("/")
                    .append(topic.topic()).append(".md\n");

            sb.append(topic.category()).append(".").append(topic.topic())
                    .append(".title = ").append(topic.titleText()).append("\n");

            for (int i = 0; i < topic.entries().size(); i++) {
                Entry entry = topic.entries().get(i);
                if (entry.key() != null) {
                    String text = topic.entryTexts().get(i);
                    sb.append(entry.key()).append(" = ").append(text).append("\n");
                }
            }

            sb.append("\n");
        }

        Files.writeString(langFile, sb.toString());
        System.out.println("Wrote: " + langFile);
    }

    // ── Manifest output ──────────────────────────────────────────────────

    private static void writeManifest(Path outputDir, List<Topic> topics) throws IOException {
        List<Map<String, Object>> topicList = new ArrayList<>();
        Map<String, String> commandMappings = new LinkedHashMap<>();

        for (Topic topic : topics) {
            Map<String, Object> topicMap = new LinkedHashMap<>();
            topicMap.put("id", topic.id());
            topicMap.put("category", topic.category());
            topicMap.put("titleKey", "hyperfactions_help." + topic.titleKey());
            topicMap.put("commands", topic.commands());

            List<Map<String, String>> entryList = new ArrayList<>();
            for (int i = 0; i < topic.entries().size(); i++) {
                Entry entry = topic.entries().get(i);
                Map<String, String> entryMap = new LinkedHashMap<>();
                entryMap.put("type", entry.type());
                if (entry.key() != null) {
                    entryMap.put("key", "hyperfactions_help." + entry.key());
                }
                if (entry.color() != null) {
                    entryMap.put("color", entry.color());
                }
                entryList.add(entryMap);
            }
            topicMap.put("entries", entryList);

            topicList.add(topicMap);

            // Build command mappings
            for (String cmd : topic.commands()) {
                commandMappings.put(cmd, topic.category());
            }
        }

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("topics", topicList);
        manifest.put("commandMappings", commandMappings);

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String json = gson.toJson(manifest);

        Path manifestFile = outputDir.resolve("help-manifest.json");
        Files.createDirectories(manifestFile.getParent());
        Files.writeString(manifestFile, json + "\n");
        System.out.println("Wrote: " + manifestFile);
    }

    // ── Utility ──────────────────────────────────────────────────────────

    private static List<String> listSortedDirectories(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private static List<Path> listMarkdownFiles(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        }
    }
}
