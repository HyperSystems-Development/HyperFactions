package com.hyperfactions.build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Build-time tool that converts help markdown files into .lang translation files
 * and a help-manifest.json for the HyperFactions help system.
 *
 * <p>Usage: {@code java HelpLangGenerator <helpDir> <outputDir>}
 *
 * <p>Reads {@code src/main/help/{locale}/{category}/{topic}.md} and produces:
 * <ul>
 *   <li>{@code {outputDir}/Server/Languages/{locale}/hyperfactions_help.lang}</li>
 *   <li>{@code {outputDir}/help-manifest.json} (generated from en-US only)</li>
 * </ul>
 */
public class HelpLangGenerator {

    /** Fixed category processing order. */
    private static final List<String> CATEGORY_ORDER = List.of(
            "welcome", "your_faction", "power_land", "diplomacy", "combat", "economy", "quick_ref"
    );

    // ── Data structures ──────────────────────────────────────────────────

    /** A single parsed entry from a markdown topic file. */
    record Entry(String type, String key) {}

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
            System.err.println("Usage: HelpLangGenerator <helpDir> <outputDir>");
            System.exit(1);
        }

        Path helpDir = Paths.get(args[0]);
        Path outputDir = Paths.get(args[1]);

        if (!Files.isDirectory(helpDir)) {
            System.err.println("Help directory not found: " + helpDir);
            System.exit(1);
        }

        try {
            List<String> locales = listSortedDirectories(helpDir);
            if (locales.isEmpty()) {
                System.err.println("No locale directories found under " + helpDir);
                System.exit(1);
            }

            System.out.println("Found locales: " + locales);

            for (String locale : locales) {
                Path localeDir = helpDir.resolve(locale);
                List<Topic> topics = parseLocale(localeDir);
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

        if (!lines.isEmpty() && "---".equals(lines.get(0).trim())) {
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if ("---".equals(line)) {
                    contentStart = i + 1;
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

            if (trimmed.startsWith("## ")) {
                // H2 → HEADING
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                String text = trimmed.substring(3).trim();
                entries.add(new Entry("HEADING", key));
                entryTexts.add(text);
                continue;
            }

            if (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length() > 2) {
                // Command line (backtick-wrapped)
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                String text = trimmed.substring(1, trimmed.length() - 1);
                entries.add(new Entry("COMMAND", key));
                entryTexts.add(text);
                continue;
            }

            if (trimmed.startsWith("> ")) {
                // Blockquote → TIP
                lineCounter++;
                String key = keyPrefix + ".line." + lineCounter;
                String text = trimmed.substring(2).trim();
                entries.add(new Entry("TIP", key));
                entryTexts.add(text);
                continue;
            }

            // Plain text → TEXT
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
            sb.append("# AUTO-GENERATED from src/main/help/")
                    .append(locale).append("/")
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
