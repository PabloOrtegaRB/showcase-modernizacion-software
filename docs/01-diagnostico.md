# 1. Diagnóstico inicial del cotizador heredado

> **Proyecto demostrativo.** El sistema heredado (`legado/CotizadorLegado.java`) se escribió a propósito con problemas típicos para mostrar el proceso de modernización. **Los defectos son controlados: no corresponden a incidentes reales.**

## Cómo se hizo el diagnóstico

1. Lectura del código y de las reglas comerciales esperadas.
2. **Pruebas de caracterización** (`CotizadorLegadoCaracterizacionTest`), que fijan el comportamiento actual, defectos incluidos, **antes de cambiar nada**.
3. Casos de borde: redondeo, combinación de descuentos y SKU inexistente.
4. Medición de consultas al catálogo y de tiempo con una cotización grande.

## Defectos (afectan el resultado)

| ID | Defecto | Caso que lo demuestra | v1 entrega | Correcto | Impacto |
|---|---|---|---|---|---|
| **D1** | Trunca el total en vez de redondear al peso (dinero en `double` y conversión `(long)`) | 5 × filtro de aire ($12.990) → total exacto $77.290,5 | $77.290 | $77.291 | Diferencias de $1 en el 26 % de las cotizaciones de la prueba de equivalencia |
| **D2** | El 2 % de cliente frecuente se descuenta del total, que incluye el recargo por urgencia y el IVA. La regla dice: 2 % del neto de los servicios | 10 visitas ($85.000), cliente frecuente y urgente | $1.139.960 | $1.142.995 | Cobra de menos $3.035 en este caso |
| **D3** | Un SKU inexistente provoca `NullPointerException` (`orElse(null)` convertido a `double`) | SKU «XXX-9999» | Error genérico | Mensaje claro con el SKU | El usuario no sabe qué corregir |

## Olores de código (dificultan mantener y cambiar)

| ID | Olor | Dónde | Riesgo |
|---|---|---|---|
| O1 | Método largo que hace todo: calcula, aplica reglas y arma el texto (37 líneas) | `cotizar` | Cada cambio toca todo |
| O2 | Números mágicos: 3000000, 0.08, 0.05, 0.15, 0.19, 0.02 | `cotizar` | Reglas escondidas y fáciles de romper |
| O3 | Dinero en `double` | `cotizar` | Errores de punto flotante (origen de D1) |
| O4 | Arreglos `String[]` como modelo de entrada y salida | firma de `cotizar` | Sin validación ni tipos |
| O5 | Contador estático público para el folio | `contador` | Folios duplicados con varios hilos y estado compartido |
| O6 | Dependencia concreta mezclada con la lógica, sin auditoría | constructor | Difícil de probar y de auditar |

## Rendimiento

| ID | Problema | Medición con 5.000 líneas y 400 productos |
|---|---|---|
| P1 | Dos consultas al catálogo por línea (y el catálogo hace búsqueda lineal) | 10.000 consultas por cotización |
| P2 | Concatenación de `String` dentro del bucle (costo cuadrático) | Parte importante de los 55 ms por cotización |

Las cifras completas, medidas en una ejecución real, están en [`04-rendimiento.md`](04-rendimiento.md).

## Plan de modernización (en este orden)

1. Fijar el comportamiento con pruebas de caracterización.
2. Introducir un modelo con tipos y validaciones (`Modelo`).
3. Reemplazar `double` por `Dinero`, con redondeo comercial (corrige D1).
4. Nombrar las reglas en `ReglasComerciales` y aplicar el descuento de cliente frecuente sobre el neto (corrige D2).
5. Error explícito `ProductoNoEncontradoException` (corrige D3).
6. Una consulta por SKU distinto y `StringBuilder` (corrige P1 y P2).
7. Folio seguro con varios hilos y auditoría inyectada (corrige O5 y O6).
8. Comprobar la equivalencia con 500 cotizaciones aleatorias (`EquivalenciaTest`).
