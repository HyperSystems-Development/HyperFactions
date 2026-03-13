package com.hyperfactions.gui.help;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyperfactions.util.Logger;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.StringJoiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central registry of all help content.
 * Loads topic structure from a build-generated {@code help-manifest.json}
 * and provides lookup by category, topic ID, or command name.
 */
public final class HelpRegistry {

  private static final HelpRegistry INSTANCE = new HelpRegistry();

  private final Map<HelpCategory, List<HelpTopic>> topicsByCategory = new EnumMap<>(HelpCategory.class);

  private final Map<String, HelpTopic> topicsById = new HashMap<>();

  private final Map<String, HelpCategory> categoryByCommand = new HashMap<>();

  private HelpRegistry() {
    loadFromManifest();
    registerAdditionalCommandMappings();
  }

  /** Returns the instance. */
  public static HelpRegistry getInstance() {
    return INSTANCE;
  }

  /** Returns the topics. */
  @NotNull
  public List<HelpTopic> getTopics(@NotNull HelpCategory category) {
    return topicsByCategory.getOrDefault(category, List.of());
  }

  /** Returns the topic. */
  @Nullable
  public HelpTopic getTopic(@NotNull String topicId) {
    return topicsById.get(topicId);
  }

  /** Returns the category for command. */
  @Nullable
  public HelpCategory getCategoryForCommand(@NotNull String command) {
    return categoryByCommand.get(command.toLowerCase());
  }

  private void register(@NotNull HelpTopic topic) {
    topicsByCategory.computeIfAbsent(topic.category(), k -> new ArrayList<>()).add(topic);
    topicsById.put(topic.id(), topic);
    for (String cmd : topic.commands()) {
      categoryByCommand.put(cmd.toLowerCase(), topic.category());
    }
  }

  private void registerCommandMapping(@NotNull String command, @NotNull HelpCategory category) {
    categoryByCommand.put(command.toLowerCase(), category);
  }

  /**
   * Loads help content structure from the build-generated help-manifest.json.
   */
  private void loadFromManifest() {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream("help-manifest.json")) {
      if (is == null) {
        Logger.warn("help-manifest.json not found in classpath — help system will be empty");
        return;
      }

      Gson gson = new Gson();
      JsonObject manifest = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);

      // Load topics
      JsonArray topics = manifest.getAsJsonArray("topics");
      if (topics != null) {
        for (JsonElement topicElement : topics) {
          JsonObject topicObj = topicElement.getAsJsonObject();
          HelpTopic topic = parseTopic(topicObj);
          if (topic != null) {
            register(topic);
          }
        }
      }

      // Load additional command mappings from manifest
      JsonObject cmdMappings = manifest.getAsJsonObject("commandMappings");
      if (cmdMappings != null) {
        for (Map.Entry<String, JsonElement> entry : cmdMappings.entrySet()) {
          String cmd = entry.getKey();
          String categoryId = entry.getValue().getAsString();
          HelpCategory category = HelpCategory.fromId(categoryId);
          // Only add if not already mapped by a topic's commands
          categoryByCommand.putIfAbsent(cmd.toLowerCase(), category);
        }
      }

      Logger.info("Loaded %d help topics from manifest", topicsById.size());
    } catch (Exception e) {
      Logger.warn("Failed to load help manifest: %s", e.getMessage());
    }
  }

  /**
   * Parses a single topic from the manifest JSON.
   */
  @Nullable
  private HelpTopic parseTopic(@NotNull JsonObject topicObj) {
    String id = topicObj.get("id").getAsString();
    String categoryId = topicObj.get("category").getAsString();
    String titleKey = topicObj.get("titleKey").getAsString();

    HelpCategory category = HelpCategory.fromId(categoryId);

    // Parse commands
    List<String> commands = new ArrayList<>();
    JsonArray cmds = topicObj.getAsJsonArray("commands");
    if (cmds != null) {
      for (JsonElement cmd : cmds) {
        commands.add(cmd.getAsString());
      }
    }

    // Parse entries
    List<HelpEntry> entries = new ArrayList<>();
    JsonArray entriesArray = topicObj.getAsJsonArray("entries");
    if (entriesArray != null) {
      for (JsonElement entryElement : entriesArray) {
        JsonObject entryObj = entryElement.getAsJsonObject();
        String type = entryObj.get("type").getAsString();
        String key = entryObj.has("key") ? entryObj.get("key").getAsString() : "";
        String color = entryObj.has("color") ? entryObj.get("color").getAsString() : null;

        HelpEntry entry = switch (type) {
          case "TEXT" -> color != null ? HelpEntry.colored(key, color) : HelpEntry.text(key);
          case "COMMAND" -> HelpEntry.command(key);
          case "TIP" -> HelpEntry.callout(key, "#55FF55"); // backward compat
          case "HEADING" -> HelpEntry.heading(key);
          case "SPACER" -> HelpEntry.spacer();
          case "BOLD" -> HelpEntry.bold(key);
          case "ITALIC" -> HelpEntry.italic(key);
          case "LIST" -> HelpEntry.list(key);
          case "SEPARATOR" -> HelpEntry.separator();
          case "CALLOUT" -> HelpEntry.callout(key, color);
          case "TABLE_HEADER", "TABLE_ROW" -> {
            // Table entries store column keys as a JSON array
            JsonArray cols = entryObj.has("columns") ? entryObj.getAsJsonArray("columns") : null;
            if (cols != null && !cols.isEmpty()) {
              StringJoiner joiner = new StringJoiner("|");
              for (JsonElement col : cols) {
                joiner.add(col.getAsString());
              }
              yield "TABLE_HEADER".equals(type)
                  ? HelpEntry.tableHeader(joiner.toString())
                  : HelpEntry.tableRow(joiner.toString());
            }
            yield null;
          }
          default -> null;
        };
        if (entry != null) {
          entries.add(entry);
        }
      }
    }

    if (commands.isEmpty()) {
      return HelpTopic.of(id, titleKey, entries, category);
    }
    return HelpTopic.withCommands(id, titleKey, entries, commands, category);
  }

  /**
   * Registers additional command → category mappings that aren't tied to specific topics.
   * These provide general navigation from any command to its relevant help category.
   */
  private void registerAdditionalCommandMappings() {
    registerCommandMapping("help", HelpCategory.WELCOME);

    registerCommandMapping("info", HelpCategory.YOUR_FACTION);
    registerCommandMapping("show", HelpCategory.YOUR_FACTION);
    registerCommandMapping("list", HelpCategory.YOUR_FACTION);
    registerCommandMapping("browse", HelpCategory.YOUR_FACTION);
    registerCommandMapping("members", HelpCategory.YOUR_FACTION);
    registerCommandMapping("invites", HelpCategory.YOUR_FACTION);
    registerCommandMapping("who", HelpCategory.YOUR_FACTION);
    registerCommandMapping("disband", HelpCategory.YOUR_FACTION);
    registerCommandMapping("leave", HelpCategory.YOUR_FACTION);

    registerCommandMapping("sethome", HelpCategory.POWER_AND_LAND);
    registerCommandMapping("home", HelpCategory.POWER_AND_LAND);
    registerCommandMapping("delhome", HelpCategory.POWER_AND_LAND);
    registerCommandMapping("stuck", HelpCategory.POWER_AND_LAND);

    registerCommandMapping("rename", HelpCategory.YOUR_FACTION);
    registerCommandMapping("desc", HelpCategory.YOUR_FACTION);
    registerCommandMapping("description", HelpCategory.YOUR_FACTION);
    registerCommandMapping("color", HelpCategory.YOUR_FACTION);
    registerCommandMapping("open", HelpCategory.YOUR_FACTION);
    registerCommandMapping("close", HelpCategory.YOUR_FACTION);
    registerCommandMapping("settings", HelpCategory.YOUR_FACTION);

    registerCommandMapping("chat", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("c", HelpCategory.QUICK_REFERENCE);

    registerCommandMapping("admin", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("reload", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("sync", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("zone", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("zones", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("safezone", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("warzone", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("removezone", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("zoneflag", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("backup", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("backups", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("config", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("factions", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("update", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("import", HelpCategory.QUICK_REFERENCE);
    registerCommandMapping("debug", HelpCategory.QUICK_REFERENCE);
  }
}
