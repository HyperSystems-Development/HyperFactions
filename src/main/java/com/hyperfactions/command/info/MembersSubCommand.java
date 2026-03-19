package com.hyperfactions.command.info;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f members
 * Views faction members.
 */
public class MembersSubCommand extends FactionSubCommand {

  /** Creates a new MembersSubCommand. */
  public MembersSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("members", "View faction members", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.MEMBERS)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Info.MEMBERS_NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: open FactionMembersPage
    if (fctx.shouldOpenGui()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
        return;
      }
    }

    // Text mode: output member list to chat
    List<FactionMember> members = faction.getMembersSorted();
    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.MEMBERS_HEADER, faction.name(), members.size()), COLOR_CYAN).bold(true));

    for (FactionMember member : members) {
      String roleColor = switch (member.role()) {
        case LEADER -> COLOR_YELLOW;
        case OFFICER -> COLOR_GREEN;
        default -> COLOR_GRAY;
      };
      boolean isOnline = plugin.getTrackedPlayer(member.uuid()) != null;
      String status = isOnline ? " " + HFMessages.get(player, CommandKeys.Info.MEMBER_ONLINE) : "";
      ctx.sendMessage(msg(ConfigManager.get().getRoleDisplayName(member.role()) + " ", roleColor)
        .insert(msg(member.username(), COLOR_WHITE))
        .insert(msg(status, isOnline ? COLOR_GREEN : COLOR_GRAY)));
    }
  }
}
