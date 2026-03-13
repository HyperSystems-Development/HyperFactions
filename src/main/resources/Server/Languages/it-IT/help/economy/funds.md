---
id: economy_funds
commands: deposit, withdraw
---
# Gestione dei Fondi

I membri della fazione collaborano per mantenere il tesoro finanziato attraverso depositi, prelievi e trasferimenti.

## Depositare

Qualsiasi membro puo' depositare fondi personali nel tesoro della fazione.

`/f deposit <amount>`
Deposita dal tuo saldo personale nel tesoro.

## Prelevare

Gli Ufficiali e il Leader possono prelevare fondi riportandoli al proprio saldo personale.

`/f withdraw <amount>`
Preleva dal tesoro al tuo saldo. (Ufficiale+)

## Trasferire

Gli Ufficiali possono trasferire fondi direttamente tra i tesori delle fazioni per accordi commerciali o diplomazia.

`/f money transfer <faction> <amount>`
Invia fondi al tesoro di un'altra fazione. (Ufficiale+)

---

## Commissioni

| Transazione | Commissione |
|-------------|-------------|
| Deposito | 0% |
| Prelievo | 0% |
| Trasferimento | 0% |

>[!INFO] Le percentuali delle commissioni sono configurabili dal server e potrebbero differire dai valori predefiniti mostrati sopra.

>[!TIP] Tutte le transazioni vengono registrate. Usa /f money log per controllare l'attivita' recente.
