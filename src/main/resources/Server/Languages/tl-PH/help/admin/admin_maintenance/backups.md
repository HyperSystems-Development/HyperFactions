---
id: admin_backups
---
# Sistema ng Backup

Ang HyperFactions ay may kasamang automatic at manual backup na may GFS (Grandfather-Father-Son) rotation.

## Mga Backup Command

| Command | Paglalarawan |
|---------|-------------|
| `/f admin backup create` | Gumawa ng manual backup ngayon |
| `/f admin backup list` | Ilista ang lahat ng available na backup |
| `/f admin backup restore <name>` | Mag-restore mula sa backup |
| `/f admin backup delete <name>` | Mag-delete ng partikular na backup |

**Permission**: `hyperfactions.admin.backup`

## Mga Default ng GFS Rotation

| Uri | Retention | Paglalarawan |
|-----|-----------|-------------|
| Hourly | 24 | Huling 24 hourly snapshot |
| Daily | 7 | Huling 7 daily snapshot |
| Weekly | 4 | Huling 4 weekly snapshot |
| Manual | 10 | Mga mano-manong ginawang backup |
| Shutdown | 5 | Ginawa sa pag-stop ng server |

>[!INFO] Ang shutdown backup ay naka-enable bilang default (`onShutdown=true`). Kinukuha nito ang pinakabagong estado bago mag-stop ang server.

## Nilalaman ng Backup

Bawat backup ZIP archive ay naglalaman ng:
- Lahat ng faction data file
- Player power data
- Mga zone definition
- Chat history at economy data
- Mga invite at join request data
- Mga configuration file

>[!WARNING] **Ang pag-restore ng backup ay destructive.** Pinapalitan nito ang lahat ng kasalukuyang data ng nilalaman ng backup. Mawawala ang anumang pagbabago na ginawa pagkatapos gumawa ng backup. Palaging gumawa muna ng sariwang backup bago mag-restore.

## Mga Best Practice

1. Gumawa ng manual backup bago ang mga malalaking admin action
2. I-review ang backup retention sa `backup.json`
3. Subukan ang restore sa staging server muna
4. Panatilihing naka-enable ang shutdown backup para sa crash recovery
