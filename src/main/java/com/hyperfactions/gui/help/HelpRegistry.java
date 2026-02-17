package com.hyperfactions.gui.help;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.hyperfactions.gui.help.HelpEntry.*;

/**
 * Central registry of all help content.
 * Provides lookup by category, topic ID, or command name.
 */
public final class HelpRegistry {

    private static final HelpRegistry INSTANCE = new HelpRegistry();

    private final Map<HelpCategory, List<HelpTopic>> topicsByCategory = new EnumMap<>(HelpCategory.class);
    private final Map<String, HelpTopic> topicsById = new HashMap<>();
    private final Map<String, HelpCategory> categoryByCommand = new HashMap<>();

    private HelpRegistry() {
        initializeContent();
    }

    public static HelpRegistry getInstance() {
        return INSTANCE;
    }

    @NotNull
    public List<HelpTopic> getTopics(@NotNull HelpCategory category) {
        return topicsByCategory.getOrDefault(category, List.of());
    }

    @Nullable
    public HelpTopic getTopic(@NotNull String topicId) {
        return topicsById.get(topicId);
    }

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

    private static String k(String category, String topic, int line) {
        return "help." + category + "." + topic + ".line." + line;
    }

    private void initializeContent() {
        // =====================================================================
        // WELCOME
        // =====================================================================

        register(HelpTopic.of("welcome_what", "help.welcome.what_are_factions.title", List.of(
                text(k("welcome", "what_are_factions", 1)),
                text(k("welcome", "what_are_factions", 2)),
                spacer(),
                text(k("welcome", "what_are_factions", 3)),
                text(k("welcome", "what_are_factions", 4)),
                spacer(),
                text(k("welcome", "what_are_factions", 5)),
                text(k("welcome", "what_are_factions", 6)),
                text(k("welcome", "what_are_factions", 7))
        ), HelpCategory.WELCOME));

        register(HelpTopic.withCommands("welcome_started", "help.welcome.getting_started.title", List.of(
                text(k("welcome", "getting_started", 1)),
                spacer(),
                command(k("welcome", "getting_started", 2)),
                text(k("welcome", "getting_started", 3)),
                text(k("welcome", "getting_started", 4)),
                spacer(),
                text(k("welcome", "getting_started", 5)),
                text(k("welcome", "getting_started", 6)),
                spacer(),
                tip(k("welcome", "getting_started", 7))
        ), List.of("gui", "menu"), HelpCategory.WELCOME));

        register(HelpTopic.of("welcome_tips", "help.welcome.quick_tips.title", List.of(
                heading(k("welcome", "quick_tips", 1)),
                command(k("welcome", "quick_tips", 2)),
                text(k("welcome", "quick_tips", 3)),
                spacer(),
                heading(k("welcome", "quick_tips", 4)),
                command(k("welcome", "quick_tips", 5)),
                text(k("welcome", "quick_tips", 6)),
                spacer(),
                heading(k("welcome", "quick_tips", 7)),
                command(k("welcome", "quick_tips", 8)),
                text(k("welcome", "quick_tips", 9)),
                spacer(),
                tip(k("welcome", "quick_tips", 10))
        ), HelpCategory.WELCOME));

        // =====================================================================
        // YOUR FACTION
        // =====================================================================

        register(HelpTopic.withCommands("faction_creating", "help.your_faction.creating.title", List.of(
                text(k("your_faction", "creating", 1)),
                text(k("your_faction", "creating", 2)),
                spacer(),
                command(k("your_faction", "creating", 3)),
                text(k("your_faction", "creating", 4)),
                spacer(),
                tip(k("your_faction", "creating", 5))
        ), List.of("create"), HelpCategory.YOUR_FACTION));

        register(HelpTopic.withCommands("faction_joining", "help.your_faction.joining.title", List.of(
                text(k("your_faction", "joining", 1)),
                spacer(),
                heading(k("your_faction", "joining", 2)),
                text(k("your_faction", "joining", 3)),
                spacer(),
                heading(k("your_faction", "joining", 4)),
                text(k("your_faction", "joining", 5)),
                spacer(),
                heading(k("your_faction", "joining", 6)),
                command(k("your_faction", "joining", 7)),
                text(k("your_faction", "joining", 8))
        ), List.of("accept", "join", "request"), HelpCategory.YOUR_FACTION));

        register(HelpTopic.of("faction_roles", "help.your_faction.roles.title", List.of(
                text(k("your_faction", "roles", 1)),
                spacer(),
                heading(k("your_faction", "roles", 2)),
                text(k("your_faction", "roles", 3)),
                text(k("your_faction", "roles", 4)),
                spacer(),
                heading(k("your_faction", "roles", 5)),
                text(k("your_faction", "roles", 6)),
                spacer(),
                heading(k("your_faction", "roles", 7)),
                text(k("your_faction", "roles", 8))
        ), HelpCategory.YOUR_FACTION));

        register(HelpTopic.withCommands("faction_managing", "help.your_faction.managing.title", List.of(
                text(k("your_faction", "managing", 1)),
                spacer(),
                command(k("your_faction", "managing", 2)),
                text(k("your_faction", "managing", 3)),
                spacer(),
                command(k("your_faction", "managing", 4)),
                text(k("your_faction", "managing", 5)),
                spacer(),
                command(k("your_faction", "managing", 6)),
                text(k("your_faction", "managing", 7)),
                spacer(),
                command(k("your_faction", "managing", 8)),
                text(k("your_faction", "managing", 9)),
                spacer(),
                command(k("your_faction", "managing", 10)),
                tip(k("your_faction", "managing", 11))
        ), List.of("invite", "kick", "promote", "demote", "transfer"),
                HelpCategory.YOUR_FACTION));

        // =====================================================================
        // POWER & LAND
        // =====================================================================

        register(HelpTopic.withCommands("power_understanding", "help.power_land.understanding_power.title", List.of(
                text(k("power_land", "understanding_power", 1)),
                text(k("power_land", "understanding_power", 2)),
                spacer(),
                command(k("power_land", "understanding_power", 3)),
                text(k("power_land", "understanding_power", 4)),
                spacer(),
                text(k("power_land", "understanding_power", 5)),
                tip(k("power_land", "understanding_power", 6))
        ), List.of("power"), HelpCategory.POWER_AND_LAND));

        register(HelpTopic.withCommands("power_claiming", "help.power_land.claiming.title", List.of(
                text(k("power_land", "claiming", 1)),
                text(k("power_land", "claiming", 2)),
                spacer(),
                command(k("power_land", "claiming", 3)),
                text(k("power_land", "claiming", 4)),
                spacer(),
                command(k("power_land", "claiming", 5)),
                text(k("power_land", "claiming", 6)),
                spacer(),
                tip(k("power_land", "claiming", 7))
        ), List.of("claim", "unclaim"), HelpCategory.POWER_AND_LAND));

        register(HelpTopic.withCommands("power_map", "help.power_land.territory_map.title", List.of(
                text(k("power_land", "territory_map", 1)),
                spacer(),
                command(k("power_land", "territory_map", 2)),
                text(k("power_land", "territory_map", 3)),
                spacer(),
                text(k("power_land", "territory_map", 4)),
                text(k("power_land", "territory_map", 5))
        ), List.of("map"), HelpCategory.POWER_AND_LAND));

        register(HelpTopic.withCommands("power_losing", "help.power_land.losing_territory.title", List.of(
                text(k("power_land", "losing_territory", 1)),
                text(k("power_land", "losing_territory", 2)),
                spacer(),
                command(k("power_land", "losing_territory", 3)),
                text(k("power_land", "losing_territory", 4)),
                spacer(),
                text(k("power_land", "losing_territory", 5)),
                text(k("power_land", "losing_territory", 6))
        ), List.of("overclaim"), HelpCategory.POWER_AND_LAND));

        // =====================================================================
        // DIPLOMACY
        // =====================================================================

        register(HelpTopic.withCommands("diplomacy_relations", "help.diplomacy.relations.title", List.of(
                text(k("diplomacy", "relations", 1)),
                spacer(),
                text(k("diplomacy", "relations", 2)),
                text(k("diplomacy", "relations", 3)),
                spacer(),
                text(k("diplomacy", "relations", 4)),
                text(k("diplomacy", "relations", 5)),
                spacer(),
                text(k("diplomacy", "relations", 6)),
                spacer(),
                command(k("diplomacy", "relations", 7)),
                text(k("diplomacy", "relations", 8))
        ), List.of("relations"), HelpCategory.DIPLOMACY));

        register(HelpTopic.withCommands("diplomacy_alliances", "help.diplomacy.alliances.title", List.of(
                text(k("diplomacy", "alliances", 1)),
                text(k("diplomacy", "alliances", 2)),
                spacer(),
                command(k("diplomacy", "alliances", 3)),
                text(k("diplomacy", "alliances", 4)),
                spacer(),
                text(k("diplomacy", "alliances", 5)),
                tip(k("diplomacy", "alliances", 6))
        ), List.of("ally"), HelpCategory.DIPLOMACY));

        register(HelpTopic.withCommands("diplomacy_enemies", "help.diplomacy.enemies.title", List.of(
                text(k("diplomacy", "enemies", 1)),
                text(k("diplomacy", "enemies", 2)),
                spacer(),
                command(k("diplomacy", "enemies", 3)),
                text(k("diplomacy", "enemies", 4)),
                spacer(),
                text(k("diplomacy", "enemies", 5)),
                text(k("diplomacy", "enemies", 6)),
                spacer(),
                command(k("diplomacy", "enemies", 7)),
                text(k("diplomacy", "enemies", 8))
        ), List.of("enemy", "neutral"), HelpCategory.DIPLOMACY));

        // =====================================================================
        // COMBAT & SAFETY
        // =====================================================================

        register(HelpTopic.of("combat_tagging", "help.combat.tagging.title", List.of(
                text(k("combat", "tagging", 1)),
                text(k("combat", "tagging", 2)),
                spacer(),
                text(k("combat", "tagging", 3)),
                text(k("combat", "tagging", 4)),
                spacer(),
                tip(k("combat", "tagging", 5))
        ), HelpCategory.COMBAT));

        register(HelpTopic.of("combat_protection", "help.combat.protection.title", List.of(
                text(k("combat", "protection", 1)),
                spacer(),
                heading(k("combat", "protection", 2)),
                text(k("combat", "protection", 3)),
                spacer(),
                heading(k("combat", "protection", 4)),
                text(k("combat", "protection", 5)),
                spacer(),
                heading(k("combat", "protection", 6)),
                text(k("combat", "protection", 7)),
                spacer(),
                tip(k("combat", "protection", 8))
        ), HelpCategory.COMBAT));

        register(HelpTopic.of("combat_zones", "help.combat.zones.title", List.of(
                text(k("combat", "zones", 1)),
                spacer(),
                heading(k("combat", "zones", 2)),
                text(k("combat", "zones", 3)),
                spacer(),
                heading(k("combat", "zones", 4)),
                text(k("combat", "zones", 5)),
                spacer(),
                tip(k("combat", "zones", 6))
        ), HelpCategory.COMBAT));

        register(HelpTopic.withCommands("combat_death", "help.combat.death.title", List.of(
                text(k("combat", "death", 1)),
                spacer(),
                text(k("combat", "death", 2)),
                text(k("combat", "death", 3)),
                spacer(),
                text(k("combat", "death", 4)),
                text(k("combat", "death", 5)),
                spacer(),
                tip(k("combat", "death", 6))
        ), List.of("home", "sethome", "stuck"), HelpCategory.COMBAT));

        // =====================================================================
        // ECONOMY
        // =====================================================================

        register(HelpTopic.withCommands("economy_treasury", "help.economy.treasury.title", List.of(
                text(k("economy", "treasury", 1)),
                text(k("economy", "treasury", 2)),
                spacer(),
                command(k("economy", "treasury", 3)),
                text(k("economy", "treasury", 4)),
                spacer(),
                tip(k("economy", "treasury", 5))
        ), List.of("balance"), HelpCategory.ECONOMY));

        register(HelpTopic.withCommands("economy_funds", "help.economy.funds.title", List.of(
                text(k("economy", "funds", 1)),
                spacer(),
                command(k("economy", "funds", 2)),
                text(k("economy", "funds", 3)),
                spacer(),
                command(k("economy", "funds", 4)),
                text(k("economy", "funds", 5)),
                spacer(),
                command(k("economy", "funds", 6)),
                text(k("economy", "funds", 7)),
                spacer(),
                tip(k("economy", "funds", 8))
        ), List.of("deposit", "withdraw"), HelpCategory.ECONOMY));

        register(HelpTopic.of("economy_commands", "help.economy.commands.title", List.of(
                text(k("economy", "commands", 1)),
                spacer(),
                command(k("economy", "commands", 2)),
                text(k("economy", "commands", 3)),
                spacer(),
                command(k("economy", "commands", 4)),
                text(k("economy", "commands", 5)),
                spacer(),
                command(k("economy", "commands", 6)),
                text(k("economy", "commands", 7)),
                spacer(),
                command(k("economy", "commands", 8)),
                text(k("economy", "commands", 9)),
                spacer(),
                command(k("economy", "commands", 10)),
                text(k("economy", "commands", 11))
        ), HelpCategory.ECONOMY));

        // =====================================================================
        // QUICK REFERENCE — All Commands
        // =====================================================================

        List<HelpEntry> cmdEntries = new ArrayList<>();
        String prefix = "help.quick_ref.all_commands.line.";

        // Core (lines 1-6)
        cmdEntries.add(heading(prefix + "1"));
        for (int i = 2; i <= 6; i++) cmdEntries.add(command(prefix + i));
        cmdEntries.add(spacer());

        // Membership (lines 7-14)
        cmdEntries.add(heading(prefix + "7"));
        for (int i = 8; i <= 14; i++) cmdEntries.add(command(prefix + i));
        cmdEntries.add(spacer());

        // Territory (lines 15-19)
        cmdEntries.add(heading(prefix + "15"));
        for (int i = 16; i <= 19; i++) cmdEntries.add(command(prefix + i));
        cmdEntries.add(spacer());

        // Teleport (lines 20-24)
        cmdEntries.add(heading(prefix + "20"));
        for (int i = 21; i <= 24; i++) cmdEntries.add(command(prefix + i));
        cmdEntries.add(spacer());

        // Information (lines 25-32)
        cmdEntries.add(heading(prefix + "25"));
        for (int i = 26; i <= 32; i++) cmdEntries.add(command(prefix + i));
        cmdEntries.add(spacer());

        // Diplomacy (lines 33-36)
        cmdEntries.add(heading(prefix + "33"));
        for (int i = 34; i <= 36; i++) cmdEntries.add(command(prefix + i));
        cmdEntries.add(spacer());

        // Settings (lines 37-43)
        cmdEntries.add(heading(prefix + "37"));
        for (int i = 38; i <= 43; i++) cmdEntries.add(command(prefix + i));
        cmdEntries.add(spacer());

        // Economy (lines 44-49)
        cmdEntries.add(heading(prefix + "44"));
        for (int i = 45; i <= 49; i++) cmdEntries.add(command(prefix + i));
        cmdEntries.add(spacer());

        // Chat (lines 50-54)
        cmdEntries.add(heading(prefix + "50"));
        for (int i = 51; i <= 54; i++) cmdEntries.add(command(prefix + i));
        cmdEntries.add(spacer());

        // Admin (lines 55-66)
        cmdEntries.add(heading(prefix + "55"));
        for (int i = 56; i <= 66; i++) cmdEntries.add(command(prefix + i));

        register(HelpTopic.of("quickref_commands", "help.quick_ref.all_commands.title",
                cmdEntries, HelpCategory.QUICK_REFERENCE));

        // =====================================================================
        // Additional command → category mappings for deep-linking
        // =====================================================================

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
