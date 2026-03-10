---
id: admin_treasury_management
---
# Gestion de Tesoreria

Comandos de administracion para gestionar tesorerias de facciones. Requiere el permiso `hyperfactions.admin.economy`.

## Comandos de Tesoreria

| Comando | Descripcion |
|---------|-------------|
| `/f admin economy balance <faction>` | Ver saldo de tesoreria de la faccion |
| `/f admin economy set <faction> <amount>` | Establecer saldo exacto |
| `/f admin economy add <faction> <amount>` | Agregar fondos a la tesoreria |
| `/f admin economy take <faction> <amount>` | Retirar fondos de la tesoreria |
| `/f admin economy reset <faction>` | Restablecer tesoreria a cero |

## Ejemplos

- `/f admin economy balance Vikings` -- consultar saldo
- `/f admin economy set Vikings 5000` -- establecer en 5000
- `/f admin economy add Vikings 1000` -- depositar 1000
- `/f admin economy take Vikings 500` -- retirar 500
- `/f admin economy reset Vikings` -- poner saldo en cero

>[!TIP] Usa `/f admin info <faction>` para ver el panorama economico completo incluyendo historial de transacciones junto al saldo de tesoreria.

## Casos de Uso

| Escenario | Comando |
|----------|---------|
| Distribucion de premios de eventos | `economy add <faction> <prize>` |
| Penalizacion por violacion de reglas | `economy take <faction> <fine>` |
| Reinicio de economia tras limpieza | `economy reset <faction>` |
| Compensacion por errores | `economy add <faction> <amount>` |

>[!WARNING] Los cambios en la tesoreria se registran en el historial de transacciones de la faccion. Las modificaciones del administrador se registran con el nombre del administrador para responsabilidad.

>[!NOTE] Todos los comandos de economia de administracion funcionan incluso cuando el modulo de economia esta deshabilitado en la configuracion. Los datos se almacenan independientemente del estado del modulo.
