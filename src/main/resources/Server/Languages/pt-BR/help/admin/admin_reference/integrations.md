---
id: admin_integrations
---
# Plugin Integrations

HyperFactions integrates with several external plugins through soft dependencies. All integrations are optional and fail gracefully if unavailable.

## Checking Integration Status

`/f admin version`
Shows current version and detected integrations.

`/f admin integration`
Opens the integration management panel with detailed status for each detected plugin.

## Integration Table

| Plugin | Type | Description |
|--------|------|-------------|
| **HyperPerms** | Permissions | Full permission system with groups, inheritance, and context |
| **LuckPerms** | Permissions | Alternative permission provider |
| **VaultUnlocked** | Permissions/Economy | Permission and economy bridge |
| **HyperProtect-Mixin** | Protection | Enables advanced zone flags (explosions, fire, keep inventory) |
| **OrbisGuard-Mixins** | Protection | Alternative mixin for zone flag enforcement |
| **PlaceholderAPI** | Placeholders | 49 faction placeholders for other plugins |
| **WiFlow PlaceholderAPI** | Placeholders | Alternative placeholder provider |
| **GravestonePlugin** | Death | Gravestone access control in zones |
| **HyperEssentials** | Features | Zone flags for homes, warps, and kits |
| **KyuubiSoft Core** | Framework | Core library integration |
| **Sentry** | Monitoring | Error tracking and diagnostics |

## Permission Provider Priority

1. **VaultUnlocked** (highest priority)
2. **HyperPerms**
3. **LuckPerms**
4. **OP fallback** (if no provider found)

>[!INFO] Integrations are detected once at startup using reflection. Results are cached for the session. A server restart is required after adding or removing an integrated plugin.

>[!TIP] Use `/f admin debug toggle integration` to enable detailed integration logging for troubleshooting.

>[!NOTE] HyperProtect-Mixin is the **recommended** protection mixin. Without it, 15 zone flags will have no effect.
