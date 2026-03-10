---
id: economy_funds
commands: deposit, withdraw
---
# Gestionar Fondos

Los miembros de la faccion trabajan juntos para mantener la
tesoreria financiada a traves de depositos, retiros y transferencias.

## Depositar

Cualquier miembro puede depositar fondos personales en la
tesoreria de la faccion.

`/f deposit <amount>`
Deposita de tu saldo personal a la tesoreria.

## Retirar

Los Oficiales y el Lider pueden retirar fondos de vuelta a
su saldo personal.

`/f withdraw <amount>`
Retira de la tesoreria a tu saldo. (Oficial+)

## Transferir

Los Oficiales pueden transferir fondos directamente entre
tesorerias de facciones para acuerdos comerciales o diplomacia.

`/f money transfer <faction> <amount>`
Envia fondos a la tesoreria de otra faccion. (Oficial+)

---

## Comisiones

| Transaccion | Comision |
|-------------|----------|
| Deposito | 0% |
| Retiro | 0% |
| Transferencia | 0% |

>[!INFO] Las tasas de comision son configurables por el servidor y pueden diferir de los valores predeterminados mostrados arriba.

>[!TIP] Todas las transacciones se registran. Usa /f money log para revisar la actividad reciente.
