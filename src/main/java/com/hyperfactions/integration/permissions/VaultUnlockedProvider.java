package com.hyperfactions.integration.permissions;

import com.hyperfactions.integration.PermissionProvider;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Permission provider for VaultUnlocked (Vault2 API).
 * Uses reflection to integrate with VaultUnlocked's PermissionUnlocked and ChatUnlocked interfaces.
 *
 * Actual class paths (from decompiled VaultUnlocked-Hytale 2.18.3):
 * - Main: net.cfh.vault.VaultUnlocked
 * - Subject: net.milkbowl.vault2.helper.subject.Subject
 * - Context: net.milkbowl.vault2.helper.context.Context
 * - TriState: net.milkbowl.vault2.helper.TriState
 * - PermissionUnlocked: net.milkbowl.vault2.permission.PermissionUnlocked
 * - ChatUnlocked: net.milkbowl.vault2.chat.ChatUnlocked
 *
 * VaultUnlocked uses TriState for permission results:
 * - TRUE: Permission is explicitly granted
 * - FALSE: Permission is explicitly denied
 * - UNDEFINED: Permission is not set (fall through to next provider)
 */
public class VaultUnlockedProvider implements PermissionProvider {

    private volatile boolean available = false;
    private volatile boolean permanentFailure = false;

    // Reflection references
    private Object permissionService = null;
    private Object chatService = null;
    private Method hasPermissionMethod = null;
    private Method getPrefixMethod = null;
    private Method getSuffixMethod = null;
    private Method getPrimaryGroupMethod = null;

    // TriState enum values
    private Object triStateTrue = null;
    private Object triStateFalse = null;
    private Object triStateUndefined = null;

    // Context and Subject helpers
    private Method createSubjectMethod = null;
    private Object globalContext = null;
    private Class<?> contextClass = null;
    private Class<?> subjectClass = null;

    @Override
    @NotNull
    public String getName() {
        return "VaultUnlocked";
    }

    /**
     * Initializes the VaultUnlocked provider.
     * Attempts to load VaultUnlocked classes via reflection.
     * Safe to call multiple times — returns immediately if already initialized
     * or permanently failed (ClassNotFoundException = not installed).
     */
    public void init() {
        if (available || permanentFailure) return;

        try {
            // Load TriState enum
            Class<?> triStateClass = Class.forName("net.milkbowl.vault2.helper.TriState");
            triStateTrue = Enum.valueOf((Class<Enum>) triStateClass, "TRUE");
            triStateFalse = Enum.valueOf((Class<Enum>) triStateClass, "FALSE");
            triStateUndefined = Enum.valueOf((Class<Enum>) triStateClass, "UNDEFINED");

            // Load Subject class (note: sub-package .subject)
            subjectClass = Class.forName("net.milkbowl.vault2.helper.subject.Subject");
            createSubjectMethod = subjectClass.getMethod("player", UUID.class, String.class);

            // Load Context class (note: sub-package .context) and get global context
            contextClass = Class.forName("net.milkbowl.vault2.helper.context.Context");
            // Context.GLOBAL is a public static final field
            globalContext = contextClass.getField("GLOBAL").get(null);

            // Get VaultUnlocked main class (net.cfh.vault, NOT net.milkbowl.vault2)
            Class<?> vaultUnlockedClass = Class.forName("net.cfh.vault.VaultUnlocked");

            // VaultUnlocked.permission() returns Optional<PermissionUnlocked>
            Method permissionMethod = vaultUnlockedClass.getMethod("permission");
            Object permOptional = permissionMethod.invoke(null);
            if (permOptional instanceof Optional<?> opt) {
                permissionService = opt.orElse(null);
            }

            // VaultUnlocked.chat() returns Optional<ChatUnlocked>
            Method chatMethod = vaultUnlockedClass.getMethod("chat");
            Object chatOptional = chatMethod.invoke(null);
            if (chatOptional instanceof Optional<?> opt) {
                chatService = opt.orElse(null);
            }

            if (permissionService == null) {
                // Service not registered yet — may become available later
                Logger.debug("[VaultUnlockedProvider] Permission service not available yet");
                return;
            }

            // Get permission check method: has(Context, Subject, String) -> TriState
            Class<?> permissionClass = permissionService.getClass();
            hasPermissionMethod = findMethod(permissionClass, "has", contextClass, subjectClass, String.class);

            if (hasPermissionMethod == null) {
                Logger.debug("[VaultUnlockedProvider] No permission check method found");
                return;
            }

            // Get chat methods if chat service available
            if (chatService != null) {
                Class<?> chatClass = chatService.getClass();
                getPrefixMethod = findMethod(chatClass, "getPrefix", contextClass, subjectClass);
                getSuffixMethod = findMethod(chatClass, "getSuffix", contextClass, subjectClass);
            }

            // Get primary group method: primaryGroup(Context, Subject)
            getPrimaryGroupMethod = findMethod(permissionClass, "primaryGroup", contextClass, subjectClass);

            available = true;
            Logger.info("[Permissions] VaultUnlocked provider initialized");

        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            permanentFailure = true;
            Logger.debug("[VaultUnlockedProvider] VaultUnlocked not installed");
        } catch (Exception e) {
            // Other errors may be temporary (service not registered yet, etc.)
            Logger.debug("[VaultUnlockedProvider] Init deferred: %s", e.getMessage());
        }
    }

    /**
     * Ensures the provider is initialized. Called before every operation
     * to support lazy initialization when VaultUnlocked loads after HyperFactions.
     */
    private void ensureInitialized() {
        if (!available && !permanentFailure) {
            init();
        }
    }

    /**
     * Finds a method by name and parameter types, checking interfaces too.
     */
    private Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            // Try interfaces
            for (Class<?> iface : clazz.getInterfaces()) {
                try {
                    return iface.getMethod(name, paramTypes);
                } catch (NoSuchMethodException ignored) {
                }
            }
            // Try superclass
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return findMethod(superclass, name, paramTypes);
            }
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        ensureInitialized();
        return available;
    }

    @Override
    @NotNull
    public Optional<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
        ensureInitialized();
        if (!available || permissionService == null || hasPermissionMethod == null) {
            return Optional.empty();
        }

        try {
            Object subject = createSubjectMethod.invoke(null, playerUuid, null);
            Object result = hasPermissionMethod.invoke(permissionService, globalContext, subject, permission);

            if (result == null) {
                return Optional.empty();
            }

            if (result.equals(triStateTrue)) {
                return Optional.of(true);
            } else if (result.equals(triStateFalse)) {
                return Optional.of(false);
            } else if (result.equals(triStateUndefined)) {
                return Optional.empty();
            }

            return Optional.empty();

        } catch (Exception e) {
            Logger.debug("[VaultUnlockedProvider] Exception checking permission: %s", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Nullable
    public String getPrefix(@NotNull UUID playerUuid, @Nullable String worldName) {
        ensureInitialized();
        if (!available || chatService == null || getPrefixMethod == null) {
            return null;
        }

        try {
            Object subject = createSubjectMethod.invoke(null, playerUuid, null);
            Object context = worldName != null ? createWorldContext(worldName) : globalContext;

            // ChatUnlocked.getPrefix() returns Optional<String>
            Object result = getPrefixMethod.invoke(chatService, context, subject);

            if (result instanceof Optional<?> opt) {
                return opt.map(Object::toString).orElse(null);
            }
            return result != null ? result.toString() : null;

        } catch (Exception e) {
            Logger.debug("[VaultUnlockedProvider] Exception getting prefix: %s", e.getMessage());
            return null;
        }
    }

    @Override
    @Nullable
    public String getSuffix(@NotNull UUID playerUuid, @Nullable String worldName) {
        ensureInitialized();
        if (!available || chatService == null || getSuffixMethod == null) {
            return null;
        }

        try {
            Object subject = createSubjectMethod.invoke(null, playerUuid, null);
            Object context = worldName != null ? createWorldContext(worldName) : globalContext;

            // ChatUnlocked.getSuffix() returns Optional<String>
            Object result = getSuffixMethod.invoke(chatService, context, subject);

            if (result instanceof Optional<?> opt) {
                return opt.map(Object::toString).orElse(null);
            }
            return result != null ? result.toString() : null;

        } catch (Exception e) {
            Logger.debug("[VaultUnlockedProvider] Exception getting suffix: %s", e.getMessage());
            return null;
        }
    }

    @Override
    @NotNull
    public String getPrimaryGroup(@NotNull UUID playerUuid) {
        ensureInitialized();
        if (!available || permissionService == null || getPrimaryGroupMethod == null) {
            return "default";
        }

        try {
            Object subject = createSubjectMethod.invoke(null, playerUuid, null);
            // PermissionUnlocked.primaryGroup(Context, Subject) returns String
            Object result = getPrimaryGroupMethod.invoke(permissionService, globalContext, subject);
            return result != null ? result.toString() : "default";

        } catch (Exception e) {
            Logger.debug("[VaultUnlockedProvider] Exception getting primary group: %s", e.getMessage());
            return "default";
        }
    }

    /**
     * Creates a world-specific context using Context(String world) constructor.
     */
    private Object createWorldContext(String worldName) {
        try {
            if (contextClass != null) {
                return contextClass.getConstructor(String.class).newInstance(worldName);
            }
        } catch (Exception e) {
            // Fall through to global
        }
        return globalContext;
    }
}
