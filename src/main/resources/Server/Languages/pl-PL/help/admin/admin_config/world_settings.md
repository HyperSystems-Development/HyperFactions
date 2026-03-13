---
id: admin_world_settings
---
# Ustawienia per-świat

HyperFactions obsługuje konfigurację per-świat dla zajmowania, PvP i zachowania ochrony.

## Komendy światów

| Komenda | Opis |
|---------|-------------|
| `/f admin world list` | Lista wszystkich nadpisań światów |
| `/f admin world info <world>` | Pokaż ustawienia dla świata |
| `/f admin world set <world> <key> <value>` | Ustaw ustawienie |
| `/f admin world reset <world>` | Resetuj świat do domyślnych |

## Dostępne ustawienia

| Ustawienie | Typ | Opis |
|---------|------|-------------|
| claiming_enabled | boolean | Zezwól na zajęcia frakcji w tym świecie |
| pvp_enabled | boolean | Zezwól na walkę PvP w tym świecie |
| power_loss | boolean | Zastosuj utratę mocy przy śmierci |
| build_protection | boolean | Wymuś ochronę budowania na zajęciach |
| explosion_protection | boolean | Chroń zajęcia przed eksplozjami |

## Biała lista / czarna lista światów

Kontroluj, które światy pozwalają na funkcje frakcji przez plik konfiguracyjny `worlds.json`:

- **Tryb białej listy**: Tylko wymienione światy pozwalają na zajmowanie
- **Tryb czarnej listy**: Wszystkie światy pozwalają na zajmowanie oprócz wymienionych

>[!INFO] Ustawienia światów są przechowywane w `worlds.json` i nadpisują globalne domyślne z `factions.json`.

## Przykłady

- `/f admin world set survival claiming_enabled true`
- `/f admin world set creative claiming_enabled false`
- `/f admin world set pvp_arena pvp_enabled true`
- `/f admin world reset lobby` -- przywróć wszystkie domyślne

>[!TIP] Wyłącz zajmowanie w światach kreatywnych lub lobby, aby skupić system frakcji na rozgrywce survivalowej.

>[!NOTE] Ustawienia per-świat mają priorytet nad globalną konfiguracją, ale są nadpisywane przez flagi stref w danym świecie.
