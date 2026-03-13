---
id: economy_funds
commands: deposit, withdraw
---
# Zarządzanie funduszami

Członkowie frakcji współpracują, aby utrzymać skarbiec zasilony poprzez wpłaty, wypłaty i przelewy.

## Wpłacanie

Każdy członek może wpłacić osobiste fundusze do skarbca frakcji.

`/f deposit <amount>`
Wpłać ze swojego osobistego salda do skarbca.

## Wypłacanie

Oficerowie i Lider mogą wypłacać fundusze z powrotem na swoje osobiste saldo.

`/f withdraw <amount>`
Wypłać ze skarbca na swoje saldo. (Oficer+)

## Przelewanie

Oficerowie mogą przelewać fundusze bezpośrednio między skarbcami frakcji w ramach umów handlowych lub dyplomacji.

`/f money transfer <faction> <amount>`
Wyślij fundusze do skarbca innej frakcji. (Oficer+)

---

## Opłaty

| Transakcja | Opłata |
|------------|-----|
| Wpłata | 0% |
| Wypłata | 0% |
| Przelew | 0% |

>[!INFO] Stawki opłat są konfigurowalne przez serwer i mogą różnić się od domyślnych wartości pokazanych powyżej.

>[!TIP] Wszystkie transakcje są rejestrowane. Używaj /f money log do przeglądania ostatniej aktywności.
