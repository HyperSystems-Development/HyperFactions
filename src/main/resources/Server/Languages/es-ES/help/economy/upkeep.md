---
id: economy_upkeep
---
# Mantenimiento de Territorio

Las facciones deben pagar un mantenimiento continuo
para conservar su territorio reclamado. Esto evita
el acaparamiento de tierras y mantiene el mapa activo.

## Costos de Mantenimiento

| Configuracion | Valor por defecto |
|---------------|-------------------|
| Costo por chunk | 2.0 por ciclo |
| Intervalo de pago | Cada 24 horas |
| Chunks gratis | 3 (sin costo) |
| Modo de escalado | Tarifa plana |

Tus primeros **3 chunks son gratis**. Mas alla de
eso, cada chunk adicional reclamado cuesta 2.0 por
ciclo de pago.

## Pago Automatico

El pago automatico esta **habilitado por defecto**.
El sistema deduce automaticamente el mantenimiento de
tu tesoreria en cada intervalo. No requiere accion
manual.

---

## Periodo de Gracia

Si tu tesoreria no puede cubrir el mantenimiento,
comienza un **periodo de gracia de 48 horas**. Se
envia una advertencia 6 horas antes de que se
empiecen a perder reclamos.

>[!WARNING] Si el mantenimiento sigue sin pagarse despues del periodo de gracia, tu faccion pierde 1 reclamo por ciclo hasta que los costos se cubran o todos los reclamos extra desaparezcan.

## Ejemplo

*Una faccion con 8 reclamos paga por 5 chunks (8 menos 3 gratis). A 2.0 por chunk, eso es 10.0 por ciclo.*

>[!TIP] Manten tu tesoreria por encima del costo de mantenimiento. Usa /f balance para revisar tus reservas.
