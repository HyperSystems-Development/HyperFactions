---
id: admin_world_settings
---
# Ustawienia per-swiat

HyperFactions obsluguje konfiguracje per-swiat dla zajmowania, PvP i zachowania ochrony.

## Komendy swiatow

| Komenda | Opis |
|---------|-------------|
| `/f admin world list` | Lista wszystkich nadpisan swiatow |
| `/f admin world info <world>` | Pokaz ustawienia dla swiata |
| `/f admin world set <world> <key> <value>` | Ustaw ustawienie |
| `/f admin world reset <world>` | Resetuj swiat do domyslnych |

## Dostepne ustawienia

| Ustawienie | Typ | Opis |
|---------|------|-------------|
| claiming_enabled | boolean | Zezwol na zajecia frakcji w tym swiecie |
| pvp_enabled | boolean | Zezwol na walke PvP w tym swiecie |
| power_loss | boolean | Zastosuj utrate mocy przy smierci |
| build_protection | boolean | Wymus ochrone budowania na zajeciach |
| explosion_protection | boolean | Chron zajecia przed eksplozjami |

## Biala lista / czarna lista swiatow

Kontroluj, ktore swiaty pozwalaja na funkcje frakcji przez plik konfiguracyjny `worlds.json`:

- **Tryb bialej listy**: Tylko wymienione swiaty pozwalaja na zajmowanie
- **Tryb czarnej listy**: Wszystkie swiaty pozwalaja na zajmowanie oprocz wymienionych

>[!INFO] Ustawienia swiatow sa przechowywane w `worlds.json` i nadpisuja globalne domyslne z `factions.json`.

## Przyklady

- `/f admin world set survival claiming_enabled true`
- `/f admin world set creative claiming_enabled false`
- `/f admin world set pvp_arena pvp_enabled true`
- `/f admin world reset lobby` -- przywroc wszystkie domyslne

>[!TIP] Wylacz zajmowanie w swiatach kreatywnych lub lobby, aby skupic system frakcji na rozgrywce survivalowej.

>[!NOTE] Ustawienia per-swiat maja priorytet nad globalna konfiguracja, ale sa nadpisywane przez flagi stref w danym swiecie.
