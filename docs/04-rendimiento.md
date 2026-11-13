# Rendimiento: versión heredada y versión modernizada

Medición **real** ejecutada el 2026-11-13 con `Rendimiento`: una cotización de 5.000 líneas sobre un catálogo de 400 productos con búsqueda lineal (la estructura del sistema heredado).

| Indicador | Heredada (v1) | Modernizada (v2) | Mejora |
|---|---|---|---|
| Consultas al catálogo por cotización | 10.000 | 400 | −96,0 % |
| Tiempo promedio por cotización | 55,1 ms | 1,0 ms | 52,8 veces más rápida |
| Líneas del método principal | 37 | 16 | Responsabilidades separadas en clases con nombre |

**Equivalencia con las mismas entradas:** neto idéntico: sí · detalle línea por línea idéntico: sí · diferencia en el total: ninguna.

**Por qué mejora:** la versión heredada consulta el catálogo **dos veces por línea** y concatena texto dentro del bucle (costo cuadrático). La modernizada consulta **una vez por SKU distinto** y usa `StringBuilder` (costo lineal).

**Entorno:** Java 21.0.8 · Windows 10 · 4 núcleos. Promedio de 5 ejecuciones (v1) y 50 (v2), después de calentar la JVM. No es un benchmark JMH: sirve para comparar órdenes de magnitud, no para afinar milisegundos.
