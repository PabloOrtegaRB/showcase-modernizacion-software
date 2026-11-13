# Registro de cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Proyecto demostrativo: los defectos son controlados y no corresponden a incidentes reales.

## [2.0.0] - 2026-11-13 · versión modernizada

### Corregido
- **D1:** el total se redondea al peso (mitad hacia arriba) en vez de truncarse.
- **D2:** el descuento de cliente frecuente se calcula sobre el neto de los servicios, no sobre el total con recargo e IVA.
- **D3:** un SKU inexistente produce `ProductoNoEncontradoException` con un mensaje claro, en vez de `NullPointerException`.

### Cambiado
- Modelo con `record` validados en lugar de arreglos de `String`.
- Tipo `Dinero` (pesos enteros) en lugar de `double`.
- Reglas comerciales con nombre en `ReglasComerciales`.
- Folio con `GeneradorFolio` (seguro con varios hilos) en lugar de un contador estático.
- Auditoría inyectada (`RegistroAuditoria`).

### Rendimiento
- Una consulta al catálogo por SKU distinto: de 10.000 a 400 consultas en una cotización de 5.000 líneas.
- `StringBuilder` para el detalle: de 55,1 ms a 1,0 ms por cotización en la medición registrada (ver `docs/04-rendimiento.md`).

### Pruebas
- 26 pruebas: 7 de caracterización, 17 unitarias (con Mockito para el catálogo y la auditoría) y 2 de equivalencia y rendimiento.

## [1.0.0] · versión heredada (referencia)
- Se conserva sin cambios en `legado/` para comparar.
