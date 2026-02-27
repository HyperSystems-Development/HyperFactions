package com.hyperfactions.gui.newplayer.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.JoinRequest;
import com.hyperfactions.data.PendingInvite;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.RefreshablePage;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.gui.newplayer.data.NewPlayerPageData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.InviteManager;
import com.hyperfactions.manager.JoinRequestManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.UuidUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Invites & Requests Page - shows pending faction invitations and outgoing join requests.
 * Allows players to accept/decline invites and cancel their own requests.
 */
public class InvitesPage extends InteractiveCustomUIPage<NewPlayerPageData> implements RefreshablePage {

  private static final String PAGE_ID = "invites";

  private static final int MAX_INVITES_DISPLAYED = 5;

  private static final int MAX_REQUESTS_DISPLAYED = 5;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final PowerManager powerManager;

  private final InviteManager inviteManager;

  private final JoinRequestManager joinRequestManager;

  private final GuiManager guiManager;

  // Saved from build() for use in refreshContent() redirect
  private Ref<EntityStore> savedRef;

  private Store<EntityStore> savedStore;

  /** Creates a new InvitesPage. */
  public InvitesPage(PlayerRef playerRef,
           FactionManager factionManager,
           PowerManager powerManager,
           InviteManager inviteManager,
           JoinRequestManager joinRequestManager,
           GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.powerManager = powerManager;
    this.inviteManager = inviteManager;
    this.joinRequestManager = joinRequestManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Save ref/store for refreshContent() redirect
    this.savedRef = ref;
    this.savedStore = store;

    // Load the main template
    cmd.append(UIPaths.NEWPLAYER_INVITES);

    // Setup navigation bar for new players
    NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

    // Register with active page tracker for real-time updates
    ActivePageTracker activeTracker = guiManager.getActivePageTracker();
    if (activeTracker != null) {
      activeTracker.register(playerRef.getUuid(), PAGE_ID, null, this);
    }

    // Get pending invites and outgoing requests
    List<PendingInvite> invites = inviteManager.getPlayerInvites(playerRef.getUuid());
    List<JoinRequest> requests = joinRequestManager.getPlayerRequests(playerRef.getUuid());

    // Set header with counts
    int totalCount = invites.size() + requests.size();
    cmd.set("#InviteCount.Text", totalCount + " pending");

    // === RECEIVED INVITES SECTION ===
    cmd.set("#InvitesHeader.Text", "RECEIVED INVITES (" + invites.size() + ")");
    if (invites.isEmpty()) {
      cmd.append("#InviteListContainer", UIPaths.RELATION_EMPTY);
      cmd.set("#InviteListContainer[0] #EmptyText.Text", "No invites. Browse factions to find one!");
    } else {
      buildInviteCards(cmd, events, invites);
    }

    // === YOUR REQUESTS SECTION ===
    cmd.set("#RequestsHeader.Text", "YOUR REQUESTS (" + requests.size() + ")");
    if (requests.isEmpty()) {
      cmd.append("#RequestListContainer", UIPaths.RELATION_EMPTY);
      cmd.set("#RequestListContainer[0] #EmptyText.Text", "No pending requests.");
    } else {
      buildRequestCards(cmd, events, requests);
    }
  }

  private void buildInviteCards(UICommandBuilder cmd, UIEventBuilder events,
                 List<PendingInvite> invites) {
    int displayCount = Math.min(MAX_INVITES_DISPLAYED, invites.size());

    for (int i = 0; i < displayCount; i++) {
      PendingInvite invite = invites.get(i);
      Faction faction = factionManager.getFaction(invite.factionId());

      if (faction == null) {
        continue;
      }

      cmd.append("#InviteListContainer", UIPaths.NEWPLAYER_INVITE_CARD);

      String prefix = "#InviteListContainer[" + i + "] ";

      // Faction info
      cmd.set(prefix + "#FactionName.Text", faction.name());

      // Invited by
      String inviterName = getPlayerName(invite.invitedBy());
      cmd.set(prefix + "#InvitedBy.Text", "Invited by: " + inviterName);

      // Stats
      PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());
      cmd.set(prefix + "#MemberCount.Text", faction.members().size() + " members");
      cmd.set(prefix + "#PowerCount.Text", String.format("%.0f power", stats.currentPower()));
      cmd.set(prefix + "#ClaimCount.Text", faction.claims().size() + " claims");

      // Time ago
      cmd.set(prefix + "#TimeAgo.Text", formatTimeAgo(invite.createdAt()));

      // Accept button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          prefix + "#AcceptBtn",
          EventData.of("Button", "Accept")
              .append("FactionId", invite.factionId().toString())
              .append("FactionName", faction.name()),
          false
      );

      // Decline button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          prefix + "#DeclineBtn",
          EventData.of("Button", "Decline")
              .append("FactionId", invite.factionId().toString()),
          false
      );
    }
  }

  private void buildRequestCards(UICommandBuilder cmd, UIEventBuilder events,
                 List<JoinRequest> requests) {
    int displayCount = Math.min(MAX_REQUESTS_DISPLAYED, requests.size());

    for (int i = 0; i < displayCount; i++) {
      JoinRequest request = requests.get(i);
      Faction faction = factionManager.getFaction(request.factionId());

      if (faction == null) {
        continue;
      }

      cmd.append("#RequestListContainer", UIPaths.NEWPLAYER_REQUEST_CARD);

      String prefix = "#RequestListContainer[" + i + "] ";

      // Faction info
      cmd.set(prefix + "#FactionName.Text", faction.name());

      // Status
      cmd.set(prefix + "#StatusText.Text", "Awaiting review");

      // Stats
      PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());
      cmd.set(prefix + "#MemberCount.Text", faction.members().size() + " members");
      cmd.set(prefix + "#PowerCount.Text", String.format("%.0f power", stats.currentPower()));

      // Time remaining
      int hoursRemaining = request.getRemainingHours();
      cmd.set(prefix + "#TimeRemaining.Text", "Expires in " + hoursRemaining + "h");

      // Cancel button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          prefix + "#CancelBtn",
          EventData.of("Button", "CancelRequest")
              .append("FactionId", request.factionId().toString()),
          false
      );
    }
  }

  private String getPlayerName(UUID uuid) {
    for (Faction faction : factionManager.getAllFactions()) {
      var member = faction.getMember(uuid);
      if (member != null) {
        return member.username();
      }
    }
    return uuid.toString().substring(0, 8);
  }

  private String formatTimeAgo(long timestamp) {
    long now = System.currentTimeMillis();
    long diff = now - timestamp;

    if (diff < TimeUnit.MINUTES.toMillis(1)) {
      return "just now";
    } else if (diff < TimeUnit.HOURS.toMillis(1)) {
      long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
      return minutes + " min ago";
    } else if (diff < TimeUnit.DAYS.toMillis(1)) {
      long hours = TimeUnit.MILLISECONDS.toHours(diff);
      return hours + "h ago";
    } else {
      long days = TimeUnit.MILLISECONDS.toDays(diff);
      return days + "d ago";
    }
  }

  /** Refresh Content. */
  @Override
  public void refreshContent() {
    // If player joined a faction (e.g., request accepted by someone else), redirect to dashboard
    if (factionManager.isInFaction(playerRef.getUuid())) {
      Faction faction = factionManager.getPlayerFaction(playerRef.getUuid());
      if (faction != null && savedRef != null && savedStore != null) {
        Player player = savedStore.getComponent(savedRef, Player.getComponentType());
        if (player != null) {
          ActivePageTracker activeTracker = guiManager.getActivePageTracker();
          if (activeTracker != null) {
            activeTracker.unregister(playerRef.getUuid());
          }
          guiManager.openFactionDashboard(player, savedRef, savedStore, playerRef, faction);
          return;
        }
      }
    }
    rebuild();
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                NewPlayerPageData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    // Handle navigation
    if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }

    switch (data.button) {
      case "Browse" -> guiManager.openNewPlayerBrowse(player, ref, store, playerRef);

      case "Accept" -> handleAccept(player, ref, store, playerRef, data);

      case "Decline" -> handleDecline(player, ref, store, playerRef, data);

      case "CancelRequest" -> handleCancelRequest(player, ref, store, playerRef, data);

      default -> sendUpdate();
    }
  }

  private void handleAccept(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
               PlayerRef playerRef, NewPlayerPageData data) {
    if (data.factionId == null) {
      sendUpdate();
      return;
    }

    UUID factionId = UuidUtil.parseOrNull(data.factionId);
    if (factionId == null) {
      player.sendMessage(MessageUtil.errorText("Invalid faction."));
      sendUpdate();
      return;
    }

    UUID playerUuid = playerRef.getUuid();

    if (!inviteManager.hasInvite(factionId, playerUuid)) {
      player.sendMessage(MessageUtil.errorText("This invite has expired or was revoked."));
      sendUpdate();
      return;
    }

    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      player.sendMessage(MessageUtil.errorText("Faction no longer exists."));
      inviteManager.removeInvite(factionId, playerUuid);
      sendUpdate();
      return;
    }

    FactionManager.FactionResult result = factionManager.addMember(
        factionId,
        playerUuid,
        playerRef.getUsername()
    );

    switch (result) {
      case SUCCESS -> {
        player.sendMessage(
            Message.raw("You joined ").color("#55FF55")
                .insert(Message.raw(faction.name()).color("#00FFFF"))
                .insert(Message.raw("!").color("#55FF55"))
        );
        // Clear all invites and requests
        inviteManager.clearPlayerInvites(playerUuid);
        joinRequestManager.clearPlayerRequests(playerUuid);
        // Open faction dashboard
        Faction freshFaction = factionManager.getPlayerFaction(playerUuid);
        if (freshFaction != null) {
          guiManager.openFactionDashboard(player, ref, store, playerRef, freshFaction);
        }
      }
      case ALREADY_IN_FACTION -> {
        player.sendMessage(MessageUtil.errorText("You are already in a faction."));
        sendUpdate();
      }
      case FACTION_FULL -> {
        player.sendMessage(MessageUtil.errorText("This faction is full."));
        sendUpdate();
      }
      default -> {
        player.sendMessage(MessageUtil.errorText("Could not join faction."));
        sendUpdate();
      }
    }
  }

  private void handleDecline(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
               PlayerRef playerRef, NewPlayerPageData data) {
    if (data.factionId == null) {
      sendUpdate();
      return;
    }

    UUID factionId = UuidUtil.parseOrNull(data.factionId);
    if (factionId == null) {
      sendUpdate();
      return;
    }

    inviteManager.removeInvite(factionId, playerRef.getUuid());

    player.sendMessage(MessageUtil.text("Invite declined.", MessageUtil.COLOR_GRAY));

    // Refresh the page
    guiManager.openInvitesPage(player, ref, store, playerRef);
  }

  private void handleCancelRequest(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                  PlayerRef playerRef, NewPlayerPageData data) {
    if (data.factionId == null) {
      sendUpdate();
      return;
    }

    UUID factionId = UuidUtil.parseOrNull(data.factionId);
    if (factionId == null) {
      sendUpdate();
      return;
    }

    Faction faction = factionManager.getFaction(factionId);
    String factionName = faction != null ? faction.name() : "the faction";

    joinRequestManager.removeRequest(factionId, playerRef.getUuid());

    player.sendMessage(MessageUtil.text("Cancelled request to join " + factionName + ".", MessageUtil.COLOR_GRAY));

    // Refresh the page
    guiManager.openInvitesPage(player, ref, store, playerRef);
  }

  /** Called when dismiss. */
  @Override
  public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
    super.onDismiss(ref, store);
    ActivePageTracker activeTracker = guiManager.getActivePageTracker();
    if (activeTracker != null) {
      activeTracker.unregister(playerRef.getUuid());
    }
  }
}
