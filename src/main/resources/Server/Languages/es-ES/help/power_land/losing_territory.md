---
id: power_losing
commands: overclaim
---
# Perder Territorio

Cuando el poder total de una faccion cae por debajo del costo de sus reclamos, se vuelve **vulnerable**. Los enemigos pueden sobrereclamar chunks directamente.

---

## Como Funciona Sobrereclamar

`/f overclaim`

Un Oficial o Lider de una faccion **enemiga** se para en tu chunk reclamado y ejecuta este comando. Si tu faccion esta en deficit de poder, el chunk se transfiere a su faccion.

## Las Matematicas

Cada reclamo cuesta **2.0 de poder** para mantener. Si tu poder total cae por debajo de ese umbral, los chunks en deficit son vulnerables.

>[!WARNING] Sobrereclamar es permanente. Una vez que un enemigo toma un chunk, debes reclamarlo de nuevo (o sobrereclamarlo de vuelta si se debilitan).

---

## Escenario de Ejemplo

| Factor | Valor |
|--------|-------|
| Miembros | 5 jugadores |
| Poder por miembro | 10 cada uno (inicial) |
| **Poder total** | **50** |
| Reclamos | 30 chunks |
| Poder necesario (30 x 2.0) | **60** |
| **Deficit** | **10 de poder faltante** |

En este ejemplo, la faccion ya es vulnerable desde el inicio. Los enemigos podrian sobrereclamar hasta **5 chunks** (10 de deficit / 2.0 por reclamo) antes de que la faccion alcance el equilibrio.

---

## Como Prevenir Sobrereclamaciones

- **No te expandas demasiado** -- siempre manten el poder total por encima del costo de tus reclamos con un margen
- **Mantente activo** -- el poder solo se regenera mientras estas en linea (+0.1/min)
- **Evita muertes innecesarias** -- cada muerte cuesta 1.0 de poder
- **Recluta mas miembros** -- mas jugadores significa mas poder total
- **Desreclama chunks sin usar** -- libera poder con `/f unclaim`

>[!TIP] Revisa tu estado de poder regularmente con `/f power`. Si tu poder total esta cerca del costo de tus reclamos, considera desreclamar chunks menos importantes antes de una guerra.
