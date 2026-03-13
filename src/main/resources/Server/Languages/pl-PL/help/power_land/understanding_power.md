---
id: power_understanding
commands: power
---
# Understanding Power

Power is the core resource that determines how much territory your faction can hold. Every player has personal power that contributes to the faction total.

---

## Default Power Values

| Setting | Value |
|---------|-------|
| Maximum power per player | 20 |
| Starting power | 10 |
| Death penalty | -1.0 per death |
| Kill reward | 0.0 |
| Regen rate | +0.1 per minute (while online) |
| Power cost per claim | 2.0 |
| Logout while tagged | -1.0 additional |

>[!NOTE] These are default values. Your server administrator may have configured different settings.

## How It Works

Your faction's total power is the sum of every member's personal power. Your required power is the number of claims multiplied by 2.0. As long as total power stays above required power, your territory is safe.

>[!INFO] Power regenerates passively at 0.1 per minute while you are online. At that rate, recovering 1.0 power takes about 10 minutes.

---

## Checking Your Power

`/f power`

Shows your personal power, your faction's total power, and how much is needed to maintain current claims.

## The Danger Zone

If total power falls below the required amount for your claims, your faction becomes vulnerable. Enemies can overclaim your chunks.

>[!WARNING] Multiple deaths in a short period can cascade quickly. If you have 5 members each at 10 power (50 total) and 20 claims (40 needed), just 5 deaths across your team drops you to 45 -- still safe. But 11 deaths puts you at 39, below the 40 threshold.

>[!TIP] Keep a power buffer. Do not claim every chunk you can afford -- leave room for a few deaths without becoming raidable.
