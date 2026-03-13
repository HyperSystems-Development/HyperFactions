package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.integration.SentryIntegration;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
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
      showTestHelp(ctx);
      return;
    }

    switch (subArgs[0].toLowerCase()) {
      case "gui" -> handleTestGui(ctx, store, ref, player, isPlayer);
      case "sentry" -> handleSentryTest(ctx);
      case "md", "markdown" -> handleMarkdownTest(ctx, store, ref, player, isPlayer);
      default -> showTestHelp(ctx);
    }
  }

  private void handleTestGui(CommandContext ctx, Store<EntityStore> store,
                Ref<EntityStore> ref, PlayerRef player, boolean isPlayer) {
    if (!isPlayer) {
      ctx.sendMessage(prefix().insert(msg("This command can only be used by a player.", COLOR_RED)));
      return;
    }
    Player playerEntity = store.getComponent(ref, Player.getComponentType());
    if (playerEntity != null) {
      hyperFactions.getGuiManager().openButtonTestPage(playerEntity, ref, store, player);
    }
  }

  private void handleSentryTest(CommandContext ctx) {
    if (!SentryIntegration.isInitialized()) {
      ctx.sendMessage(prefix().insert(msg("Sentry is not initialized. Check config/debug.json", COLOR_RED)));
      return;
    }

    boolean sent = SentryIntegration.sendTestEvent();
    if (sent) {
      ctx.sendMessage(prefix().insert(msg("Test error sent to Sentry. Check your Sentry dashboard.", COLOR_GREEN)));
    } else {
      ctx.sendMessage(prefix().insert(msg("Failed to send test event.", COLOR_RED)));
    }
  }

  private void handleMarkdownTest(CommandContext ctx, Store<EntityStore> store,
                  Ref<EntityStore> ref, PlayerRef player, boolean isPlayer) {
    if (!isPlayer) {
      ctx.sendMessage(prefix().insert(msg("This command can only be used by a player.", COLOR_RED)));
      return;
    }
    Player playerEntity = store.getComponent(ref, Player.getComponentType());
    if (playerEntity != null) {
      hyperFactions.getGuiManager().openMarkdownTestPage(playerEntity, ref, store, player);
    }
  }

  private void showTestHelp(CommandContext ctx) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin test gui", "Open UI element test page"));
    commands.add(new CommandHelp("/f admin test sentry", "Send test error to Sentry"));
    commands.add(new CommandHelp("/f admin test md", "Open markdown rendering test page"));
    ctx.sendMessage(HelpFormatter.buildHelp("Test Commands", "Development testing tools", commands, null));
  }
}
