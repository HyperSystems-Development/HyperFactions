---
id: admin_power_overrides
---
# Sobrescrituras de Poder

Comandos especiales de poder que cambian como funciona
el poder para jugadores o facciones especificos.

## Comandos de Sobrescritura

| Comando | Descripcion |
|---------|-------------|
| `/f admin power setmax <player> <amount>` | Establecer limite maximo de poder personalizado |
| `/f admin power noloss <player>` | Alternar inmunidad a penalizacion de poder por muerte |
| `/f admin power nodecay <player>` | Alternar inmunidad a deterioro de poder por desconexion |
| `/f admin power info <player>` | Ver todas las sobrescrituras y detalles de poder |

## Poder Maximo Personalizado

`/f admin power setmax <player> <amount>`
Establece un limite maximo de poder personal para el
jugador, sobrescribiendo el valor predeterminado del servidor.

>[!INFO] Establecer un maximo personalizado **no** cambia el poder actual. Solo cambia el techo. El jugador aun debe ganar poder hasta el nuevo limite.

## Modo Sin Perdida

`/f admin power noloss <player>`
Alterna la inmunidad a perdida de poder por muerte.
Cuando esta habilitado, el jugador **no** perdera poder
al morir.

Util para:
- Periodos de proteccion para nuevos jugadores
- Participantes de eventos
- Miembros del staff

## Modo Sin Deterioro

`/f admin power nodecay <player>`
Alterna la inmunidad al deterioro de poder por desconexion.
Cuando esta habilitado, el poder del jugador **no**
disminuira mientras este desconectado.

Util para:
- Jugadores en ausencia prolongada
- Miembros VIP
- Proteccion estacional

## Informacion de Poder

`/f admin power info <player>`
Muestra un desglose completo:

- Poder actual y poder maximo
- Sobrescrituras activas (sin perdida, sin deterioro, maximo personalizado)
- Ultima muerte y poder perdido
- Porcentaje de contribucion a la faccion

>[!TIP] Todas las sobrescrituras de poder persisten entre reinicios del servidor y se almacenan en el archivo de datos del jugador.
