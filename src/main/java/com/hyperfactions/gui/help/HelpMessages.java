package com.hyperfactions.gui.help;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Key-based string store for all help content.
 * Separates content from rendering code so future locale loading
 * only needs to swap this class's backing map.
 *
 * <p>i18n future path: Replace {@link #loadDefaults()} body with a
 * JSON/properties file loader keyed by locale. The {@link #get(String)}
 * API stays the same.</p>
 */
public final class HelpMessages {

  private static final Map<String, String> MESSAGES = new LinkedHashMap<>();

  static {
    loadDefaults();
  }

  private HelpMessages() {}

  /**
   * Gets the localized string for a message key.
   *
   * @param key The message key
   * @return The localized string, or the key itself if not found
   */
  @NotNull
  public static String get(@NotNull String key) {
    return MESSAGES.getOrDefault(key, key);
  }

  /**
   * Collects ordered lines for a topic.
   * Looks for keys matching {@code <topicKey>.line.1}, {@code .line.2}, etc.
   *
   * @param topicKey The topic key prefix (e.g. "help.welcome.what_are_factions")
   * @return Ordered list of line values
   */
  @NotNull
  public static List<String> getLines(@NotNull String topicKey) {
    List<String> lines = new ArrayList<>();
    for (int i = 1; ; i++) {
      String key = topicKey + ".line." + i;
      String value = MESSAGES.get(key);
      if (value == null) {
        break;
      }
      lines.add(value);
    }
    return lines;
  }

  private static void put(@NotNull String key, @NotNull String value) {
    MESSAGES.put(key, value);
  }

  private static void loadDefaults() {
    // =================================================================
    // Category names
    // =================================================================
    put("help.category.welcome", "Welcome");
    put("help.category.your_faction", "Your Faction");
    put("help.category.power_land", "Power & Land");
    put("help.category.diplomacy", "Diplomacy");
    put("help.category.combat", "Combat & Safety");
    put("help.category.economy", "Economy");
    put("help.category.quick_ref", "Quick Reference");

    // =================================================================
    // WELCOME
    // =================================================================

    // --- What Are Factions? ---
    put("help.welcome.what_are_factions.title", "What Are Factions?");
    put("help.welcome.what_are_factions.line.1",
        "Factions are player teams that claim territory,");
    put("help.welcome.what_are_factions.line.2",
        "build bases, and grow stronger together.");
    put("help.welcome.what_are_factions.line.3",
        "As a member you get protected land, a faction");
    put("help.welcome.what_are_factions.line.4",
        "home, private chat, and diplomatic relations.");
    put("help.welcome.what_are_factions.line.5",
        "Strength is measured by power. Active members");
    put("help.welcome.what_are_factions.line.6",
        "generate power; dying costs it. If power drops");
    put("help.welcome.what_are_factions.line.7",
        "below your claim count, enemies can steal land.");

    // --- Getting Started ---
    put("help.welcome.getting_started.title", "Getting Started");
    put("help.welcome.getting_started.line.1",
        "Ready to dive in? Here's how:");
    put("help.welcome.getting_started.line.2", "/f");
    put("help.welcome.getting_started.line.3",
        "Opens the faction menu. Browse factions, create");
    put("help.welcome.getting_started.line.4",
        "your own, or check invitations.");
    put("help.welcome.getting_started.line.5",
        "If invited, check the Invites tab and accept.");
    put("help.welcome.getting_started.line.6",
        "Otherwise, browse open factions or start fresh.");
    put("help.welcome.getting_started.line.7",
        "Once in, explore territory and start claiming!");

    // --- Quick Tips ---
    put("help.welcome.quick_tips.title", "Quick Tips");
    put("help.welcome.quick_tips.line.1", "Claiming Land");
    put("help.welcome.quick_tips.line.2", "/f claim");
    put("help.welcome.quick_tips.line.3",
        "Protects the chunk you're standing in.");
    put("help.welcome.quick_tips.line.4", "Faction Home");
    put("help.welcome.quick_tips.line.5", "/f home");
    put("help.welcome.quick_tips.line.6",
        "Teleports to your faction home. Set with /f sethome.");
    put("help.welcome.quick_tips.line.7", "Faction Chat");
    put("help.welcome.quick_tips.line.8", "/f c");
    put("help.welcome.quick_tips.line.9",
        "Cycles chat mode: Normal > Faction > Ally.");
    put("help.welcome.quick_tips.line.10",
        "Dying costs power, weakening your territory hold!");

    // =================================================================
    // YOUR FACTION
    // =================================================================

    // --- Creating a Faction ---
    put("help.your_faction.creating.title", "Creating a Faction");
    put("help.your_faction.creating.line.1",
        "Starting a faction makes you the Leader with");
    put("help.your_faction.creating.line.2",
        "full control over settings, members, and land.");
    put("help.your_faction.creating.line.3", "/f create <name>");
    put("help.your_faction.creating.line.4",
        "Creates a faction and opens your dashboard.");
    put("help.your_faction.creating.line.5",
        "Invite friends, claim land, and start building!");

    // --- Joining a Faction ---
    put("help.your_faction.joining.title", "Joining a Faction");
    put("help.your_faction.joining.line.1",
        "Three ways to join an existing faction:");
    put("help.your_faction.joining.line.2", "Browse Open Factions");
    put("help.your_faction.joining.line.3",
        "Open /f and click Browse. Click Join on any open faction.");
    put("help.your_faction.joining.line.4", "Accept an Invitation");
    put("help.your_faction.joining.line.5",
        "Check the Invites tab and click Accept.");
    put("help.your_faction.joining.line.6", "Request to Join");
    put("help.your_faction.joining.line.7", "/f request <faction>");
    put("help.your_faction.joining.line.8",
        "Send a request to an invite-only faction.");

    // --- Roles & Ranks ---
    put("help.your_faction.roles.title", "Roles & Ranks");
    put("help.your_faction.roles.line.1",
        "Three ranks with different capabilities:");
    put("help.your_faction.roles.line.2", "Leader (1 per faction)");
    put("help.your_faction.roles.line.3",
        "Full control: disband, transfer ownership,");
    put("help.your_faction.roles.line.4",
        "promote/demote, plus all Officer permissions.");
    put("help.your_faction.roles.line.5", "Officer");
    put("help.your_faction.roles.line.6",
        "Invite/kick, claim/unclaim, set home, relations.");
    put("help.your_faction.roles.line.7", "Member");
    put("help.your_faction.roles.line.8",
        "Use faction home, chat, build in territory.");

    // --- Managing Members ---
    put("help.your_faction.managing.title", "Managing Members");
    put("help.your_faction.managing.line.1",
        "Officers and Leaders manage the roster:");
    put("help.your_faction.managing.line.2", "/f invite <player>");
    put("help.your_faction.managing.line.3",
        "Sends an invitation. (Officer+)");
    put("help.your_faction.managing.line.4", "/f kick <player>");
    put("help.your_faction.managing.line.5",
        "Removes a member. Officers kick Members; Leaders all.");
    put("help.your_faction.managing.line.6", "/f promote <player>");
    put("help.your_faction.managing.line.7",
        "Promotes a Member to Officer. (Leader only)");
    put("help.your_faction.managing.line.8", "/f demote <player>");
    put("help.your_faction.managing.line.9",
        "Demotes an Officer to Member. (Leader only)");
    put("help.your_faction.managing.line.10", "/f transfer <player>");
    put("help.your_faction.managing.line.11",
        "Transfers leadership. You become Officer. Cannot undo!");

    // =================================================================
    // POWER & LAND
    // =================================================================

    // --- Understanding Power ---
    put("help.power_land.understanding_power.title", "Understanding Power");
    put("help.power_land.understanding_power.line.1",
        "Power lets your faction hold territory. Every");
    put("help.power_land.understanding_power.line.2",
        "player has personal power that adds to the total.");
    put("help.power_land.understanding_power.line.3", "/f power");
    put("help.power_land.understanding_power.line.4",
        "Check your power and your faction's total.");
    put("help.power_land.understanding_power.line.5",
        "Power regenerates online, decreases on death.");
    put("help.power_land.understanding_power.line.6",
        "If claims exceed power, you're vulnerable!");

    // --- Claiming Territory ---
    put("help.power_land.claiming.title", "Claiming Territory");
    put("help.power_land.claiming.line.1",
        "Claiming a chunk protects it. Only members can");
    put("help.power_land.claiming.line.2",
        "build, break, or access containers inside.");
    put("help.power_land.claiming.line.3", "/f claim");
    put("help.power_land.claiming.line.4",
        "Claims the chunk you're standing in. (Officer+)");
    put("help.power_land.claiming.line.5", "/f unclaim");
    put("help.power_land.claiming.line.6",
        "Releases a claim back to wilderness. (Officer+)");
    put("help.power_land.claiming.line.7",
        "Each claim costs one power. Don't over-expand!");

    // --- The Territory Map ---
    put("help.power_land.territory_map.title", "The Territory Map");
    put("help.power_land.territory_map.line.1",
        "A bird's-eye view of claimed chunks near you.");
    put("help.power_land.territory_map.line.2", "/f map");
    put("help.power_land.territory_map.line.3",
        "Opens the territory map. Click chunks to claim.");
    put("help.power_land.territory_map.line.4",
        "Your faction shows in your color. Allies in blue,");
    put("help.power_land.territory_map.line.5",
        "enemies in red, neutrals in gray, wilderness dark.");

    // --- Losing Territory ---
    put("help.power_land.losing_territory.title", "Losing Territory");
    put("help.power_land.losing_territory.line.1",
        "If total power drops below claim count, you're");
    put("help.power_land.losing_territory.line.2",
        "raidable. Enemies can overclaim your chunks.");
    put("help.power_land.losing_territory.line.3", "/f overclaim");
    put("help.power_land.losing_territory.line.4",
        "Takes a chunk from a weakened faction. (Officer+)");
    put("help.power_land.losing_territory.line.5",
        "Stay safe: stay active, avoid deaths, don't");
    put("help.power_land.losing_territory.line.6",
        "over-expand beyond what your power supports.");

    // =================================================================
    // DIPLOMACY
    // =================================================================

    // --- Faction Relations ---
    put("help.diplomacy.relations.title", "Faction Relations");
    put("help.diplomacy.relations.line.1",
        "Every faction pair has a diplomatic relation:");
    put("help.diplomacy.relations.line.2",
        "Ally \u2014 No friendly fire, protected from each");
    put("help.diplomacy.relations.line.3",
        "other's claims. Requires mutual agreement.");
    put("help.diplomacy.relations.line.4",
        "Enemy \u2014 PvP enabled in each other's territory.");
    put("help.diplomacy.relations.line.5",
        "Overclaiming possible if target is weakened.");
    put("help.diplomacy.relations.line.6",
        "Neutral \u2014 Default state. Standard rules apply.");
    put("help.diplomacy.relations.line.7", "/f relations");
    put("help.diplomacy.relations.line.8",
        "View all alliances, enemies, and pending requests.");

    // --- Forming Alliances ---
    put("help.diplomacy.alliances.title", "Forming Alliances");
    put("help.diplomacy.alliances.line.1",
        "Alliances protect both factions from friendly");
    put("help.diplomacy.alliances.line.2",
        "fire and territorial disputes.");
    put("help.diplomacy.alliances.line.3", "/f ally <faction>");
    put("help.diplomacy.alliances.line.4",
        "Sends an alliance request. Both sides must agree.");
    put("help.diplomacy.alliances.line.5",
        "Benefits: no friendly fire, shared map visibility.");
    put("help.diplomacy.alliances.line.6",
        "There may be a limit on alliance count.");

    // --- Enemy Factions ---
    put("help.diplomacy.enemies.title", "Enemy Factions");
    put("help.diplomacy.enemies.line.1",
        "Declaring an enemy enables PvP and territorial");
    put("help.diplomacy.enemies.line.2",
        "aggression against them. One-way action.");
    put("help.diplomacy.enemies.line.3", "/f enemy <faction>");
    put("help.diplomacy.enemies.line.4",
        "Declares enemy immediately. No agreement needed.");
    put("help.diplomacy.enemies.line.5",
        "PvP enabled in each other's territory. Overclaim");
    put("help.diplomacy.enemies.line.6",
        "possible if they become weakened.");
    put("help.diplomacy.enemies.line.7", "/f neutral <faction>");
    put("help.diplomacy.enemies.line.8",
        "Resets relation to neutral, ending enemy status.");

    // =================================================================
    // COMBAT & SAFETY
    // =================================================================

    // --- Combat Tagging ---
    put("help.combat.tagging.title", "Combat Tagging");
    put("help.combat.tagging.line.1",
        "Attacking or being attacked combat tags you.");
    put("help.combat.tagging.line.2",
        "A timer shows the remaining tag duration.");
    put("help.combat.tagging.line.3",
        "While tagged: no /f home, /f stuck, or teleports.");
    put("help.combat.tagging.line.4",
        "The tag resets with each new combat action.");
    put("help.combat.tagging.line.5",
        "Logging out while tagged is risky. Stay and fight!");

    // --- Territory Protection ---
    put("help.combat.protection.title", "Territory Protection");
    put("help.combat.protection.line.1",
        "Claimed territory has several protections:");
    put("help.combat.protection.line.2", "Block Protection");
    put("help.combat.protection.line.3",
        "Only members can place or break blocks.");
    put("help.combat.protection.line.4", "Container Protection");
    put("help.combat.protection.line.5",
        "Chests, barrels, etc. are secured to members.");
    put("help.combat.protection.line.6", "Entry Alerts");
    put("help.combat.protection.line.7",
        "You're notified when non-members enter claims.");
    put("help.combat.protection.line.8",
        "Territory protects blocks, not players!");

    // --- Special Zones ---
    put("help.combat.zones.title", "Special Zones");
    put("help.combat.zones.line.1",
        "Admins can create zones with special rules:");
    put("help.combat.zones.line.2", "SafeZone");
    put("help.combat.zones.line.3",
        "No PvP, no block breaking. For spawn/trading.");
    put("help.combat.zones.line.4", "WarZone");
    put("help.combat.zones.line.5",
        "PvP always enabled, no protection. Battle areas.");
    put("help.combat.zones.line.6",
        "Zone rules always override faction territory.");

    // --- Death & Recovery ---
    put("help.combat.death.title", "Death & Recovery");
    put("help.combat.death.line.1",
        "Death has real consequences:");
    put("help.combat.death.line.2",
        "You lose personal power, lowering faction total.");
    put("help.combat.death.line.3",
        "If claims exceed power, enemies can overclaim.");
    put("help.combat.death.line.4",
        "Power regenerates while online. Multiple deaths");
    put("help.combat.death.line.5",
        "can leave your faction dangerously vulnerable.");
    put("help.combat.death.line.6",
        "Pick your battles carefully!");

    // =================================================================
    // ECONOMY
    // =================================================================

    // --- Faction Treasury ---
    put("help.economy.treasury.title", "Faction Treasury");
    put("help.economy.treasury.line.1",
        "Every faction has a shared treasury. Managed");
    put("help.economy.treasury.line.2",
        "by Officers and the Leader.");
    put("help.economy.treasury.line.3", "/f balance");
    put("help.economy.treasury.line.4",
        "Check your faction's treasury balance. (Alias: bal)");
    put("help.economy.treasury.line.5",
        "Contribute regularly to keep your faction funded!");

    // --- Managing Funds ---
    put("help.economy.funds.title", "Managing Funds");
    put("help.economy.funds.line.1",
        "Members deposit; Officers can withdraw/transfer.");
    put("help.economy.funds.line.2", "/f deposit <amount>");
    put("help.economy.funds.line.3",
        "Deposit from your balance into the treasury.");
    put("help.economy.funds.line.4", "/f withdraw <amount>");
    put("help.economy.funds.line.5",
        "Withdraw from treasury. (Officer+)");
    put("help.economy.funds.line.6", "/f money transfer <faction> <amount>");
    put("help.economy.funds.line.7",
        "Transfer funds to another faction's treasury.");
    put("help.economy.funds.line.8",
        "All transactions are logged for review.");

    // --- Economy Commands ---
    put("help.economy.commands.title", "Economy Commands");
    put("help.economy.commands.line.1",
        "Quick reference for economy commands:");
    put("help.economy.commands.line.2", "/f balance");
    put("help.economy.commands.line.3", "View treasury balance.");
    put("help.economy.commands.line.4", "/f deposit <amount>");
    put("help.economy.commands.line.5", "Deposit funds.");
    put("help.economy.commands.line.6", "/f withdraw <amount>");
    put("help.economy.commands.line.7", "Withdraw funds. (Officer+)");
    put("help.economy.commands.line.8", "/f money transfer <faction> <amount>");
    put("help.economy.commands.line.9", "Transfer to another faction.");
    put("help.economy.commands.line.10", "/f money log [page]");
    put("help.economy.commands.line.11", "View transaction history.");

    // =================================================================
    // QUICK REFERENCE
    // =================================================================

    // --- All Commands ---
    put("help.quick_ref.all_commands.title", "All Commands");

    // Core
    put("help.quick_ref.all_commands.line.1", "Core");
    put("help.quick_ref.all_commands.line.2", "/f  \u2014  Open faction menu (alias: gui, menu)");
    put("help.quick_ref.all_commands.line.3", "/f help  \u2014  Open this help center");
    put("help.quick_ref.all_commands.line.4", "/f create <name>  \u2014  Create a faction");
    put("help.quick_ref.all_commands.line.5", "/f disband  \u2014  Delete your faction (Leader)");
    put("help.quick_ref.all_commands.line.6", "/f leave  \u2014  Leave your faction");

    // Membership
    put("help.quick_ref.all_commands.line.7", "Membership");
    put("help.quick_ref.all_commands.line.8", "/f invite <player>  \u2014  Invite player (Officer+)");
    put("help.quick_ref.all_commands.line.9", "/f accept [faction]  \u2014  Accept invite (alias: join)");
    put("help.quick_ref.all_commands.line.10", "/f request <faction>  \u2014  Request to join");
    put("help.quick_ref.all_commands.line.11", "/f kick <player>  \u2014  Remove member (Officer+)");
    put("help.quick_ref.all_commands.line.12", "/f promote <player>  \u2014  Promote to Officer (Leader)");
    put("help.quick_ref.all_commands.line.13", "/f demote <player>  \u2014  Demote to Member (Leader)");
    put("help.quick_ref.all_commands.line.14", "/f transfer <player>  \u2014  Transfer leadership");

    // Territory
    put("help.quick_ref.all_commands.line.15", "Territory");
    put("help.quick_ref.all_commands.line.16", "/f claim  \u2014  Claim current chunk (Officer+)");
    put("help.quick_ref.all_commands.line.17", "/f unclaim  \u2014  Release current chunk (Officer+)");
    put("help.quick_ref.all_commands.line.18", "/f overclaim  \u2014  Take weakened faction's chunk");
    put("help.quick_ref.all_commands.line.19", "/f map  \u2014  Open territory map");

    // Teleport
    put("help.quick_ref.all_commands.line.20", "Teleport");
    put("help.quick_ref.all_commands.line.21", "/f home  \u2014  Teleport to faction home");
    put("help.quick_ref.all_commands.line.22", "/f sethome  \u2014  Set faction home (Officer+)");
    put("help.quick_ref.all_commands.line.23", "/f delhome  \u2014  Delete faction home (Officer+)");
    put("help.quick_ref.all_commands.line.24", "/f stuck  \u2014  Escape enemy territory");

    // Information
    put("help.quick_ref.all_commands.line.25", "Information");
    put("help.quick_ref.all_commands.line.26", "/f info [faction]  \u2014  View faction details");
    put("help.quick_ref.all_commands.line.27", "/f list  \u2014  Browse all factions");
    put("help.quick_ref.all_commands.line.28", "/f members  \u2014  View roster");
    put("help.quick_ref.all_commands.line.29", "/f who [player]  \u2014  View player info");
    put("help.quick_ref.all_commands.line.30", "/f power [player]  \u2014  Check power levels");
    put("help.quick_ref.all_commands.line.31", "/f invites  \u2014  Manage invites/requests");
    put("help.quick_ref.all_commands.line.32", "/f relations  \u2014  View diplomatic relations");

    // Diplomacy
    put("help.quick_ref.all_commands.line.33", "Diplomacy");
    put("help.quick_ref.all_commands.line.34", "/f ally <faction>  \u2014  Request alliance (Officer+)");
    put("help.quick_ref.all_commands.line.35", "/f enemy <faction>  \u2014  Declare enemy (Officer+)");
    put("help.quick_ref.all_commands.line.36", "/f neutral <faction>  \u2014  Reset to neutral");

    // Settings
    put("help.quick_ref.all_commands.line.37", "Settings");
    put("help.quick_ref.all_commands.line.38", "/f settings  \u2014  Open settings GUI (Officer+)");
    put("help.quick_ref.all_commands.line.39", "/f rename <name>  \u2014  Rename faction (Leader)");
    put("help.quick_ref.all_commands.line.40", "/f desc [text]  \u2014  Set description (Officer+)");
    put("help.quick_ref.all_commands.line.41", "/f color <code>  \u2014  Set faction color (Officer+)");
    put("help.quick_ref.all_commands.line.42", "/f open  \u2014  Allow anyone to join (Leader)");
    put("help.quick_ref.all_commands.line.43", "/f close  \u2014  Require invitation (Leader)");

    // Economy
    put("help.quick_ref.all_commands.line.44", "Economy");
    put("help.quick_ref.all_commands.line.45", "/f balance  \u2014  View treasury");
    put("help.quick_ref.all_commands.line.46", "/f deposit <amount>  \u2014  Deposit funds");
    put("help.quick_ref.all_commands.line.47", "/f withdraw <amount>  \u2014  Withdraw (Officer+)");
    put("help.quick_ref.all_commands.line.48", "/f money transfer <faction> <amt>  \u2014  Transfer");
    put("help.quick_ref.all_commands.line.49", "/f money log [page]  \u2014  Transaction history");

    // Chat
    put("help.quick_ref.all_commands.line.50", "Chat");
    put("help.quick_ref.all_commands.line.51", "/f c  \u2014  Cycle: Normal > Faction > Ally");
    put("help.quick_ref.all_commands.line.52", "/f c f  \u2014  Set faction chat");
    put("help.quick_ref.all_commands.line.53", "/f c a  \u2014  Set ally chat");
    put("help.quick_ref.all_commands.line.54", "/f c off  \u2014  Set public chat");

    // Admin
    put("help.quick_ref.all_commands.line.55", "Admin (requires hyperfactions.admin)");
    put("help.quick_ref.all_commands.line.56", "/f admin  \u2014  Open admin dashboard");
    put("help.quick_ref.all_commands.line.57", "/f admin reload  \u2014  Reload configuration");
    put("help.quick_ref.all_commands.line.58", "/f admin sync  \u2014  Sync faction data");
    put("help.quick_ref.all_commands.line.59", "/f admin factions  \u2014  Faction management");
    put("help.quick_ref.all_commands.line.60", "/f admin config  \u2014  Configuration editor");
    put("help.quick_ref.all_commands.line.61", "/f admin zones  \u2014  Zone management");
    put("help.quick_ref.all_commands.line.62", "/f admin backup create  \u2014  Create backup");
    put("help.quick_ref.all_commands.line.63", "/f admin backup restore  \u2014  Restore backup");
    put("help.quick_ref.all_commands.line.64", "/f admin safezone <name>  \u2014  Create SafeZone");
    put("help.quick_ref.all_commands.line.65", "/f admin warzone <name>  \u2014  Create WarZone");
    put("help.quick_ref.all_commands.line.66", "/f admin debug toggle <cat>  \u2014  Debug logging");
  }
}
