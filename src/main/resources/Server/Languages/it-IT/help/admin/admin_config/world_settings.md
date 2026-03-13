---
id: admin_world_settings
---
# Impostazioni Per-Mondo

HyperFactions supporta la configurazione per-mondo per claim, PvP e comportamento di protezione.

## Comandi Mondo

| Comando | Descrizione |
|---------|-------------|
| `/f admin world list` | Elenca tutti gli override per mondo |
| `/f admin world info <world>` | Mostra le impostazioni per un mondo |
| `/f admin world set <world> <key> <value>` | Imposta un'impostazione |
| `/f admin world reset <world>` | Ripristina il mondo ai valori predefiniti |

## Impostazioni Disponibili

| Impostazione | Tipo | Descrizione |
|--------------|------|-------------|
| claiming_enabled | boolean | Permetti claim delle fazioni in questo mondo |
| pvp_enabled | boolean | Permetti combattimento PvP in questo mondo |
| power_loss | boolean | Applica perdita di potere alla morte |
| build_protection | boolean | Applica protezione costruzione nei claim |
| explosion_protection | boolean | Proteggi i claim dalle esplosioni |

## Whitelist / Blacklist Mondi

Controlla quali mondi permettono le funzionalita' delle fazioni tramite il file di configurazione `worlds.json`:

- **Modalita' whitelist**: Solo i mondi elencati permettono il claim
- **Modalita' blacklist**: Tutti i mondi permettono il claim tranne quelli elencati

>[!INFO] Le impostazioni per mondo sono salvate in `worlds.json` e sovrascrivono i valori predefiniti globali da `factions.json`.

## Esempi

- `/f admin world set survival claiming_enabled true`
- `/f admin world set creative claiming_enabled false`
- `/f admin world set pvp_arena pvp_enabled true`
- `/f admin world reset lobby` -- ripristina tutti i valori predefiniti

>[!TIP] Disabilita il claim nei mondi creativi o lobby per mantenere il sistema fazioni focalizzato sul gameplay survival.

>[!NOTE] Le impostazioni per-mondo hanno priorita' sulla configurazione globale ma sono sovrascritte dai flag delle zone all'interno di quel mondo.
