---
id: economy_commands
---
# Comandos de Economia

Referencia rapida para todos los comandos de economia de faccion.

| Comando | Descripcion | Rol |
|---------|-------------|-----|
| /f balance | Ver saldo de tesoreria | Cualquiera |
| /f deposit (amount) | Depositar en la tesoreria | Cualquiera |
| /f withdraw (amount) | Retirar de la tesoreria | Oficial+ |
| /f money transfer (faction) (amount) | Transferir a otra faccion | Oficial+ |
| /f money log [page] | Ver historial de transacciones | Oficial+ |

---

## Alias de Comandos

- `/f balance` tambien puede usarse como `/f bal`
- `/f deposit` y `/f withdraw` aceptan cantidades decimales

## Permisos

Todos los comandos de economia requieren nodos de permiso `hyperfactions.economy.*`. Retirar y transferir estan adicionalmente restringidos por rol de faccion (Oficial o superior).

>[!TIP] Usa /f money log para revisar depositos, retiros y transferencias recientes con marcas de tiempo.
