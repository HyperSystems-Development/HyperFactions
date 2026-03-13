---
id: admin_backups
---
# System kopii zapasowych

HyperFactions zawiera automatyczne i ręczne kopie zapasowe z rotacją GFS (Grandfather-Father-Son).

## Komendy kopii zapasowych

| Komenda | Opis |
|---------|-------------|
| `/f admin backup create` | Utwórz ręczną kopię zapasową teraz |
| `/f admin backup list` | Lista wszystkich dostępnych kopii zapasowych |
| `/f admin backup restore <name>` | Przywróć z kopii zapasowej |
| `/f admin backup delete <name>` | Usuń konkretną kopię zapasową |

**Uprawnienie**: `hyperfactions.admin.backup`

## Domyślna rotacja GFS

| Typ | Retencja | Opis |
|------|-----------|-------------|
| Godzinowe | 24 | Ostatnie 24 godzinne migawki |
| Dzienne | 7 | Ostatnie 7 dziennych migawek |
| Tygodniowe | 4 | Ostatnie 4 tygodniowe migawki |
| Ręczne | 10 | Ręcznie utworzone kopie zapasowe |
| Przy wyłączeniu | 5 | Tworzone przy zatrzymaniu serwera |

>[!INFO] Kopie zapasowe przy wyłączeniu są domyślnie włączone (`onShutdown=true`). Przechwytują najnowszy stan przed zatrzymaniem serwera.

## Zawartość kopii zapasowej

Każde archiwum ZIP kopii zapasowej zawiera:
- Wszystkie pliki danych frakcji
- Dane mocy graczy
- Definicje stref
- Historię czatu i dane ekonomii
- Dane zaproszeń i próśb o dołączenie
- Pliki konfiguracyjne

>[!WARNING] **Przywracanie kopii zapasowej jest destrukcyjne.** Zastępuje wszystkie aktualne dane zawartością kopii zapasowej. Wszelkie zmiany dokonane po utworzeniu kopii zapasowej zostaną utracone. Zawsze twórz świeżą kopię zapasową przed przywracaniem.

## Najlepsze praktyki

1. Utwórz ręczną kopię zapasową przed ważnymi akcjami administracyjnymi
2. Przejrzyj retencję kopii zapasowych w `backup.json`
3. Przetestuj przywracanie na serwerze testowym
4. Utrzymuj kopie zapasowe przy wyłączeniu włączone dla odzyskiwania po awariach
