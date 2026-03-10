---
id: economy_upkeep
---
# Territory Upkeep

Factions must pay ongoing upkeep to maintain their
claimed territory. This prevents land hoarding and
keeps the map dynamic.

## Upkeep Costs

| Setting | Default |
|---------|---------|
| Cost per chunk | 2.0 per cycle |
| Payment interval | Every 24 hours |
| Free chunks | 3 (no cost) |
| Scaling mode | Flat rate |

Your first **3 chunks are free**. Beyond that, each
additional claimed chunk costs 2.0 per payment cycle.

## Auto-Pay

Auto-pay is **enabled by default**. The system
automatically deducts upkeep from your treasury at
each interval. No manual action needed.

---

## Grace Period

If your treasury cannot cover upkeep, a **48-hour
grace period** begins. A warning is sent 6 hours
before claims start being lost.

>[!WARNING] If upkeep remains unpaid after the grace period, your faction loses 1 claim per cycle until costs are covered or all extra claims are gone.

## Example

*A faction with 8 claims pays for 5 chunks (8 minus 3 free). At 2.0 per chunk, that is 10.0 per cycle.*

>[!TIP] Keep your treasury funded above your upkeep cost. Use /f balance to check your reserves.
