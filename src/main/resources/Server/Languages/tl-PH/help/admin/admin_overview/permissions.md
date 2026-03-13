---
id: admin_permissions
---
# Mga Admin Permission

Lahat ng admin feature ay naka-gate sa likod ng mga permission node sa `hyperfactions.admin` namespace.

## Mga Permission Node

| Permission | Paglalarawan |
|-----------|-------------|
| `hyperfactions.admin.*` | Nagbibigay ng **lahat** ng admin permission |
| `hyperfactions.admin.use` | Access sa `/f admin` dashboard |
| `hyperfactions.admin.reload` | Mag-reload ng mga configuration file |
| `hyperfactions.admin.debug` | I-toggle ang mga debug logging category |
| `hyperfactions.admin.zones` | Gumawa, mag-edit, at mag-delete ng mga zone |
| `hyperfactions.admin.disband` | Mag-force-disband ng kahit anong faction |
| `hyperfactions.admin.modify` | Mag-modify ng settings ng kahit anong faction |
| `hyperfactions.admin.bypass.limits` | Mag-bypass ng claim at power limits |
| `hyperfactions.admin.backup` | Gumawa at mag-restore ng mga backup |
| `hyperfactions.admin.power` | Mag-override ng player power values |
| `hyperfactions.admin.economy` | Pamahalaan ang mga faction treasury |

## Fallback Behavior

Kapag **walang naka-install na permission plugin**, ang mga admin permission ay bumabalik sa server operator (OP) status. Kontrolado ito ng `adminRequiresOp` sa server config (default: `true`).

>[!NOTE] Ang `hyperfactions.admin.*` wildcard ay nagbibigay ng bawat admin permission. Gumamit ng individual node para sa granular na kontrol sa staff team mo.

## Pagkakasunud-sunod ng Permission Resolution

1. **VaultUnlocked** provider (kung available)
2. **HyperPerms** provider (kung available)
3. **LuckPerms** provider (kung available)
4. **OP check** para sa mga admin node (fallback)

>[!WARNING] Kapag walang permission plugin at naka-disable ang `adminRequiresOp`, ang mga admin command ay **bukas sa lahat ng manlalaro**. Palaging gumamit ng permission plugin sa production.
