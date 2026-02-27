# HyperFactions Announcement System

> **Version**: 0.10.0 | **Package**: `com.hyperfactions.manager`

The announcement system broadcasts significant faction events to all online players. Events can be individually toggled in the configuration.

---

## Configuration

**File**: `config/announcements.json`

```json
{
  "enabled": true,
  "events": {
    "factionCreated": true,
    "factionDisbanded": true,
    "leadershipTransfer": true,
    "overclaim": true,
    "warDeclared": true,
    "allianceFormed": true,
    "allianceBroken": true
  }
}
```

Set `enabled: false` to disable all announcements globally. Individual events can be toggled independently.

---

## Event Types

| Event | Color | Message Format |
|-------|-------|----------------|
| **factionCreated** | `#55FF55` (green) | `{player} has founded the faction {name}!` |
| **factionDisbanded** | `#FF5555` (red) | `The faction {name} has been disbanded!` |
| **leadershipTransfer** | `#FFAA00` (gold) | `{newLeader} is now the leader of {name}!` |
| **overclaim** | `#FF5555` (red) | `{attacker} has overclaimed territory from {defender}!` |
| **warDeclared** | `#FF5555` (red) | `{declarer} has declared war on {target}!` |
| **allianceFormed** | `#55FF55` (green) | `{faction1} and {faction2} are now allies!` |
| **allianceBroken** | `#FFAA00` (gold) | `{faction1} and {faction2} are no longer allies!` |

---

## Broadcast Mechanism

- Uses `Supplier<Collection<PlayerRef>>` for online player access
- Messages are built with the configured prefix from `config.json` (messages section)
- Format: `[HyperFactions] <colored message text>`
- Iterates all online players and sends directly

---

## Admin Exclusions

Admin-initiated actions intentionally do not trigger announcements:

- **Force disband** (`/f admin disband`) — no `factionDisbanded` announcement
- **Admin set relation** (`/f admin setrelation`) — no alliance/war announcements

This prevents confusion when admins perform maintenance operations.

---

## Manager API

```java
AnnouncementManager announcements = hyperFactions.getAnnouncementManager();

announcements.announceFactionCreated("Warriors", "Steve");
announcements.announceFactionDisbanded("Warriors");
announcements.announceLeadershipTransfer("Warriors", "Steve", "Alex");
announcements.announceOverclaim("Raiders", "Defenders");
announcements.announceWarDeclared("Raiders", "Defenders");
announcements.announceAllianceFormed("Warriors", "Builders");
announcements.announceAllianceBroken("Warriors", "Builders");
```

Each method checks both the global `enabled` flag and the per-event toggle before broadcasting. Exceptions in the broadcast loop are caught and logged without propagating.
