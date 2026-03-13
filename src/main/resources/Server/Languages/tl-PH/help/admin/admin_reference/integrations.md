---
id: admin_integrations
---
# Mga Plugin Integration

Ang HyperFactions ay nag-i-integrate sa ilang external plugin sa pamamagitan ng mga soft dependency. Lahat ng integration ay opsyonal at gracefully na nagfa-fail kung hindi available.

## Pagsuri ng Integration Status

`/f admin version`
Ipinapakita ang kasalukuyang bersyon at mga na-detect na integration.

`/f admin integration`
Binubuksan ang integration management panel na may detalyadong status para sa bawat na-detect na plugin.

## Talahanayan ng Integration

| Plugin | Uri | Paglalarawan |
|--------|-----|-------------|
| **HyperPerms** | Permissions | Buong permission system na may mga grupo, inheritance, at context |
| **LuckPerms** | Permissions | Alternatibong permission provider |
| **VaultUnlocked** | Permissions/Economy | Permission at economy bridge |
| **HyperProtect-Mixin** | Protection | Nag-e-enable ng mga advanced zone flag (explosions, fire, keep inventory) |
| **OrbisGuard-Mixins** | Protection | Alternatibong mixin para sa zone flag enforcement |
| **PlaceholderAPI** | Placeholders | 49 faction placeholder para sa ibang plugin |
| **WiFlow PlaceholderAPI** | Placeholders | Alternatibong placeholder provider |
| **GravestonePlugin** | Death | Gravestone access control sa mga zone |
| **HyperEssentials** | Features | Zone flags para sa homes, warps, at kits |
| **KyuubiSoft Core** | Framework | Core library integration |
| **Sentry** | Monitoring | Error tracking at diagnostics |

## Priority ng Permission Provider

1. **VaultUnlocked** (pinakamataas na priority)
2. **HyperPerms**
3. **LuckPerms**
4. **OP fallback** (kung walang nakitang provider)

>[!INFO] Ang mga integration ay nide-detect nang isang beses sa startup gamit ang reflection. Ang mga resulta ay naka-cache para sa session. Kailangan ng server restart pagkatapos magdagdag o magtanggal ng integrated plugin.

>[!TIP] Gamitin ang `/f admin debug toggle integration` para mag-enable ng detalyadong integration logging para sa troubleshooting.

>[!NOTE] Ang HyperProtect-Mixin ang **inirerekomendang** protection mixin. Kung wala ito, 15 zone flag ang walang epekto.
