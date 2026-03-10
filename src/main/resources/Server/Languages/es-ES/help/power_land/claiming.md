---
id: power_claiming
commands: claim, unclaim
---
# Reclamar Territorio

Reclamar un chunk lo protege bajo el control de tu faccion. Solo los miembros de la faccion pueden construir, destruir o acceder a contenedores dentro del territorio reclamado.

---

## Como Reclamar

`/f claim`

Parate en el chunk que quieres reclamar y ejecuta este comando. El chunk queda protegido inmediatamente. Requiere rango de **Oficial** o superior.

## Como Desreclamar

`/f unclaim`

Libera el chunk donde estas parado de vuelta a terreno salvaje. Tambien requiere Oficial+.

---

## Reglas de Reclamo

| Regla | Predeterminado |
|-------|----------------|
| **Costo de poder por reclamo** | 2.0 de poder |
| **Reclamos maximos** | 100 por faccion |
| **Solo adyacentes** | No (puedes reclamar en cualquier lugar) |

>[!INFO] Cada reclamo cuesta 2.0 de poder para mantener. Una faccion con 50 de poder total puede mantener hasta 25 reclamos de forma segura.

---

## Que Proporciona la Proteccion

Dentro del territorio reclamado, lo siguiente se aplica por defecto:

- **Los foraneos** no pueden destruir, colocar o interactuar con bloques
- **Los aliados** pueden usar puertas, asientos y transporte pero no pueden destruir o colocar bloques
- **Los Miembros y Oficiales** tienen acceso completo para construir, destruir y usar todo
- El acceso a contenedores (cofres, cajas) esta restringido solo a miembros

>[!TIP] Tambien puedes reclamar directamente desde el mapa de territorio. Abre `/f map` y haz clic en chunks sin reclamar para reclamarlos.

>[!WARNING] No te expandas demasiado. Si tu faccion pierde poder por muertes, los reclamos que excedan tu presupuesto de poder se vuelven vulnerables a sobrereclamaciones.
