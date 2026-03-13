---
id: diplomacy_enemies
commands: enemy, neutral
---
# Facciones Enemigas

Declarar un enemigo es una **accion unilateral** que inmediatamente habilita PvP y agresion territorial contra la faccion objetivo. No se requiere acuerdo.

---

## Declarar un Enemigo

`/f enemy <faction>`

Marca instantaneamente a la faccion objetivo como tu enemigo. Esto entra en efecto inmediatamente -- no se necesita confirmacion del otro lado. Requiere rango de Oficial o superior.

## Restablecer a Neutral

`/f neutral <faction>`

Termina el estado de enemigo y restablece la relacion a neutral. Esto tambien requiere Oficial+ y entra en efecto inmediatamente.

---

## Que Habilita el Estado de Enemigo

| Efecto | Detalles |
|--------|----------|
| **PvP en territorio** | PvP completo habilitado en el territorio de ambas facciones |
| **Sobrereclamar** | Puedes usar `/f overclaim` en sus chunks si estan en deficit de poder |
| **Marcacion en mapa** | El territorio enemigo se muestra en [#FF5555] rojo en el mapa de territorio |
| **Sin proteccion** | La proteccion de territorio estandar no previene PvP enemigo |

>[!WARNING] Declarar un enemigo es una decision seria. Sus miembros tambien pueden pelear contigo en tu propio territorio una vez que declares.

---

## Consideraciones Estrategicas

- Las declaraciones de enemigo son **unilaterales** -- puedes declarar sin su consentimiento, pero ellos tambien te ven como hostil
- Antes de declarar, revisa el poder del objetivo con `/f info <faction>`. Si son fuertes, puedes perder territorio en su lugar
- Debilita a los enemigos a traves de combate repetido para drenar su poder, luego sobreclama su tierra
- **No hay limite** de cuantos enemigos puedes tener, pero pelear en multiples frentes es arriesgado

>[!TIP] Usa `/f neutral <faction>` para desescalar conflictos. A veces una paz estrategica es mas valiosa que una guerra continua.

>[!NOTE] Si estas aliado con una faccion y la declaras como enemiga, la alianza se rompe primero.
