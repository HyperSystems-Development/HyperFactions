---
id: admin_backups
---
# System kopii zapasowych

HyperFactions zawiera automatyczne i reczne kopie zapasowe z rotacja GFS (Grandfather-Father-Son).

## Komendy kopii zapasowych

| Komenda | Opis |
|---------|-------------|
| `/f admin backup create` | Utworz reczna kopie zapasowa teraz |
| `/f admin backup list` | Lista wszystkich dostepnych kopii zapasowych |
| `/f admin backup restore <name>` | Przywroc z kopii zapasowej |
| `/f admin backup delete <name>` | Usun konkretna kopie zapasowa |

**Uprawnienie**: `hyperfactions.admin.backup`

## Domyslna rotacja GFS

| Typ | Retencja | Opis |
|------|-----------|-------------|
| Godzinowe | 24 | Ostatnie 24 godzinne migawki |
| Dzienne | 7 | Ostatnie 7 dziennych migawek |
| Tygodniowe | 4 | Ostatnie 4 tygodniowe migawki |
| Reczne | 10 | Recznie utworzone kopie zapasowe |
| Przy wylaczeniu | 5 | Tworzone przy zatrzymaniu serwera |

>[!INFO] Kopie zapasowe przy wylaczeniu sa domyslnie wlaczone (`onShutdown=true`). Przechwytuja najnowszy stan przed zatrzymaniem serwera.

## Zawartosc kopii zapasowej

Kazde archiwum ZIP kopii zapasowej zawiera:
- Wszystkie pliki danych frakcji
- Dane mocy graczy
- Definicje stref
- Historie czatu i dane ekonomii
- Dane zaproszen i prosb o dolaczenie
- Pliki konfiguracyjne

>[!WARNING] **Przywracanie kopii zapasowej jest destrukcyjne.** Zastepuje wszystkie aktualne dane zawartoscia kopii zapasowej. Wszelkie zmiany dokonane po utworzeniu kopii zapasowej zostana utracone. Zawsze tworz swieza kopie zapasowa przed przywracaniem.

## Najlepsze praktyki

1. Utworz reczna kopie zapasowa przed waznymi akcjami administracyjnymi
2. Przejrzyj retencje kopii zapasowych w `backup.json`
3. Przetestuj przywracanie na serwerze testowym
4. Utrzymuj kopie zapasowe przy wylaczeniu wlaczone dla odzyskiwania po awariach
