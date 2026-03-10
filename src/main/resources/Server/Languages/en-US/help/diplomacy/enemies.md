---
id: diplomacy_enemies
commands: enemy, neutral
---
# Enemy Factions

Declaring an enemy is a **one-way action** that immediately enables PvP and territorial aggression against the target faction. No agreement is required.

---

## Declaring an Enemy

`/f enemy <faction>`

Instantly marks the target faction as your enemy. This takes effect immediately -- no confirmation from the other side is needed. Requires Officer rank or higher.

## Resetting to Neutral

`/f neutral <faction>`

Ends the enemy status and resets the relation to neutral. This also requires Officer+ and takes effect immediately.

---

## What Enemy Status Enables

| Effect | Details |
|--------|---------|
| **PvP in territory** | Full PvP is enabled in both factions' territory |
| **Overclaiming** | You can `/f overclaim` their chunks if they are in a power deficit |
| **Map marking** | Enemy territory shows in [#FF5555] red on the territory map |
| **No protection** | Standard territory protection does not prevent enemy PvP |

>[!WARNING] Declaring an enemy is a serious decision. Their members can also fight you in your own territory once you declare.

---

## Strategic Considerations

- Enemy declarations are **one-way** -- you can declare without their consent, but they also see you as hostile
- Before declaring, check the target's power with `/f info <faction>`. If they are strong, you may lose territory instead
- Weaken enemies through repeated combat to drain their power, then overclaim their land
- There is **no limit** to how many enemies you can have, but fighting on multiple fronts is risky

>[!TIP] Use `/f neutral <faction>` to de-escalate conflicts. Sometimes a strategic peace is more valuable than continued war.

>[!NOTE] If you are allied with a faction and declare them as an enemy, the alliance is broken first.
