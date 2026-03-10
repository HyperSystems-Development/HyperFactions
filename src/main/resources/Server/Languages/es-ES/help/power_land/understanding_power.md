---
id: power_understanding
commands: power
---
# Entender el Poder

El poder es el recurso principal que determina cuanto territorio puede mantener tu faccion. Cada jugador tiene poder personal que contribuye al total de la faccion.

---

## Valores de Poder Predeterminados

| Configuracion | Valor |
|---------------|-------|
| **Poder maximo por jugador** | 20 |
| **Poder inicial** | 10 |
| **Penalidad por muerte** | -1.0 por muerte |
| **Recompensa por matar** | 0.0 |
| **Tasa de regeneracion** | +0.1 por minuto (mientras esta en linea) |
| **Costo de poder por reclamo** | 2.0 |
| **Desconexion mientras etiquetado** | -1.0 adicional |

## Como Funciona

El **poder total** de tu faccion es la suma del poder personal de cada miembro. Tu **poder requerido** es el numero de reclamos multiplicado por 2.0. Mientras el poder total se mantenga por encima del poder requerido, tu territorio esta seguro.

>[!INFO] El poder se regenera pasivamente a 0.1 por minuto mientras estas en linea. A esa tasa, recuperar 1.0 de poder toma aproximadamente 10 minutos.

---

## Consultar Tu Poder

`/f power`

Muestra tu poder personal, el poder total de tu faccion y cuanto se necesita para mantener los reclamos actuales.

## La Zona de Peligro

Si el poder total cae **por debajo** de la cantidad requerida para tus reclamos, tu faccion se vuelve vulnerable. Los enemigos pueden usar `/f overclaim` para robar tus chunks.

>[!WARNING] Multiples muertes en un corto periodo pueden escalar rapidamente. Si tienes 5 miembros cada uno con 10 de poder (50 total) y 20 reclamos (40 necesarios), solo 5 muertes en tu equipo te bajan a 45 -- aun seguro. Pero 11 muertes te ponen en 39, por debajo del umbral de 40.

>[!TIP] Manten un margen de poder. No reclames cada chunk que puedas costear -- deja espacio para algunas muertes sin volverte vulnerable.
