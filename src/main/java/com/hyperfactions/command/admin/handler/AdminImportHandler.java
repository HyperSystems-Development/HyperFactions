package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.importer.ElbaphFactionsImporter;
import com.hyperfactions.importer.FactionsXImporter;
import com.hyperfactions.importer.HyFactionsImporter;
import com.hyperfactions.importer.ImportResult;
import com.hyperfactions.importer.SimpleClaimsImporter;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handles /f admin import commands (hyfactions, elbaphfactions, simpleclaims).
 */
public class AdminImportHandler {

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

  /** Creates a new AdminImportHandler. */
  public AdminImportHandler(HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /** Handles admin import. */
  public void handleAdminImport(CommandContext ctx, String[] args) {
    if (args.length == 0) {
      showImportHelp(ctx);
      return;
    }

    String subCmd = args[0].toLowerCase();
    String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

    switch (subCmd) {
      case "hyfactions" -> handleImportHyFactions(ctx, subArgs);
      case "elbaphfactions" -> handleImportElbaphFactions(ctx, subArgs);
      case "factionsx" -> handleImportFactionsX(ctx, subArgs);
      case "simpleclaims" -> handleImportSimpleClaims(ctx, subArgs);
      case "help", "?" -> showImportHelp(ctx);
      default -> {
        ctx.sendMessage(prefix().insert(msg("Unknown import source: " + subCmd, COLOR_RED)));
        showImportHelp(ctx);
      }
    }
  }

  private void showImportHelp(CommandContext ctx) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin import hyfactions [path] [flags]", "Import from HyFactions mod"));
    commands.add(new CommandHelp("  Default path: mods/Kaws_Hyfaction", ""));
    commands.add(new CommandHelp("/f admin import elbaphfactions [path] [flags]", "Import from ElbaphFactions mod"));
    commands.add(new CommandHelp("  Default path: mods/ElbaphFactions", ""));
    commands.add(new CommandHelp("/f admin import factionsx [path] [flags]", "Import from FactionsX mod"));
    commands.add(new CommandHelp("  Default path: mods/FactionsX", ""));
    commands.add(new CommandHelp("/f admin import simpleclaims [path] [flags]", "Import from SimpleClaims mod"));
    commands.add(new CommandHelp("  Default path: Server/universe/SimpleClaims", ""));
    commands.add(new CommandHelp("  Flags:", ""));
    commands.add(new CommandHelp("    --dry-run / -n", "Simulate without changes"));
    commands.add(new CommandHelp("    --overwrite", "Replace existing factions"));
    commands.add(new CommandHelp("    --no-zones", "Skip zone import"));
    commands.add(new CommandHelp("    --no-power", "Skip power distribution"));
    ctx.sendMessage(HelpFormatter.buildHelp("Import Commands", "Migrate from other faction plugins", commands, null));
  }

  /** Handles import hy factions. */
  public void handleImportHyFactions(CommandContext ctx, String[] args) {
    // Parse path (optional - default to mods/Kaws_Hyfaction)
    String pathStr = "mods/Kaws_Hyfaction";
    int flagStartIndex = 0;

    if (args.length > 0 && !args[0].startsWith("-")) {
      pathStr = args[0];
      flagStartIndex = 1;
    }

    Path dataPath = Paths.get(pathStr);

    boolean dryRun = false;
    boolean overwrite = false;
    boolean skipZones = false;
    boolean skipPower = false;

    for (int i = flagStartIndex; i < args.length; i++) {
      String flag = args[i].toLowerCase();
      switch (flag) {
        case "--dry-run", "-n" -> dryRun = true;
        case "--overwrite" -> overwrite = true;
        case "--no-zones" -> skipZones = true;
        case "--no-power" -> skipPower = true;
        default -> throw new IllegalStateException("Unexpected value");
      }
    }

    ctx.sendMessage(prefix().insert(msg("Importing from HyFactions...", COLOR_YELLOW)));
    ctx.sendMessage(msg("  Path: " + dataPath, COLOR_GRAY));
    if (dryRun) {
      ctx.sendMessage(msg("  (Dry run - no changes will be made)", COLOR_GRAY));
    }

    HyFactionsImporter importer = new HyFactionsImporter(
      hyperFactions.getFactionManager(),
      hyperFactions.getClaimManager(),
      hyperFactions.getZoneManager(),
      hyperFactions.getPowerManager(),
      hyperFactions.getBackupManager()
    );

    importer.setDryRun(dryRun);
    importer.setOverwrite(overwrite);
    importer.setSkipZones(skipZones);
    importer.setSkipPower(skipPower);

    final boolean finalDryRun = dryRun;
    CompletableFuture.supplyAsync(() -> importer.importFrom(dataPath))
      .thenAccept(result -> reportImportResult(ctx, result, finalDryRun, "HyFactions"));
  }

  /** Handles import elbaph factions. */
  public void handleImportElbaphFactions(CommandContext ctx, String[] args) {
    // Parse path (optional - default to mods/ElbaphFactions)
    String pathStr = "mods/ElbaphFactions";
    int flagStartIndex = 0;

    if (args.length > 0 && !args[0].startsWith("-")) {
      pathStr = args[0];
      flagStartIndex = 1;
    }

    Path dataPath = Paths.get(pathStr);

    boolean dryRun = false;
    boolean overwrite = false;
    boolean skipZones = false;
    boolean skipPower = false;

    for (int i = flagStartIndex; i < args.length; i++) {
      String flag = args[i].toLowerCase();
      switch (flag) {
        case "--dry-run", "-n" -> dryRun = true;
        case "--overwrite" -> overwrite = true;
        case "--no-zones" -> skipZones = true;
        case "--no-power" -> skipPower = true;
        default -> throw new IllegalStateException("Unexpected value");
      }
    }

    ctx.sendMessage(prefix().insert(msg("Importing from ElbaphFactions...", COLOR_YELLOW)));
    ctx.sendMessage(msg("  Path: " + dataPath, COLOR_GRAY));
    if (dryRun) {
      ctx.sendMessage(msg("  (Dry run - no changes will be made)", COLOR_GRAY));
    }

    ElbaphFactionsImporter importer = new ElbaphFactionsImporter(
      hyperFactions.getFactionManager(),
      hyperFactions.getClaimManager(),
      hyperFactions.getZoneManager(),
      hyperFactions.getPowerManager(),
      hyperFactions.getBackupManager()
    );

    importer.setDryRun(dryRun);
    importer.setOverwrite(overwrite);
    importer.setSkipZones(skipZones);
    importer.setSkipPower(skipPower);

    final boolean finalDryRun = dryRun;
    CompletableFuture.supplyAsync(() -> importer.importFrom(dataPath))
      .thenAccept(result -> reportImportResult(ctx, result, finalDryRun, "ElbaphFactions"));
  }

  /** Handles import factions x. */
  public void handleImportFactionsX(CommandContext ctx, String[] args) {
    // Parse path (optional - default to mods/FactionsX)
    String pathStr = "mods/FactionsX";
    int flagStartIndex = 0;

    if (args.length > 0 && !args[0].startsWith("-")) {
      pathStr = args[0];
      flagStartIndex = 1;
    }

    Path dataPath = Paths.get(pathStr);

    boolean dryRun = false;
    boolean overwrite = false;
    boolean skipZones = false;
    boolean skipPower = false;

    for (int i = flagStartIndex; i < args.length; i++) {
      String flag = args[i].toLowerCase();
      switch (flag) {
        case "--dry-run", "-n" -> dryRun = true;
        case "--overwrite" -> overwrite = true;
        case "--no-zones" -> skipZones = true;
        case "--no-power" -> skipPower = true;
        default -> throw new IllegalStateException("Unexpected value");
      }
    }

    ctx.sendMessage(prefix().insert(msg("Importing from FactionsX...", COLOR_YELLOW)));
    ctx.sendMessage(msg("  Path: " + dataPath, COLOR_GRAY));
    if (dryRun) {
      ctx.sendMessage(msg("  (Dry run - no changes will be made)", COLOR_GRAY));
    }

    FactionsXImporter importer = new FactionsXImporter(
      hyperFactions.getFactionManager(),
      hyperFactions.getClaimManager(),
      hyperFactions.getZoneManager(),
      hyperFactions.getPowerManager(),
      hyperFactions.getBackupManager()
    );

    importer.setDryRun(dryRun);
    importer.setOverwrite(overwrite);
    importer.setSkipZones(skipZones);
    importer.setSkipPower(skipPower);

    final boolean finalDryRun = dryRun;
    CompletableFuture.supplyAsync(() -> importer.importFrom(dataPath))
      .thenAccept(result -> reportImportResult(ctx, result, finalDryRun, "FactionsX"));
  }

  /** Handles import simple claims. */
  public void handleImportSimpleClaims(CommandContext ctx, String[] args) {
    // Parse path (optional - default to Server/universe/SimpleClaims)
    String pathStr = "Server/universe/SimpleClaims";
    int flagStartIndex = 0;

    if (args.length > 0 && !args[0].startsWith("-")) {
      pathStr = args[0];
      flagStartIndex = 1;
    }

    Path dataPath = Paths.get(pathStr);

    boolean dryRun = false;
    boolean overwrite = false;
    boolean skipPower = false;

    for (int i = flagStartIndex; i < args.length; i++) {
      String flag = args[i].toLowerCase();
      switch (flag) {
        case "--dry-run", "-n" -> dryRun = true;
        case "--overwrite" -> overwrite = true;
        case "--no-power" -> skipPower = true;
        default -> throw new IllegalStateException("Unexpected value");
      }
    }

    ctx.sendMessage(prefix().insert(msg("Importing from SimpleClaims...", COLOR_YELLOW)));
    ctx.sendMessage(msg("  Path: " + dataPath, COLOR_GRAY));
    if (dryRun) {
      ctx.sendMessage(msg("  (Dry run - no changes will be made)", COLOR_GRAY));
    }

    SimpleClaimsImporter importer = new SimpleClaimsImporter(
      hyperFactions.getFactionManager(),
      hyperFactions.getClaimManager(),
      hyperFactions.getZoneManager(),
      hyperFactions.getPowerManager(),
      hyperFactions.getBackupManager()
    );

    importer.setDryRun(dryRun);
    importer.setOverwrite(overwrite);
    importer.setSkipPower(skipPower);

    final boolean finalDryRun = dryRun;
    CompletableFuture.supplyAsync(() -> importer.importFrom(dataPath))
      .thenAccept(result -> reportImportResult(ctx, result, finalDryRun, "SimpleClaims"));
  }

  private void reportImportResult(CommandContext ctx, ImportResult result, boolean dryRun, String sourceName) {
    if (!result.hasErrors()) {
      ctx.sendMessage(prefix().insert(msg(sourceName + " import " + (dryRun ? "simulation " : "") + "complete!", COLOR_GREEN)));
      ctx.sendMessage(msg("  Factions: " + result.factionsImported(), COLOR_GRAY));
      ctx.sendMessage(msg("  Claims: " + result.claimsImported(), COLOR_GRAY));
      ctx.sendMessage(msg("  Zones: " + result.zonesCreated(), COLOR_GRAY));
      ctx.sendMessage(msg("  Players with power: " + result.playersWithPower(), COLOR_GRAY));
      if (result.factionsSkipped() > 0) {
        ctx.sendMessage(msg("  Skipped: " + result.factionsSkipped(), COLOR_YELLOW));
      }
      if (result.hasWarnings()) {
        ctx.sendMessage(msg("  Warnings: " + result.warnings().size() + " (check logs)", COLOR_YELLOW));
      }
    } else {
      ctx.sendMessage(prefix().insert(msg(sourceName + " import failed with errors:", COLOR_RED)));
      for (String error : result.errors()) {
        ctx.sendMessage(msg("  - " + error, COLOR_RED));
      }
    }
  }
}
