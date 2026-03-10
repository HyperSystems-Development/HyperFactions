---
id: economy_commands
---
# Economy Commands

Quick reference for all faction economy commands.

| Command | Description | Role |
|---------|-------------|------|
| /f balance | View treasury balance | Any |
| /f deposit (amount) | Deposit into treasury | Any |
| /f withdraw (amount) | Withdraw from treasury | Officer+ |
| /f money transfer (faction) (amount) | Transfer to another faction | Officer+ |
| /f money log [page] | View transaction history | Officer+ |

---

## Command Aliases

- `/f balance` can also be used as `/f bal`
- `/f deposit` and `/f withdraw` accept decimal amounts

## Permissions

All economy commands require `hyperfactions.economy.*` permission nodes. Withdraw and transfer are further restricted by faction role (Officer or higher).

>[!TIP] Use /f money log to review recent deposits, withdrawals, and transfers with timestamps.
