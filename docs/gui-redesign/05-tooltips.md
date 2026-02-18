# Phase 5: Tooltips

> Add `TooltipText` + `$C.@DefaultTextTooltipStyle` on interactive elements throughout the UI

## Overview

HyperFactions currently has no tooltips anywhere in the UI. Phase 5 adds helpful hover text to buttons, stat cards, permission checkboxes, navigation items, and other interactive elements. Tooltips use Hytale's native tooltip system.

---

## Syntax

### Static tooltips (in .ui files)

```
TextButton #HomeBtn {
  Text: "Home";
  Style: $C.@SecondaryTextButtonStyle;
  TooltipText: "Teleport to faction home";
  TextTooltipStyle: $C.@DefaultTextTooltipStyle;
}
```

### Dynamic tooltips (from Java)

```java
cmd.set("#PowerStat.TooltipText", "Current: " + current + " / Max: " + max);
cmd.set("#PowerStat.TextTooltipStyle", Value.ref("../../Common.ui", "DefaultTextTooltipStyle"));
```

**Note**: Both `TooltipText` and `TextTooltipStyle` must be set. Without the style, the tooltip may not render.

---

## Tooltip Inventory

### Dashboard stat cards

| Element | TooltipText | Source |
|---------|------------|--------|
| Power card | `"Current faction power / Maximum power"` | Dynamic (Java) — includes actual values |
| Members card | `"Online members / Total members"` | Dynamic |
| Territory card | `"Claimed chunks / Maximum claimable"` | Dynamic |
| Treasury card | `"Faction balance (if economy enabled)"` | Dynamic |
| Allies card | `"Number of allied factions"` | Static |
| Age card | `"Days since faction was created"` | Dynamic |

### Dashboard quick action buttons

| Button | TooltipText |
|--------|------------|
| Home | `"Teleport to faction home"` |
| Map | `"View territory map"` |
| Chat | `"Open faction chat"` |
| Invite | `"Invite a player to your faction"` |
| Leave | `"Leave this faction"` |
| Settings | `"Faction settings"` |

### Navigation bar items

| Tab | TooltipText |
|-----|------------|
| Dashboard | `"Faction overview and stats"` |
| Members | `"View and manage members"` |
| Relations | `"Diplomatic relations with other factions"` |
| Territory | `"Territory map and claims"` |
| Settings | `"Faction settings and permissions"` |
| Economy | `"Treasury and transactions"` |
| Help | `"Help and command reference"` |

### Settings page

| Element | TooltipText |
|---------|------------|
| Faction name | `"Click to rename your faction"` |
| Faction tag | `"Click to change faction tag"` |
| Description | `"Click to edit faction description"` |
| Open/Closed toggle | `"Open: anyone can join. Closed: invitation only"` |
| Friendly fire toggle | `"Allow faction members to damage each other"` |
| Each permission checkbox | Context-specific, e.g. `"Allow outsiders to break blocks in your territory"` |
| Server-locked options | `"This setting is controlled by the server administrator"` |
| Disband button | `"Permanently delete this faction. This cannot be undone."` |

### Member list

| Element | TooltipText |
|---------|------------|
| Role badge (Leader) | `"Faction leader — full control over the faction"` |
| Role badge (Officer) | `"Officer — can invite, kick members, and claim territory"` |
| Role badge (Member) | `"Member — standard faction member"` |
| Online indicator | `"Currently online"` / `"Last seen: X ago"` (dynamic) |
| Promote button | `"Promote to Officer"` |
| Demote button | `"Demote to Member"` |
| Kick button | `"Remove from faction"` |
| Transfer button | `"Transfer leadership to this player"` |

### Relations page

| Element | TooltipText |
|---------|------------|
| Ally badge | `"Allied faction — mutual protection pact"` |
| Enemy badge | `"Enemy faction — hostile relations"` |
| Neutral badge | `"Neutral — no special relationship"` |
| Set Ally button | `"Request alliance with this faction"` |
| Set Enemy button | `"Declare this faction as enemy"` |
| Set Neutral button | `"Reset to neutral relations"` |

### Territory/Map

| Element | TooltipText |
|---------|------------|
| Zoom in | `"Zoom in"` |
| Zoom out | `"Zoom out"` |
| Mode toggle | `"Switch between flat and terrain view"` |
| Legend: own territory | `"Your faction's claimed territory"` |
| Legend: ally territory | `"Territory claimed by allied factions"` |
| Legend: enemy territory | `"Territory claimed by enemy factions"` |
| Legend: neutral territory | `"Territory claimed by other factions"` |
| Legend: wilderness | `"Unclaimed territory"` |

### Confirmation modals

| Element | TooltipText |
|---------|------------|
| Disband confirm button | `"This action cannot be undone"` |
| Leave confirm button | `"You will lose access to faction resources"` |
| Transfer confirm button | `"You will become a regular member"` |

### New player pages

| Element | TooltipText |
|---------|------------|
| Create button | `"Start a new faction"` |
| Browse button | `"Find factions to join"` |
| Invites button | `"View pending invitations"` |
| Join request button | `"Send a request to join this faction"` |
| Accept invite button | `"Join this faction"` |
| Decline invite button | `"Reject this invitation"` |

### Admin pages

| Element | TooltipText |
|---------|------------|
| Zone flag checkboxes | Description of what each flag controls (from `ZoneFlags.getDescription()`) |
| Config fields | Explanation of each config option |
| Force disband | `"Forcefully disband this faction as administrator"` |
| Backup now | `"Create a backup of all faction data"` |
| Restore backup | `"Restore faction data from this backup"` |

---

## Java Implementation Pattern

For dynamic tooltips, add tooltip setting in the page's `build()` or `populateData()` method:

```java
// Static tooltip (set once)
cmd.set("#HomeBtn.TooltipText", "Teleport to faction home");
cmd.set("#HomeBtn.TextTooltipStyle", Value.ref("../../Common.ui", "DefaultTextTooltipStyle"));

// Dynamic tooltip (includes runtime data)
String powerTip = String.format("Power: %d / %d (%.0f%%)", current, max, percentage);
cmd.set("#PowerCard.TooltipText", powerTip);
cmd.set("#PowerCard.TextTooltipStyle", Value.ref("../../Common.ui", "DefaultTextTooltipStyle"));
```

### Helper method (optional)

To avoid repeating the style reference, consider a utility method in the page base class:

```java
protected void setTooltip(CustomUICommand cmd, String elementId, String text) {
    cmd.set(elementId + ".TooltipText", text);
    cmd.set(elementId + ".TextTooltipStyle", Value.ref("../../Common.ui", "DefaultTextTooltipStyle"));
}
```

---

## Verification

1. Build and deploy
2. Hover over every button, stat card, badge, and interactive element
3. Verify tooltip text appears after brief hover delay
4. Verify tooltip disappears when moving away
5. Verify dynamic tooltips show correct runtime values
6. Check tooltips don't obscure critical UI elements
7. Verify no performance impact from tooltip rendering
