# HyperFactions Announcement System

> **Version**: 0.13.0 | **Package**: `com.hyperfactions.manager`

The announcement system broadcasts significant faction events to all online players. Events can be individually toggled in the configuration, and each event has a configurable color.

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
  },
  "colors": {
    "factionCreated": "#55FF55",
    "factionDisbanded": "#FF5555",
    "leadershipTransfer": "#FFAA00",
    "overclaim": "#FF5555",
    "warDeclared": "#FF5555",
    "allianceFormed": "#55FF55",
    "allianceBroken": "#FFAA00"
  },
  "territoryNotifications": {
    "enabled": true,
    "wilderness": {
      "onLeaveZone": {
        "enabled": true,
        "upper": "",
        "lower": "Wilderness"
      },
      "onLeaveClaim": {
        "enabled": true,
        "upper": "",
        "lower": "Wilderness"
      }
    }
  }
}
```

Set `enabled: false` to disable all announcements globally. Individual events can be toggled independently. Colors are hex strings (e.g. `#55FF55`) and can be customized per event.

---

## Event Types

| Event | Default Color | Message Format |
|-------|---------------|----------------|
| **factionCreated** | `#55FF55` (green) | `{0} has founded the faction {1}!` |
| **factionDisbanded** | `#FF5555` (red) | `The faction {0} has been disbanded!` |
| **leadershipTransfer** | `#FFAA00` (gold) | `{0} is now the leader of {1}!` |
| **overclaim** | `#FF5555` (red) | `{0} has overclaimed territory from {1}!` |
| **warDeclared** | `#FF5555` (red) | `{0} has declared war on {1}!` |
| **allianceFormed** | `#55FF55` (green) | `{0} and {1} are now allies!` |
| **allianceBroken** | `#FFAA00` (gold) | `{0} and {1} are no longer allies!` |

Messages use i18n keys with `{0}`, `{1}` placeholders for player/faction names, resolved per-player at broadcast time.

---

### Territory Notifications

The `territoryNotifications` config block controls HUD notifications shown when players move between territory zones.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | boolean | `true` | Master toggle for all territory notifications |
| `wilderness.onLeaveZone.enabled` | boolean | `true` | Show notification when leaving a faction zone into wilderness |
| `wilderness.onLeaveZone.upper` | string | `""` | Upper line text for the zone-exit notification |
| `wilderness.onLeaveZone.lower` | string | `"Wilderness"` | Lower line text for the zone-exit notification |
| `wilderness.onLeaveClaim.enabled` | boolean | `true` | Show notification when leaving a claimed chunk into wilderness |
| `wilderness.onLeaveClaim.upper` | string | `""` | Upper line text for the claim-exit notification |
| `wilderness.onLeaveClaim.lower` | string | `"Wilderness"` | Lower line text for the claim-exit notification |

---

## Broadcast Mechanism

- Uses `Supplier<Collection<PlayerRef>>` for online player access
- Messages are built with the configured prefix from `config.json` (messages section)
- Format: `[HyperFactions] <colored message text>`
- Each event uses its configured color from the `colors` section
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
