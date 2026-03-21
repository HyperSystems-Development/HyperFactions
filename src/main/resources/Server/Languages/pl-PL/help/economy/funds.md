---
id: economy_funds
commands: deposit, withdraw
---
# Zarzadzanie funduszami

Czlonkowie frakcji wspolpracuja, aby utrzymac skarbiec zasilony poprzez wplaty, wyplaty i przelewy.

## Wplacanie

Kazdy czlonek moze wplacic osobiste fundusze do skarbca frakcji.

`/f deposit <amount>`
Wplac ze swojego osobistego salda do skarbca.

## Wyplacanie

Oficerowie i Lider moga wyplacac fundusze z powrotem na swoje osobiste saldo.

`/f withdraw <amount>`
Wyplac ze skarbca na swoje saldo. (Oficer+)

## Przelewanie

Oficerowie moga przelewac fundusze bezposrednio miedzy skarbcami frakcji w ramach umow handlowych lub dyplomacji.

`/f money transfer <faction> <amount>`
Wyslij fundusze do skarbca innej frakcji. (Oficer+)

---

## Oplaty

| Transakcja | Oplata |
|------------|-----|
| Wplata | 0% |
| Wyplata | 0% |
| Przelew | 0% |

>[!INFO] Stawki oplat sa konfigurowalne przez serwer i moga roznic sie od domyslnych wartosci pokazanych powyzej.

>[!TIP] Wszystkie transakcje sa rejestrowane. Uzywaj /f money log do przegladania ostatniej aktywnosci.
