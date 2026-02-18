package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRelation;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.RefreshablePage;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.gui.faction.data.FactionDashboardData;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.manager.ChatManager;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.InviteManager;
import com.hyperfactions.manager.JoinRequestManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.TeleportManager;
import com.hyperfactions.util.ChunkUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Faction Dashboard page - main view for faction members.
 * Shows faction stats, quick actions, and recent activity.
 */
public class FactionDashboardPage extends InteractiveCustomUIPage<FactionDashboardData> implements RefreshablePage {

    private static final String PAGE_ID = "dashboard";
    private static final int ACTIVITY_ENTRIES = 15;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final ClaimManager claimManager;
    private final PowerManager powerManager;
    private final TeleportManager teleportManager;
    private final GuiManager guiManager;
    private final HyperFactions plugin;
    private final Faction faction;

    public FactionDashboardPage(PlayerRef playerRef,
                                FactionManager factionManager,
                                ClaimManager claimManager,
                                PowerManager powerManager,
                                TeleportManager teleportManager,
                                GuiManager guiManager,
                                HyperFactions plugin,
                                Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionDashboardData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.claimManager = claimManager;
        this.powerManager = powerManager;
        this.teleportManager = teleportManager;
        this.guiManager = guiManager;
        this.plugin = plugin;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Fetch fresh faction data to ensure we have current state
        Faction currentFaction = factionManager.getFaction(faction.id());
        if (currentFaction == null) {
            // Faction was deleted - show error
            cmd.append("HyperFactions/shared/error_page.ui");
            cmd.set("#ErrorMessage.Text", "Your faction no longer exists.");
            return;
        }

        UUID viewerUuid = playerRef.getUuid();
        FactionMember member = currentFaction.getMember(viewerUuid);
        FactionRole viewerRole = member != null ? member.role() : FactionRole.MEMBER;
        boolean isOfficerPlus = viewerRole.getLevel() >= FactionRole.OFFICER.getLevel();
        boolean isLeader = viewerRole == FactionRole.LEADER;

        // Load the main template
        cmd.append("HyperFactions/faction/faction_dashboard.ui");

        // Setup navigation bar
        setupNavBar(cmd, events);

        // Faction header (use fresh data)
        buildFactionHeader(cmd, currentFaction);

        // Stat cards (use fresh data)
        buildStatCards(cmd, currentFaction);

        // Quick actions (conditional)
        buildQuickActions(cmd, events, isOfficerPlus, isLeader);

        // Activity feed (use fresh data)
        buildActivityFeed(cmd, events, currentFaction);

        // Register with active page tracker for real-time updates
        ActivePageTracker activeTracker = guiManager.getActivePageTracker();
        if (activeTracker != null) {
            activeTracker.register(playerRef.getUuid(), PAGE_ID, faction.id(), this);
        }
    }

    @Override
    public void refreshContent() {
        rebuild();
    }

    private void setupNavBar(UICommandBuilder cmd, UIEventBuilder events) {
        // Use NavBarHelper for consistent nav bar setup across all pages
        NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);
    }

    private void buildFactionHeader(UICommandBuilder cmd, Faction currentFaction) {
        // Faction name with color
        cmd.set("#FactionName.Text", currentFaction.name());

        // Tag in gold if present
        if (currentFaction.tag() != null && !currentFaction.tag().isEmpty()) {
            cmd.set("#FactionTag.Text", "[" + currentFaction.tag() + "]");
        }

        // Description
        if (currentFaction.description() != null && !currentFaction.description().isEmpty()) {
            cmd.set("#FactionDescription.Text", "\"" + currentFaction.description() + "\"");
        }
    }

    private void buildStatCards(UICommandBuilder cmd, Faction currentFaction) {
        PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(currentFaction.id());

        // Row 1: Power, Claims, Members

        // Power stat - current/max and percentage
        cmd.set("#PowerValue.Text", String.format("%.0f / %.0f", stats.currentPower(), stats.maxPower()));
        int powerPercent = stats.maxPower() > 0 ? (int) ((stats.currentPower() / stats.maxPower()) * 100) : 0;
        cmd.set("#PowerPercent.Text", powerPercent + "%");

        // Claims stat - used/max and available
        int claimCount = currentFaction.claims().size();
        int maxClaims = stats.maxClaims();
        int available = Math.max(0, maxClaims - claimCount);
        cmd.set("#ClaimsValue.Text", claimCount + " / " + maxClaims);
        cmd.set("#ClaimsAvailable.Text", available + " available");

        // Check if faction is raidable (at risk of overclaiming)
        boolean isRaidable = claimCount > maxClaims;
        if (isRaidable) {
            // Show warning - claims exceed power limit
            cmd.set("#ClaimsValue.Style.TextColor", "#FF5555");
            cmd.set("#ClaimsAvailable.Text", "At Risk!");
            cmd.set("#ClaimsAvailable.Style.TextColor", "#FF5555");
        }

        // Members stat - total and online
        int totalMembers = currentFaction.members().size();
        int onlineCount = countOnlineMembers(currentFaction);
        cmd.set("#MembersValue.Text", String.valueOf(totalMembers));
        cmd.set("#MembersOnline.Text", onlineCount + " online");

        // Row 2: Relations, Status, Invites

        // Relations stat - ally/enemy count
        int allyCount = 0;
        int enemyCount = 0;
        for (FactionRelation relation : currentFaction.relations().values()) {
            if (relation.type() == RelationType.ALLY) {
                allyCount++;
            } else if (relation.type() == RelationType.ENEMY) {
                enemyCount++;
            }
        }
        cmd.set("#AllyCount.Text", String.valueOf(allyCount));
        cmd.set("#EnemyCount.Text", String.valueOf(enemyCount));

        // Status stat - Open/Invite Only
        if (currentFaction.open()) {
            cmd.set("#StatusValue.Text", "Open");
            cmd.set("#StatusValue.Style.TextColor", "#55FF55");
        } else {
            cmd.set("#StatusValue.Text", "Invite");
            cmd.set("#StatusValue.Style.TextColor", "#FFAA00");
        }
        cmd.set("#StatusDesc.Text", "");

        // Invites stat - sent/requests
        InviteManager inviteManager = plugin.getInviteManager();
        JoinRequestManager joinRequestManager = plugin.getJoinRequestManager();
        int sentInvites = inviteManager.getFactionInviteCount(currentFaction.id());
        int requestCount = joinRequestManager.getFactionRequests(currentFaction.id()).size();
        cmd.set("#InvitesSent.Text", String.valueOf(sentInvites));
        cmd.set("#InvitesReceived.Text", String.valueOf(requestCount));

        // Economy row (conditional - only if economy enabled)
        EconomyManager econ = plugin.getEconomyManager();
        if (econ != null) {
            cmd.set("#EconomyRow.Visible", true);

            // Treasury balance
            double treasuryBalance = econ.getFactionBalance(currentFaction.id());
            cmd.set("#TreasuryBalance.Text", econ.formatCurrencyCompact(treasuryBalance));

            // Upkeep info
            if (ConfigManager.get().isUpkeepEnabled()) {
                cmd.set("#UpkeepStat.Visible", true);
                double upkeepAmount = ConfigManager.get().getUpkeepCostPerChunk() * currentFaction.claims().size();
                cmd.set("#UpkeepValue.Text", econ.formatCurrencyCompact(upkeepAmount));
            }

            // Personal wallet balance
            UUID viewerUuid = playerRef.getUuid();
            try {
                double walletBalance = econ.getVaultProvider().getBalance(viewerUuid);
                cmd.set("#WalletBalance.Text", econ.formatCurrencyCompact(walletBalance));
            } catch (Exception e) {
                cmd.set("#WalletBalance.Text", "N/A");
            }
        }
    }

    private int countOnlineMembers(Faction currentFaction) {
        int count = 0;
        Universe universe = Universe.get();
        for (UUID memberUuid : currentFaction.members().keySet()) {
            if (universe.getPlayer(memberUuid) != null) {
                count++;
            }
        }
        return count;
    }

    private void buildQuickActions(UICommandBuilder cmd, UIEventBuilder events,
                                    boolean isOfficerPlus, boolean isLeader) {
        UUID viewerUuid = playerRef.getUuid();

        // HOME button - show for anyone if home exists, or for officers+ to set home
        if ((faction.hasHome() || isOfficerPlus)
                && PermissionManager.get().hasPermission(viewerUuid, Permissions.HOME)) {
            cmd.append("#HomeBtnContainer", "HyperFactions/faction/dashboard_action_btn.ui");
            cmd.set("#HomeBtnContainer #ActionBtn.Text", faction.hasHome() ? "Home" : "Set Home");
            cmd.set("#HomeBtnContainer #ActionBtn.Style",
                    Value.ref("Common.ui", "SecondaryTextButtonStyle"));
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#HomeBtnContainer #ActionBtn",
                    EventData.of("Button", "Home"),
                    false
            );
        } else {
            cmd.set("#HomeBtnContainer.Visible", false);
        }

        // CLAIM button - only for officers+ with CLAIM permission
        if (isOfficerPlus && PermissionManager.get().hasPermission(viewerUuid, Permissions.CLAIM)) {
            cmd.append("#ClaimBtnContainer", "HyperFactions/faction/dashboard_action_btn.ui");
            cmd.set("#ClaimBtnContainer #ActionBtn.Text", "Claim");
            cmd.set("#ClaimBtnContainer #ActionBtn.Style",
                    Value.ref("Common.ui", "SecondaryTextButtonStyle"));
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ClaimBtnContainer #ActionBtn",
                    EventData.of("Button", "Claim"),
                    false
            );
        } else {
            cmd.set("#ClaimBtnContainer.Visible", false);
        }

        // CHAT MODE button - shows current mode, click to cycle
        if (PermissionManager.get().hasPermission(viewerUuid, Permissions.CHAT_FACTION)
                || PermissionManager.get().hasPermission(viewerUuid, Permissions.CHAT_ALLY)) {
            ChatManager chatManager = plugin.getChatManager();
            ChatManager.ChatChannel currentChannel = chatManager.getChannel(viewerUuid);
            String display = "Chat: " + ChatManager.getChannelDisplay(currentChannel);

            cmd.append("#ChatModeBtnContainer", "HyperFactions/faction/dashboard_action_btn.ui");
            cmd.set("#ChatModeBtnContainer #ActionBtn.Text", display);
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ChatModeBtnContainer #ActionBtn",
                    EventData.of("Button", "ChatMode"),
                    false
            );
        } else {
            cmd.set("#ChatModeBtnContainer.Visible", false);
        }

        // LEAVE button - flat red background for danger action
        if (PermissionManager.get().hasPermission(viewerUuid, Permissions.LEAVE)) {
            cmd.append("#LeaveBtnContainer", "HyperFactions/faction/dashboard_action_btn.ui");
            cmd.set("#LeaveBtnContainer #ActionBtn.Text", "Leave");
            cmd.set("#LeaveBtnContainer #ActionBtn.Style",
                    Value.ref("Common.ui", "CancelTextButtonStyle"));
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#LeaveBtnContainer #ActionBtn",
                    EventData.of("Button", "Leave"),
                    false
            );
        } else {
            cmd.set("#LeaveBtnContainer.Visible", false);
        }
    }

    private void buildActivityFeed(UICommandBuilder cmd, UIEventBuilder events, Faction currentFaction) {
        // TODO: Enable View All button when Activity Logs page (logs_viewer.ui) is implemented
        // ViewLogsBtn is styled as disabled in the UI template for now

        // Show recent activity entries (appended dynamically, scrollable)
        List<FactionLog> logs = currentFaction.logs();
        int displayCount = Math.min(ACTIVITY_ENTRIES, logs.size());

        if (displayCount == 0) {
            cmd.appendInline("#ActivityFeed",
                    "Label { Text: \"No recent activity.\"; Style: (FontSize: 11, TextColor: #555555); " +
                    "Anchor: (Height: 26); }");
            return;
        }

        for (int i = 0; i < displayCount; i++) {
            FactionLog log = logs.get(i);
            String idx = "#ActivityFeed[" + i + "]";

            cmd.append("#ActivityFeed", "HyperFactions/faction/activity_entry.ui");
            cmd.set(idx + " #ActivityType.Text", log.type().getDisplayName().toUpperCase());
            cmd.set(idx + " #ActivityMessage.Text", log.message());
            cmd.set(idx + " #ActivityTime.Text", formatTimeAgo(log.timestamp()));
        }
    }

    private String formatTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + "m ago";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + "h ago";
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + "d ago";
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionDashboardData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        UUID uuid = playerRef.getUuid();
        Faction currentFaction = factionManager.getPlayerFaction(uuid);

        // Handle navigation via NavBarHelper (consistent with other pages)
        if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, currentFaction, guiManager)) {
            return;
        }

        // Verify still in faction
        if (currentFaction == null) {
            player.sendMessage(MessageUtil.errorText("You are no longer in a faction."));
            guiManager.openFactionMain(player, ref, store, playerRef);
            return;
        }

        FactionMember member = currentFaction.getMember(uuid);
        FactionRole viewerRole = member != null ? member.role() : FactionRole.MEMBER;
        boolean isOfficerPlus = viewerRole.getLevel() >= FactionRole.OFFICER.getLevel();
        boolean isLeader = viewerRole == FactionRole.LEADER;

        switch (data.button) {
            case "Home" -> {
                if (!PermissionManager.get().hasPermission(uuid, Permissions.HOME)) {
                    sendUpdate();
                    return;
                }
                if (!currentFaction.hasHome()) {
                    // No home set - for officers+, prompt to set home
                    if (isOfficerPlus) {
                        handleSetHomeAction(player, ref, store, uuid, currentFaction);
                    } else {
                        player.sendMessage(MessageUtil.errorText("Your faction has no home set. Ask an officer to set one."));
                        sendUpdate();
                    }
                } else {
                    handleHomeAction(player, ref, store, uuid, currentFaction);
                }
            }

            case "Claim" -> {
                if (!isOfficerPlus || !PermissionManager.get().hasPermission(uuid, Permissions.CLAIM)) {
                    player.sendMessage(MessageUtil.errorText("Only officers can claim territory."));
                    sendUpdate();
                    return;
                }
                handleClaimAction(player, ref, store, playerRef, uuid, currentFaction);
            }

            case "ChatMode" -> {
                ChatManager chatManager = plugin.getChatManager();
                ChatManager.ToggleResult chatResult = chatManager.cycleChannelChecked(uuid);
                if (chatResult.isSuccess() && chatResult.channel() != null) {
                    String display = ChatManager.getChannelDisplay(chatResult.channel());
                    String color = ChatManager.getChannelColor(chatResult.channel());
                    player.sendMessage(Message.raw("Chat mode: ").color("#AAAAAA")
                            .insert(Message.raw(display).color(color)));
                }
                rebuild();
            }

            case "Leave" -> {
                if (!PermissionManager.get().hasPermission(uuid, Permissions.LEAVE)) {
                    sendUpdate();
                    return;
                }
                if (isLeader) {
                    // Leaders get a special confirmation page with succession info
                    guiManager.openLeaderLeaveConfirm(player, ref, store, playerRef, currentFaction);
                } else {
                    guiManager.openLeaveConfirm(player, ref, store, playerRef, currentFaction);
                }
            }

            // TODO: Enable when Activity Logs page (logs_viewer.ui) is implemented
            // case "ViewLogs" -> {
            //     guiManager.openLogsViewer(player, ref, store, playerRef, currentFaction);
            // }

            default -> sendUpdate();
        }
    }

    private void handleHomeAction(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                  UUID uuid, Faction faction) {
        if (!faction.hasHome()) {
            player.sendMessage(MessageUtil.errorText("Your faction has no home set."));
            sendUpdate();
            return;
        }

        // Get player's current location for start location
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            player.sendMessage(MessageUtil.errorText("Could not determine your location."));
            sendUpdate();
            return;
        }

        Vector3d pos = transform.getPosition();
        World world = player.getWorld();
        if (world == null) {
            player.sendMessage(MessageUtil.errorText("Could not determine your world."));
            sendUpdate();
            return;
        }

        TeleportManager.StartLocation startLoc = new TeleportManager.StartLocation(
                world.getName(), pos.getX(), pos.getY(), pos.getZ()
        );

        // Initiate teleport with warmup/combat checking
        // For instant teleport: executeTeleport runs immediately
        // For warmup teleport: TerritoryTickingSystem executes later
        TeleportManager.TeleportResult result = teleportManager.teleportToHome(
                uuid,
                startLoc,
                f -> executeTeleport(store, ref, world, f),
                player::sendMessage,
                () -> plugin.getCombatTagManager().isTagged(uuid)
        );

        // Handle immediate result messages
        handleTeleportResult(player, result);
    }

    private TeleportManager.TeleportResult executeTeleport(Store<EntityStore> store, Ref<EntityStore> ref,
                                                           World currentWorld, Faction faction) {
        Faction.FactionHome home = faction.home();
        if (home == null) {
            return TeleportManager.TeleportResult.NO_HOME;
        }

        // Get target world (supports cross-world teleportation)
        World targetWorld;
        if (currentWorld.getName().equals(home.world())) {
            targetWorld = currentWorld;
        } else {
            targetWorld = Universe.get().getWorld(home.world());
            if (targetWorld == null) {
                return TeleportManager.TeleportResult.WORLD_NOT_FOUND;
            }
        }

        // Execute teleport on the target world's thread using createForPlayer for proper player teleportation
        targetWorld.execute(() -> {
            Vector3d position = new Vector3d(home.x(), home.y(), home.z());
            Vector3f rotation = new Vector3f(home.pitch(), home.yaw(), 0);
            Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
            store.addComponent(ref, Teleport.getComponentType(), teleport);
        });

        return TeleportManager.TeleportResult.SUCCESS_INSTANT;
    }

    private void handleTeleportResult(Player player, TeleportManager.TeleportResult result) {
        switch (result) {
            case NOT_IN_FACTION -> player.sendMessage(MessageUtil.errorText("You are not in a faction."));
            case NO_HOME -> player.sendMessage(MessageUtil.errorText("Your faction has no home set."));
            case COMBAT_TAGGED -> player.sendMessage(MessageUtil.errorText("You cannot teleport while in combat!"));
            case SUCCESS_INSTANT -> player.sendMessage(MessageUtil.successText("Teleported to faction home!"));
            case ON_COOLDOWN, SUCCESS_WARMUP -> {} // Message sent by TeleportManager
            default -> {}
        }
    }

    private void handleSetHomeAction(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                      UUID uuid, Faction faction) {
        // Get player's current location
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            player.sendMessage(MessageUtil.errorText("Could not determine your location."));
            sendUpdate();
            return;
        }

        World world = player.getWorld();
        if (world == null) {
            player.sendMessage(MessageUtil.errorText("Could not determine your world."));
            sendUpdate();
            return;
        }

        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        // Check if in faction territory
        UUID owner = claimManager.getClaimOwner(world.getName(), chunkX, chunkZ);
        if (owner == null || !owner.equals(faction.id())) {
            player.sendMessage(MessageUtil.errorText("You can only set home in your faction's territory."));
            sendUpdate();
            return;
        }

        // Get rotation from transform
        Vector3f rot = transform.getRotation();
        float yaw = rot != null ? rot.getY() : 0;
        float pitch = rot != null ? rot.getX() : 0;

        // Set the home
        Faction.FactionHome home = new Faction.FactionHome(
                world.getName(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                yaw,
                pitch,
                System.currentTimeMillis(),
                uuid
        );

        Faction updated = faction.withHome(home);
        factionManager.updateFaction(updated);

        player.sendMessage(MessageUtil.successText("Faction home set!"));

        // Refresh dashboard
        Faction fresh = factionManager.getFaction(faction.id());
        if (fresh != null) {
            guiManager.openFactionDashboard(player, ref, store, playerRef, fresh);
        }
    }

    private void handleClaimAction(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                   PlayerRef playerRef, UUID uuid, Faction faction) {
        // Get player's current chunk
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            player.sendMessage(MessageUtil.errorText("Could not determine your location."));
            sendUpdate();
            return;
        }

        World world = player.getWorld();
        if (world == null) {
            player.sendMessage(MessageUtil.errorText("Could not determine your world."));
            sendUpdate();
            return;
        }

        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        // Attempt to claim
        ClaimManager.ClaimResult result = claimManager.claim(uuid, world.getName(), chunkX, chunkZ);

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(
                        Message.raw("Claimed chunk at (").color("#55FF55")
                                .insert(Message.raw(chunkX + ", " + chunkZ).color("#AAAAAA"))
                                .insert(Message.raw(")").color("#55FF55"))
                );
                // Refresh dashboard with updated faction data
                Faction fresh = factionManager.getFaction(faction.id());
                if (fresh != null) {
                    guiManager.openFactionDashboard(player, ref, store, playerRef, fresh);
                }
            }
            case NOT_IN_FACTION -> player.sendMessage(MessageUtil.errorText("You are not in a faction."));
            case NOT_OFFICER -> player.sendMessage(MessageUtil.errorText("Only officers can claim land."));
            case ALREADY_CLAIMED_SELF -> player.sendMessage(MessageUtil.text("This chunk is already claimed by your faction.", MessageUtil.COLOR_GOLD));
            case ALREADY_CLAIMED_OTHER, ALREADY_CLAIMED_ALLY, ALREADY_CLAIMED_ENEMY -> player.sendMessage(MessageUtil.errorText("This chunk is claimed by another faction."));
            case MAX_CLAIMS_REACHED -> player.sendMessage(MessageUtil.errorText("Your faction has reached its claim limit."));
            case WORLD_NOT_ALLOWED -> player.sendMessage(MessageUtil.errorText("Claiming is not allowed in this world."));
            case NOT_ADJACENT -> player.sendMessage(MessageUtil.errorText("You can only claim chunks adjacent to existing claims."));
            case INSUFFICIENT_POWER -> player.sendMessage(MessageUtil.errorText("Your faction doesn't have enough power to claim more land."));
            case ORBISGUARD_PROTECTED -> player.sendMessage(MessageUtil.errorText("This area is protected by OrbisGuard."));
            default -> player.sendMessage(MessageUtil.errorText("Could not claim this chunk."));
        }
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        super.onDismiss(ref, store);
        ActivePageTracker activeTracker = guiManager.getActivePageTracker();
        if (activeTracker != null) {
            activeTracker.unregister(playerRef.getUuid());
        }
    }
}
