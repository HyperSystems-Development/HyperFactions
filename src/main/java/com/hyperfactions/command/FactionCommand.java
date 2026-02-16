package com.hyperfactions.command;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.admin.AdminSubCommand;
import com.hyperfactions.command.economy.*;
import com.hyperfactions.command.faction.*;
import com.hyperfactions.command.info.*;
import com.hyperfactions.command.member.*;
import com.hyperfactions.command.relation.*;
import com.hyperfactions.command.social.*;
import com.hyperfactions.command.teleport.*;
import com.hyperfactions.command.territory.*;
import com.hyperfactions.command.ui.*;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Main faction command with all subcommands.
 * /faction (aliases: /f, /hf, /hyperfactions)
 */
public class FactionCommand extends AbstractPlayerCommand {

    private final HyperFactions hyperFactions;
    private final HyperFactionsPlugin plugin;

    public FactionCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("faction", "Faction management commands");
        this.hyperFactions = hyperFactions;
        this.plugin = plugin;

        addAliases("f", "hf", "hyperfactions");
        setAllowsExtraArguments(true);

        // Register all subcommands

        // Faction management
        addSubCommand(new CreateSubCommand(hyperFactions, plugin));
        addSubCommand(new DisbandSubCommand(hyperFactions, plugin));
        addSubCommand(new RenameSubCommand(hyperFactions, plugin));
        addSubCommand(new DescSubCommand(hyperFactions, plugin));
        addSubCommand(new ColorSubCommand(hyperFactions, plugin));
        addSubCommand(new OpenSubCommand(hyperFactions, plugin));
        addSubCommand(new CloseSubCommand(hyperFactions, plugin));

        // Member management
        addSubCommand(new InviteSubCommand(hyperFactions, plugin));
        addSubCommand(new AcceptSubCommand(hyperFactions, plugin));
        addSubCommand(new LeaveSubCommand(hyperFactions, plugin));
        addSubCommand(new KickSubCommand(hyperFactions, plugin));
        addSubCommand(new PromoteSubCommand(hyperFactions, plugin));
        addSubCommand(new DemoteSubCommand(hyperFactions, plugin));
        addSubCommand(new TransferSubCommand(hyperFactions, plugin));

        // Territory commands
        addSubCommand(new ClaimSubCommand(hyperFactions, plugin));
        addSubCommand(new UnclaimSubCommand(hyperFactions, plugin));
        addSubCommand(new OverclaimSubCommand(hyperFactions, plugin));
        addSubCommand(new StuckSubCommand(hyperFactions, plugin));

        // Teleport commands
        addSubCommand(new HomeSubCommand(hyperFactions, plugin));
        addSubCommand(new SetHomeSubCommand(hyperFactions, plugin));
        addSubCommand(new DelHomeSubCommand(hyperFactions, plugin));

        // Relation commands
        addSubCommand(new AllySubCommand(hyperFactions, plugin));
        addSubCommand(new EnemySubCommand(hyperFactions, plugin));
        addSubCommand(new NeutralSubCommand(hyperFactions, plugin));
        addSubCommand(new RelationsSubCommand(hyperFactions, plugin));

        // Information commands
        addSubCommand(new InfoSubCommand(hyperFactions, plugin));
        addSubCommand(new ListSubCommand(hyperFactions, plugin));
        addSubCommand(new MapSubCommand(hyperFactions, plugin));
        addSubCommand(new MembersSubCommand(hyperFactions, plugin));
        addSubCommand(new WhoSubCommand(hyperFactions, plugin));
        addSubCommand(new PowerSubCommand(hyperFactions, plugin));
        addSubCommand(new HelpSubCommand(hyperFactions, plugin));

        // Social commands
        addSubCommand(new RequestSubCommand(hyperFactions, plugin));
        addSubCommand(new InvitesSubCommand(hyperFactions, plugin));
        addSubCommand(new ChatSubCommand(hyperFactions, plugin));

        // Economy commands (only if treasury is enabled)
        if (hyperFactions.isTreasuryEnabled()) {
            addSubCommand(new MoneySubCommand(hyperFactions, plugin));
            addSubCommand(new BalanceSubCommand(hyperFactions, plugin));
            addSubCommand(new DepositSubCommand(hyperFactions, plugin));
            addSubCommand(new WithdrawSubCommand(hyperFactions, plugin));
        }

        // UI commands
        addSubCommand(new GuiSubCommand(hyperFactions, plugin));
        addSubCommand(new SettingsSubCommand(hyperFactions, plugin));

        // Admin commands
        addSubCommand(new AdminSubCommand(hyperFactions, plugin));
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        // No subcommand provided - open faction main dashboard GUI
        if (!hasPermission(player, Permissions.USE)) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg("You don't have permission to use factions.", CommandUtil.COLOR_RED)));
            return;
        }

        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
            hyperFactions.getGuiManager().openFactionMain(playerEntity, ref, store, player);
        } else {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg("Could not access GUI. Use /f help for commands.", CommandUtil.COLOR_YELLOW)));
        }
    }

    /**
     * Disable Hytale's auto-generated permissions.
     * We use our own permission system via hasPermission() checks.
     */
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    /**
     * Permission check helper.
     */
    private boolean hasPermission(PlayerRef player, String permission) {
        return PermissionManager.get().hasPermission(player.getUuid(), permission);
    }
}
