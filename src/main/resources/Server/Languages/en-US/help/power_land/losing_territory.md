---
id: power_losing
commands: overclaim
---
# Losing Territory

When a faction's total power drops below the cost of its claims, it becomes **raidable**. Enemies can overclaim chunks right out from under you.

---

## How Overclaiming Works

`/f overclaim`

An Officer or Leader from an **enemy** faction stands in your claimed chunk and runs this command. If your faction is in a power deficit, the chunk transfers to their faction.

## The Math

Each claim costs **2.0 power** to maintain. If your total power falls below that threshold, the deficit chunks are vulnerable.

>[!WARNING] Overclaiming is permanent. Once an enemy takes a chunk, you must reclaim it (or overclaim it back if they weaken).

---

## Example Scenario

| Factor | Value |
|--------|-------|
| Members | 5 players |
| Power per member | 10 each (starting) |
| **Total power** | **50** |
| Claims | 30 chunks |
| Power needed (30 x 2.0) | **60** |
| **Deficit** | **10 power short** |

In this example, the faction is already raidable from the start. Enemies could overclaim up to **5 chunks** (10 deficit / 2.0 per claim) before the faction reaches equilibrium.

---

## How to Prevent Overclaiming

- **Do not over-expand** -- always keep total power above your claim cost with a buffer
- **Stay active** -- power only regenerates while online (+0.1/min)
- **Avoid unnecessary deaths** -- each death costs 1.0 power
- **Recruit more members** -- more players means more total power
- **Unclaim unused chunks** -- free up power with `/f unclaim`

>[!TIP] Check your power status regularly with `/f power`. If your total power is close to your claim cost, consider unclaiming less important chunks before a war.
