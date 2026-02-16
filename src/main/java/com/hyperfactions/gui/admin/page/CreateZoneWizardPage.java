package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.data.AdminZoneData;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Create Zone Wizard page - modern card-based UI with 5 claiming methods.
 * Uses immutable state pattern: each state change creates a fresh page instance.
 */
public class CreateZoneWizardPage extends InteractiveCustomUIPage<AdminZoneData> {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 32;
    private static final int[] RADIUS_PRESETS = {3, 5, 10, 15, 20};
    private static final int DEFAULT_RADIUS = 5;
    private static final int MAX_RADIUS = 50;

    /**
     * Available claiming methods for zone creation.
     */
    public enum ClaimMethod {
        NO_CLAIMS,        // Create empty zone
        SINGLE_CHUNK,     // Current location
        RADIUS_CIRCLE,    // Circular radius around player
        RADIUS_SQUARE,    // Square radius around player
        USE_MAP           // Navigate to map after creation
    }

    private final PlayerRef playerRef;
    private final ZoneManager zoneManager;
    private final GuiManager guiManager;

    // Immutable state fields (set via constructor for fresh page pattern)
    private final ZoneType selectedType;
    private final String preservedName;
    private final ClaimMethod claimMethod;
    private final int selectedRadius;
    private final boolean customizeFlags;

    /**
     * Default constructor - opens with SafeZone selected and defaults.
     */
    public CreateZoneWizardPage(PlayerRef playerRef,
                                ZoneManager zoneManager,
                                GuiManager guiManager) {
        this(playerRef, zoneManager, guiManager, ZoneType.SAFE, "", ClaimMethod.NO_CLAIMS, DEFAULT_RADIUS, false);
    }

    /**
     * Constructor with selected zone type.
     */
    public CreateZoneWizardPage(PlayerRef playerRef,
                                ZoneManager zoneManager,
                                GuiManager guiManager,
                                ZoneType selectedType) {
        this(playerRef, zoneManager, guiManager, selectedType, "", ClaimMethod.NO_CLAIMS, DEFAULT_RADIUS, false);
    }

    /**
     * Constructor with selected zone type and preserved name.
     * Used when user switches zone type to preserve the entered name.
     */
    public CreateZoneWizardPage(PlayerRef playerRef,
                                ZoneManager zoneManager,
                                GuiManager guiManager,
                                ZoneType selectedType,
                                String preservedName) {
        this(playerRef, zoneManager, guiManager, selectedType, preservedName, ClaimMethod.NO_CLAIMS, DEFAULT_RADIUS, false);
    }

    /**
     * Full constructor with all state fields.
     */
    public CreateZoneWizardPage(PlayerRef playerRef,
                                ZoneManager zoneManager,
                                GuiManager guiManager,
                                ZoneType selectedType,
                                String preservedName,
                                ClaimMethod claimMethod,
                                int selectedRadius,
                                boolean customizeFlags) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminZoneData.CODEC);
        this.playerRef = playerRef;
        this.zoneManager = zoneManager;
        this.guiManager = guiManager;
        this.selectedType = selectedType != null ? selectedType : ZoneType.SAFE;
        this.preservedName = preservedName != null ? preservedName : "";
        this.claimMethod = claimMethod != null ? claimMethod : ClaimMethod.NO_CLAIMS;
        this.selectedRadius = Math.max(1, Math.min(MAX_RADIUS, selectedRadius));
        this.customizeFlags = customizeFlags;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        Logger.debug("[CreateZoneWizardPage] build() type=%s, method=%s, radius=%d, customizeFlags=%s",
                selectedType.name(), claimMethod.name(), selectedRadius, customizeFlags);

        // Load the template
        cmd.append("HyperFactions/admin/create_zone_wizard.ui");

        // Restore preserved input value
        if (!preservedName.isEmpty()) {
            cmd.set("#NameInput.Value", preservedName);
        }

        // Setup zone type display
        buildZoneTypeSection(cmd, events);

        // Setup claiming method section
        buildClaimMethodSection(cmd, events);

        // Setup radius section (visible/hidden based on claim method)
        buildRadiusSection(cmd, events);

        // Setup flags section
        buildFlagsSection(cmd, events);

        // Back button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );

        // Create button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CreateBtn",
                EventData.of("Button", "Create")
                        .append("ZoneType", selectedType == ZoneType.SAFE ? "safe" : "war")
                        .append("@Name", "#NameInput.Value")
                        .append("ClaimMethod", claimMethod.name().toLowerCase())
                        .append("Radius", String.valueOf(selectedRadius))
                        .append("FlagsChoice", customizeFlags ? "customize" : "defaults"),
                false
        );
    }

    private void buildZoneTypeSection(UICommandBuilder cmd, UIEventBuilder events) {
        // Use disabled state to show selected type
        boolean isSafe = selectedType == ZoneType.SAFE;
        cmd.set("#SafeZoneBtn.Disabled", isSafe);
        cmd.set("#WarZoneBtn.Disabled", !isSafe);

        // Type selection buttons - capture name when switching
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SafeZoneBtn",
                EventData.of("Button", "SelectType")
                        .append("ZoneType", "safe")
                        .append("@Name", "#NameInput.Value"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#WarZoneBtn",
                EventData.of("Button", "SelectType")
                        .append("ZoneType", "war")
                        .append("@Name", "#NameInput.Value"),
                false
        );
    }

    private void buildClaimMethodSection(UICommandBuilder cmd, UIEventBuilder events) {
        // Highlight selected method by using cyan style
        String[] methodButtons = {"#MethodNone", "#MethodSingle", "#MethodCircle", "#MethodSquare", "#MethodMap"};
        ClaimMethod[] methods = {ClaimMethod.NO_CLAIMS, ClaimMethod.SINGLE_CHUNK,
                                 ClaimMethod.RADIUS_CIRCLE, ClaimMethod.RADIUS_SQUARE, ClaimMethod.USE_MAP};

        for (int i = 0; i < methodButtons.length; i++) {
            // We can't dynamically change button styles, so we use Disabled state to indicate selection
            cmd.set(methodButtons[i] + ".Disabled", claimMethod == methods[i]);
        }

        // Event bindings for each method
        for (ClaimMethod method : ClaimMethod.values()) {
            String buttonId = switch (method) {
                case NO_CLAIMS -> "#MethodNone";
                case SINGLE_CHUNK -> "#MethodSingle";
                case RADIUS_CIRCLE -> "#MethodCircle";
                case RADIUS_SQUARE -> "#MethodSquare";
                case USE_MAP -> "#MethodMap";
            };

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    buttonId,
                    EventData.of("Button", "SetClaimMethod")
                            .append("ClaimMethod", method.name().toLowerCase())
                            .append("@Name", "#NameInput.Value"),
                    false
            );
        }
    }

    private void buildRadiusSection(UICommandBuilder cmd, UIEventBuilder events) {
        // Show/hide radius card based on claim method
        boolean showRadius = claimMethod == ClaimMethod.RADIUS_CIRCLE || claimMethod == ClaimMethod.RADIUS_SQUARE;
        cmd.set("#RadiusCard.Visible", showRadius);

        if (!showRadius) {
            return;
        }

        // Calculate and show preview
        int previewChunks = calculateChunkCount(selectedRadius, claimMethod == ClaimMethod.RADIUS_CIRCLE);
        cmd.set("#RadiusPreview.Text", "~" + previewChunks + " chunks");

        // Highlight selected preset
        for (int preset : RADIUS_PRESETS) {
            cmd.set("#Radius" + preset + ".Disabled", selectedRadius == preset);
        }

        // Show custom radius value in the input field
        boolean isCustomRadius = true;
        for (int preset : RADIUS_PRESETS) {
            if (selectedRadius == preset) {
                isCustomRadius = false;
                break;
            }
        }
        if (isCustomRadius || selectedRadius > 20) {
            cmd.set("#CustomRadiusInput.Value", String.valueOf(selectedRadius));
        }

        // Preset button events
        for (int preset : RADIUS_PRESETS) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#Radius" + preset,
                    EventData.of("Button", "SetRadius")
                            .append("Radius", String.valueOf(preset))
                            .append("@Name", "#NameInput.Value"),
                    false
            );
        }

        // Custom radius apply button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ApplyCustomRadius",
                EventData.of("Button", "ApplyCustomRadius")
                        .append("@CustomRadius", "#CustomRadiusInput.Value")
                        .append("@Name", "#NameInput.Value"),
                false
        );
    }

    private void buildFlagsSection(UICommandBuilder cmd, UIEventBuilder events) {
        // Highlight selected choice
        cmd.set("#FlagsDefaults.Disabled", !customizeFlags);
        cmd.set("#FlagsCustomize.Disabled", customizeFlags);

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#FlagsDefaults",
                EventData.of("Button", "SetFlagsChoice")
                        .append("FlagsChoice", "defaults")
                        .append("@Name", "#NameInput.Value"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#FlagsCustomize",
                EventData.of("Button", "SetFlagsChoice")
                        .append("FlagsChoice", "customize")
                        .append("@Name", "#NameInput.Value"),
                false
        );
    }

    /**
     * Calculates approximate chunk count for a radius.
     */
    private int calculateChunkCount(int radius, boolean circle) {
        if (circle) {
            // Circle: pi * r^2 (approximate)
            return (int) Math.ceil(Math.PI * radius * radius);
        } else {
            // Square: (2r+1)^2
            return (2 * radius + 1) * (2 * radius + 1);
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminZoneData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        World world = player != null ? player.getWorld() : null;
        String worldName = world != null ? world.getName() : "world";

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        switch (data.button) {
            case "Back" -> guiManager.openAdminZone(player, ref, store, playerRef);

            case "SelectType" -> {
                ZoneType newType = "war".equals(data.zoneType) ? ZoneType.WAR : ZoneType.SAFE;
                String name = data.inputName != null ? data.inputName : preservedName;
                guiManager.openCreateZoneWizard(player, ref, store, playerRef, newType, name,
                        claimMethod, selectedRadius, customizeFlags);
            }

            case "SetClaimMethod" -> {
                ClaimMethod newMethod = parseClaimMethod(data.claimMethod);
                String name = data.inputName != null ? data.inputName : preservedName;
                guiManager.openCreateZoneWizard(player, ref, store, playerRef, selectedType, name,
                        newMethod, selectedRadius, customizeFlags);
            }

            case "SetRadius" -> {
                int newRadius = parseRadius(data.radius);
                String name = data.inputName != null ? data.inputName : preservedName;
                guiManager.openCreateZoneWizard(player, ref, store, playerRef, selectedType, name,
                        claimMethod, newRadius, customizeFlags);
            }

            case "ApplyCustomRadius" -> {
                int newRadius = parseRadius(data.customRadius);
                if (newRadius < 1 || newRadius > MAX_RADIUS) {
                    player.sendMessage(MessageUtil.errorText("Radius must be between 1 and " + MAX_RADIUS + "."));
                    sendUpdate();
                    return;
                }
                String name = data.inputName != null ? data.inputName : preservedName;
                guiManager.openCreateZoneWizard(player, ref, store, playerRef, selectedType, name,
                        claimMethod, newRadius, customizeFlags);
            }

            case "SetFlagsChoice" -> {
                boolean newCustomize = "customize".equals(data.flagsChoice);
                String name = data.inputName != null ? data.inputName : preservedName;
                guiManager.openCreateZoneWizard(player, ref, store, playerRef, selectedType, name,
                        claimMethod, selectedRadius, newCustomize);
            }

            case "Create" -> handleCreate(player, ref, store, playerRef, data, worldName);

            default -> sendUpdate();
        }
    }

    private ClaimMethod parseClaimMethod(String value) {
        if (value == null) return ClaimMethod.NO_CLAIMS;
        return switch (value.toLowerCase()) {
            case "single_chunk" -> ClaimMethod.SINGLE_CHUNK;
            case "radius_circle" -> ClaimMethod.RADIUS_CIRCLE;
            case "radius_square" -> ClaimMethod.RADIUS_SQUARE;
            case "use_map" -> ClaimMethod.USE_MAP;
            default -> ClaimMethod.NO_CLAIMS;
        };
    }

    private int parseRadius(String value) {
        if (value == null || value.isEmpty()) return DEFAULT_RADIUS;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_RADIUS;
        }
    }

    private void handleCreate(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                              PlayerRef playerRef, AdminZoneData data, String worldName) {
        String name = data.inputName != null ? data.inputName.trim() : "";

        // Validate zone name
        if (name.isEmpty()) {
            player.sendMessage(MessageUtil.errorText("Please enter a zone name."));
            sendUpdate();
            return;
        }

        if (name.length() < MIN_NAME_LENGTH) {
            player.sendMessage(MessageUtil.errorText("Zone name must be at least " + MIN_NAME_LENGTH + " characters."));
            sendUpdate();
            return;
        }

        if (name.length() > MAX_NAME_LENGTH) {
            player.sendMessage(MessageUtil.errorText("Zone name cannot exceed " + MAX_NAME_LENGTH + " characters."));
            sendUpdate();
            return;
        }

        // Check if name is already taken
        if (zoneManager.getZoneByName(name) != null) {
            player.sendMessage(MessageUtil.errorText("A zone with this name already exists."));
            sendUpdate();
            return;
        }

        // Get zone type
        ZoneType type = "war".equals(data.zoneType) ? ZoneType.WAR : ZoneType.SAFE;

        // Parse claim method and other options from data
        ClaimMethod method = parseClaimMethod(data.claimMethod);
        int radius = parseRadius(data.radius);
        boolean customize = "customize".equals(data.flagsChoice);

        // Create the zone (always starts empty)
        ZoneManager.ZoneResult result = zoneManager.createZone(name, type, worldName, playerRef.getUuid());

        if (result != ZoneManager.ZoneResult.SUCCESS) {
            player.sendMessage(MessageUtil.errorText("Could not create zone: " + result));
            sendUpdate();
            return;
        }

        Zone newZone = zoneManager.getZoneByName(name);
        if (newZone == null) {
            player.sendMessage(MessageUtil.text("Zone created but could not be found.", MessageUtil.COLOR_GOLD));
            guiManager.openAdminZone(player, ref, store, playerRef);
            return;
        }

        // Success message
        String typeColor = type == ZoneType.SAFE ? "#55FF55" : "#FF5555";
        player.sendMessage(
                Message.raw("Created ").color("#55FF55")
                        .insert(Message.raw(type.getDisplayName()).color(typeColor))
                        .insert(Message.raw(" '" + name + "'!").color("#55FF55"))
        );

        // Handle claiming based on method
        switch (method) {
            case SINGLE_CHUNK -> {
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform != null) {
                    var position = transform.getPosition();
                    int chunkX = ChunkUtil.blockToChunk((int) position.x);
                    int chunkZ = ChunkUtil.blockToChunk((int) position.z);

                    ZoneManager.ZoneResult claimResult = zoneManager.claimChunk(
                            newZone.id(), worldName, chunkX, chunkZ);

                    if (claimResult == ZoneManager.ZoneResult.SUCCESS) {
                        player.sendMessage(MessageUtil.text("Claimed chunk (" + chunkX + ", " + chunkZ + ").", "#44cc44"));
                        newZone = zoneManager.getZoneById(newZone.id());
                    } else {
                        player.sendMessage(MessageUtil.text("Could not claim current chunk: " + claimResult, MessageUtil.COLOR_GOLD));
                    }
                }
            }

            case RADIUS_CIRCLE, RADIUS_SQUARE -> {
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform != null) {
                    var position = transform.getPosition();
                    int centerX = ChunkUtil.blockToChunk((int) position.x);
                    int centerZ = ChunkUtil.blockToChunk((int) position.z);
                    boolean circle = method == ClaimMethod.RADIUS_CIRCLE;

                    int claimed = zoneManager.claimRadius(newZone.id(), worldName, centerX, centerZ, radius, circle);

                    if (claimed > 0) {
                        player.sendMessage(MessageUtil.text("Claimed " + claimed + " chunks in a "
                                + (circle ? "circular" : "square") + " radius of " + radius + ".", "#44cc44"));
                        newZone = zoneManager.getZoneById(newZone.id());
                    } else {
                        player.sendMessage(MessageUtil.text("No chunks could be claimed (area may be occupied).", MessageUtil.COLOR_GOLD));
                    }
                }
            }

            case NO_CLAIMS, USE_MAP -> {
                // No chunks to claim now
                if (method == ClaimMethod.NO_CLAIMS) {
                    player.sendMessage(MessageUtil.text("Zone created with no claims.", "#888888"));
                }
            }
        }

        // Navigate based on user choices
        navigateAfterCreation(player, ref, store, playerRef, newZone, method, customize);
    }

    private void navigateAfterCreation(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                       PlayerRef playerRef, Zone zone, ClaimMethod method, boolean customize) {
        if (zone == null) {
            guiManager.openAdminZone(player, ref, store, playerRef);
            return;
        }

        if (method == ClaimMethod.USE_MAP) {
            // USE_MAP takes priority - open map for claiming first
            // If customize is also set, open map with flag to go to settings after
            guiManager.openAdminZoneMap(player, ref, store, playerRef, zone, customize);
        } else if (customize) {
            // Open flag settings
            guiManager.openAdminZoneSettings(player, ref, store, playerRef, zone.id());
        } else {
            // Return to zone list
            guiManager.openAdminZone(player, ref, store, playerRef);
        }
    }
}
