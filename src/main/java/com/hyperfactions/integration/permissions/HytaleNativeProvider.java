package com.hyperfactions.integration.permissions;

import com.hyperfactions.integration.PermissionProvider;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Permission provider that delegates to Hytale's native PermissionsModule.
 * Automatically supports any permission plugin that registers with
 * PermissionsModule.addProvider() (LuckPerms, PermissionsPlus, etc.)
 * without per-plugin reflection.
 *
 * This provider checks areProvidersTampered() at permission-check time,
 * solving the timing issue where LuckPerms loads after HyperFactions.
 * When no external providers are registered, returns Optional.empty()
 * so the existing fallback logic runs (respects allowWithoutPermissionMod).
 */
public class HytaleNativeProvider implements PermissionProvider {

    @Override
    @NotNull
    public String getName() {
        return "HytaleNative";
    }

    @Override
    public boolean isAvailable() {
        return PermissionsModule.get() != null;
    }

    @Override
    @NotNull
    public Optional<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
        PermissionsModule pm = PermissionsModule.get();
        if (pm == null) {
            return Optional.empty();
        }

        // Only delegate when external providers are registered.
        // areProvidersTampered() returns true when LuckPerms/PermissionsPlus/etc.
        // have added themselves via PermissionsModule.addProvider().
        if (!pm.areProvidersTampered()) {
            // No external providers — let PermissionManager fallback handle it
            // (respects allowWithoutPermissionMod config)
            return Optional.empty();
        }

        // Delegate to native system which routes through all registered providers.
        // PermissionsModule internally handles wildcards (*, prefix.*, -permission).
        boolean result = pm.hasPermission(playerUuid, permission);
        Logger.debug("[HytaleNativeProvider] %s for %s: %s", permission, playerUuid, result);
        return Optional.of(result);
    }

    // Native system doesn't expose prefix/suffix/group via PermissionsModule
    @Override
    @Nullable
    public String getPrefix(@NotNull UUID playerUuid, @Nullable String worldName) {
        return null;
    }

    @Override
    @Nullable
    public String getSuffix(@NotNull UUID playerUuid, @Nullable String worldName) {
        return null;
    }

    @Override
    @NotNull
    public String getPrimaryGroup(@NotNull UUID playerUuid) {
        return "default";
    }
}
