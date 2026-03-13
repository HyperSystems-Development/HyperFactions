---
id: admin_disbanding
---
# Wymuszone rozwiązanie

Administratorzy mogą wymusić rozwiązanie dowolnej frakcji, niezależnie od woli lidera.

## Komenda

`/f admin disband <faction>`
Wymusza rozwiązanie nazwanej frakcji. Przed wykonaniem akcji pojawi się monit o potwierdzenie.

**Uprawnienie**: `hyperfactions.admin.disband`

>[!WARNING] Rozwiązanie frakcji jest **nieodwracalne**. Wszystkie zajęcia są zwalniane, wszyscy członkowie są usuwani, a frakcja przestaje istnieć. Najpierw utwórz kopię zapasową.

## Konsekwencje

Gdy frakcja zostaje rozwiązana:

| Efekt | Opis |
|--------|-------------|
| **Zajęcia** | Całe terytorium jest natychmiast zwalniane |
| **Członkowie** | Wszyscy gracze są usuwani ze składu |
| **Relacje** | Wszystkie sojusze i wrogości są czyszczone |
| **Skarbiec** | Obsługiwany zgodnie z ustawieniami konfiguracji ekonomii |
| **Baza** | Baza frakcji jest usuwana |
| **Czat** | Historia czatu frakcji jest usuwana |

## Najlepsze praktyki

1. Zawsze wpisz `/f admin backup create` przed rozwiązaniem
2. Powiadom członków frakcji, gdy to możliwe
3. Udokumentuj powód dla rejestrów serwera
4. Sprawdź `/f admin info <faction>`, aby przejrzeć przed podjęciem akcji

>[!TIP] Jeśli problem dotyczy konkretnego członka, rozważ użycie GUI administracyjnego frakcji do przekazania przywództwa zamiast rozwiązywania całej frakcji.
