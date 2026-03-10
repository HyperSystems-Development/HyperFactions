---
id: admin_treasury_management
---
# Treasury Management

Admin commands for managing faction treasuries.
Requires `hyperfactions.admin.economy` permission.

## Treasury Commands

| Command | Description |
|---------|-------------|
| `/f admin economy balance <faction>` | View faction treasury balance |
| `/f admin economy set <faction> <amount>` | Set exact balance |
| `/f admin economy add <faction> <amount>` | Add funds to treasury |
| `/f admin economy take <faction> <amount>` | Remove funds from treasury |
| `/f admin economy reset <faction>` | Reset treasury to zero |

## Examples

- `/f admin economy balance Vikings` -- check balance
- `/f admin economy set Vikings 5000` -- set to 5000
- `/f admin economy add Vikings 1000` -- deposit 1000
- `/f admin economy take Vikings 500` -- withdraw 500
- `/f admin economy reset Vikings` -- zero out balance

>[!TIP] Use `/f admin info <faction>` to see the full economy overview including transaction history alongside the treasury balance.

## Use Cases

| Scenario | Command |
|----------|---------|
| Event prize distribution | `economy add <faction> <prize>` |
| Penalty for rule violation | `economy take <faction> <fine>` |
| Economy reset after wipe | `economy reset <faction>` |
| Compensation for bugs | `economy add <faction> <amount>` |

>[!WARNING] Treasury changes are logged in the faction's transaction history. Admin modifications are recorded with the admin's name for accountability.

>[!NOTE] All economy admin commands work even when the economy module is disabled in config. The data is stored regardless of module status.
