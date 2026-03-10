---
id: diplomacy_relations
commands: relations
---
# Relaciones entre Facciones

Cada par de facciones tiene una relacion diplomatica que determina como interactuan. Hay tres estados: **Aliado**, **Enemigo** y **Neutral**.

---

## Comparacion de Relaciones

| Efecto | Aliado | Neutral | Enemigo |
|--------|--------|---------|---------|
| **PvP en territorio** | Desactivado | Reglas estandar | Activado |
| **Proteccion de territorio** | Proteccion mutua | Proteccion estandar | Puede sobrereclamar si esta debilitado |
| **Fuego amigo** | Desactivado | N/A | Activado en todas partes |
| **Color en mapa** | [#5555FF] Azul | [#AAAAAA] Gris | [#FF5555] Rojo |
| **Como establecer** | Acuerdo mutuo | Estado predeterminado | Declaracion unilateral |
| **Acceso a chat** | Canal de chat aliado | Ninguno | Ninguno |

---

## Ver Relaciones

`/f relations`

Muestra todas tus alianzas actuales, enemigos y cualquier solicitud de alianza pendiente.

## Como Funcionan las Relaciones

- **Neutral** es el estado predeterminado entre todas las facciones. Se aplican las reglas estandar del servidor.
- **Alianza** requiere que ambas facciones esten de acuerdo. Cualquier lado puede romperla unilateralmente.
- **Enemigo** se declara de forma unilateral. No se necesita acuerdo -- la otra faccion queda marcada inmediatamente como tu enemigo.

>[!INFO] Las relaciones son gestionadas por Oficiales y Lideres. Los Miembros pueden ver relaciones pero no pueden cambiarlas.

>[!TIP] Usa `/f relations` regularmente para mantenerte al tanto del panorama diplomatico. Saber quienes son tus enemigos te ayuda a prepararte para conflictos territoriales.
