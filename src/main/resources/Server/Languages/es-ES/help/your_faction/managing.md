---
id: faction_managing
commands: invite, kick, promote, demote, transfer
---
# Gestionar Miembros

Los Oficiales y Lideres comparten la responsabilidad de gestionar la lista de miembros de la faccion. Aqui estan los comandos clave y quien puede usarlos.

---

## Comandos

| Comando | Que Hace | Rol Requerido |
|---------|----------|---------------|
| `/f invite <player>` | Envia una invitacion (expira en 5 min) | Oficial+ |
| `/f kick <player>` | Remueve a un miembro de la faccion | Oficial+ (ver nota) |
| `/f promote <player>` | Promueve un Miembro a Oficial | Solo Lider |
| `/f demote <player>` | Degrada un Oficial a Miembro | Solo Lider |
| `/f transfer <player>` | Transfiere la propiedad de la faccion | Solo Lider |

>[!NOTE] Los Oficiales solo pueden expulsar **Miembros**. Para remover a otro Oficial, el Lider debe degradarlo primero o expulsarlo directamente.

---

## Invitaciones

- Las invitaciones expiran despues de **5 minutos** si no son aceptadas
- El jugador invitado las ve en su pestana de Invitaciones cuando abre `/f`
- No hay limite de cuantas invitaciones puedes enviar a la vez
- Tu faccion puede tener hasta **50 miembros** en total

## Promociones y Degradaciones

- Solo el **Lider** puede promover o degradar
- `/f promote <player>` eleva a un Miembro a Oficial
- `/f demote <player>` baja a un Oficial de vuelta a Miembro

## Transferir Liderazgo

>[!WARNING] Transferir el liderazgo es **irreversible**. Seras degradado a Oficial y el jugador objetivo se convierte en el nuevo Lider. Asegurate de confiar completamente en el.

`/f transfer <player>`

El objetivo debe ser un miembro actual de tu faccion.
