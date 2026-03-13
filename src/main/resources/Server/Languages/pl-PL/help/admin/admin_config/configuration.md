---
id: admin_configuration
---
# System konfiguracji

HyperFactions używa modularnego systemu konfiguracji JSON z 11 plikami konfiguracyjnymi.

## Komendy konfiguracji administracyjnej

| Komenda | Opis |
|---------|-------------|
| `/f admin config` | Otwórz wizualny edytor konfiguracji GUI |
| `/f admin reload` | Przeładuj wszystkie pliki konfiguracyjne z dysku |
| `/f admin sync` | Synchronizuj dane frakcji do magazynu |

## Pliki konfiguracyjne

| Plik | Zawartość |
|------|----------|
| `factions.json` | Role, moc, zajęcia, walka, relacje |
| `server.json` | Teleportacja, auto-zapis, wiadomości, GUI, uprawnienia |
| `economy.json` | Skarbiec, utrzymanie, ustawienia transakcji |
| `backup.json` | Rotacja i retencja kopii zapasowych |
| `chat.json` | Formatowanie czatu frakcyjnego i sojuszniczego |
| `debug.json` | Kategorie logowania debugowego |
| `faction-permissions.json` | Domyślne uprawnienia dla ról |
| `announcements.json` | Transmisja wydarzeń i powiadomienia terytorialne |
| `gravestones.json` | Ustawienia integracji nagrobków |
| `worldmap.json` | Tryby odświeżania mapy świata |
| `worlds.json` | Nadpisania zachowań dla poszczególnych światów |

>[!TIP] GUI konfiguracji zapewnia wizualny edytor z opisami dla każdego ustawienia. Zmiany są zapisywane natychmiast, ale niektóre wymagają `/f admin reload`, aby w pełni zadziałać.

## Lokalizacja konfiguracji

Wszystkie pliki są przechowywane w:
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Ręczne edycje JSON wymagają `/f admin reload`, aby zostały zastosowane. Niepoprawny JSON spowoduje pominięcie pliku z ostrzeżeniem w logu serwera.

>[!NOTE] Wersja konfiguracji jest śledzona w `server.json`. Plugin automatycznie migruje starsze konfiguracje przy uruchomieniu.
