package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.integration.SentryIntegration;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.AdminKeys;
import com.hyperfactions.util.HelpKeys;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles /f admin test subcommands: gui, sentry, md.
 */
public class AdminTestHandler {

  private final HyperFactions hyperFactions;

  private static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;

  private static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;

  private static final String COLOR_RED = CommandUtil.COLOR_RED;

  private static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;

  private static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;

  private static Message prefix() {
    return CommandUtil.prefix();
  }

  private static Message msg(String text, String color) {
    return CommandUtil.msg(text, color);
  }

  /** Creates a new AdminTestHandler. */
  public AdminTestHandler(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /**
   * Dispatches /f admin test subcommands.
   */
  public void handleTest(@NotNull CommandContext ctx, @Nullable Store<EntityStore> store,
              @Nullable Ref<EntityStore> ref, @Nullable PlayerRef player,
              @NotNull String[] subArgs, boolean isPlayer) {
    if (subArgs.length == 0) {
      showTestHelp(ctx, player);
      return;
    }

    switch (subArgs[0].toLowerCase()) {
      case "gui" -> handleTestGui(ctx, store, ref, player, isPlayer);
      case "sentry" -> handleSentryTest(ctx);
      case "md", "markdown" -> handleMarkdownTest(ctx, store, ref, player, isPlayer);
      default -> showTestHelp(ctx, player);
    }
  }

  private void handleTestGui(CommandContext ctx, Store<EntityStore> store,
                Ref<EntityStore> ref, PlayerRef player, boolean isPlayer) {
    if (!isPlayer) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.PLAYER_ONLY), COLOR_RED)));
      return;
    }
    Player playerEntity = store.getComponent(ref, Player.getComponentType());
    if (playerEntity != null) {
      hyperFactions.getGuiManager().openButtonTestPage(playerEntity, ref, store, player);
    }
  }

  private void handleSentryTest(CommandContext ctx) {
    if (!SentryIntegration.isInitialized()) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_NOT_INITIALIZED), COLOR_RED)));
      return;
    }

    boolean sent = SentryIntegration.sendTestEvent();
    if (sent) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_TEST_SENT), COLOR_GREEN)));
    } else {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_TEST_FAILED), COLOR_RED)));
    }
  }

  private void handleMarkdownTest(CommandContext ctx, Store<EntityStore> store,
                  Ref<EntityStore> ref, PlayerRef player, boolean isPlayer) {
    if (!isPlayer) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.PLAYER_ONLY), COLOR_RED)));
      return;
    }
    Player playerEntity = store.getComponent(ref, Player.getComponentType());
    if (playerEntity != null) {
      hyperFactions.getGuiManager().openMarkdownTestPage(playerEntity, ref, store, player);
    }
  }

  private void showTestHelp(CommandContext ctx, @Nullable PlayerRef player) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin test gui", HelpKeys.Help.TEST_CMD_GUI));
    commands.add(new CommandHelp("/f admin test sentry", HelpKeys.Help.TEST_CMD_SENTRY));
    commands.add(new CommandHelp("/f admin test md", HelpKeys.Help.TEST_CMD_MD));
    ctx.sendMessage(HelpFormatter.buildHelp(HelpKeys.Help.TEST_TITLE, HelpKeys.Help.TEST_DESCRIPTION, commands, null, player));
  }
}
