---
id: admin_disbanding
---
# Wymuszone rozwiazanie

Administratorzy moga wymusic rozwiazanie dowolnej frakcji, niezaleznie od woli lidera.

## Komenda

`/f admin disband <faction>`
Wymusza rozwiazanie nazwanej frakcji. Przed wykonaniem akcji pojawi sie monit o potwierdzenie.

**Uprawnienie**: `hyperfactions.admin.disband`

>[!WARNING] Rozwiazanie frakcji jest **nieodwracalne**. Wszystkie zajecia sa zwalniane, wszyscy czlonkowie sa usuwani, a frakcja przestaje istniec. Najpierw utworz kopie zapasowa.

## Konsekwencje

Gdy frakcja zostaje rozwiazana:

| Efekt | Opis |
|--------|-------------|
| **Zajecia** | Cale terytorium jest natychmiast zwalniane |
| **Czlonkowie** | Wszyscy gracze sa usuwani ze skladu |
| **Relacje** | Wszystkie sojusze i wrogosci sa czyszczone |
| **Skarbiec** | Obslugiwany zgodnie z ustawieniami konfiguracji ekonomii |
| **Baza** | Baza frakcji jest usuwana |
| **Czat** | Historia czatu frakcji jest usuwana |

## Najlepsze praktyki

1. Zawsze wpisz `/f admin backup create` przed rozwiazaniem
2. Powiadom czlonkow frakcji, gdy to mozliwe
3. Udokumentuj powod dla rejestrow serwera
4. Sprawdz `/f admin info <faction>`, aby przejrzec przed podjeciem akcji

>[!TIP] Jesli problem dotyczy konkretnego czlonka, rozwaz uzycie GUI administracyjnego frakcji do przekazania przywodztwa zamiast rozwiazywania calej frakcji.
