---
id: admin_configuration
---
# System konfiguracji

HyperFactions uzywa modularnego systemu konfiguracji JSON z 11 plikami konfiguracyjnymi.

## Komendy konfiguracji administracyjnej

| Komenda | Opis |
|---------|-------------|
| `/f admin config` | Otworz wizualny edytor konfiguracji GUI |
| `/f admin reload` | Przeladuj wszystkie pliki konfiguracyjne z dysku |
| `/f admin sync` | Synchronizuj dane frakcji do magazynu |

## Pliki konfiguracyjne

| Plik | Zawartosc |
|------|----------|
| `factions.json` | Role, moc, zajecia, walka, relacje |
| `server.json` | Teleportacja, auto-zapis, wiadomosci, GUI, uprawnienia |
| `economy.json` | Skarbiec, utrzymanie, ustawienia transakcji |
| `backup.json` | Rotacja i retencja kopii zapasowych |
| `chat.json` | Formatowanie czatu frakcyjnego i sojuszniczego |
| `debug.json` | Kategorie logowania debugowego |
| `faction-permissions.json` | Domyslne uprawnienia dla rol |
| `announcements.json` | Transmisja wydarzen i powiadomienia terytorialne |
| `gravestones.json` | Ustawienia integracji nagrobkow |
| `worldmap.json` | Tryby odswiezania mapy swiata |
| `worlds.json` | Nadpisania zachowan dla poszczegolnych swiatow |

>[!TIP] GUI konfiguracji zapewnia wizualny edytor z opisami dla kazdego ustawienia. Zmiany sa zapisywane natychmiast, ale niektore wymagaja `/f admin reload`, aby w pelni zadzialac.

## Lokalizacja konfiguracji

Wszystkie pliki sa przechowywane w:
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Reczne edycje JSON wymagaja `/f admin reload`, aby zostaly zastosowane. Niepoprawny JSON spowoduje pominiecie pliku z ostrzezeniem w logu serwera.

>[!NOTE] Wersja konfiguracji jest sledzona w `server.json`. Plugin automatycznie migruje starsze konfiguracje przy uruchomieniu.
