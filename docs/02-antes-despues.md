# 2. Código antes y después

Fragmentos reales del repositorio: `legado/CotizadorLegado.java` (antes) y el paquete `moderno/` (después).

## Dinero y redondeo (defecto D1)

**Antes:** `double` y truncamiento.

```java
double iva = base * 0.19;
double total = base + iva;
long totalFinal = (long) total;          // 77.290,5 → 77.290
```

**Después:** un tipo `Dinero` con pesos enteros y redondeo comercial.

```java
public Dinero porcentaje(BigDecimal tasa) {
    return new Dinero(BigDecimal.valueOf(pesos).multiply(tasa)
            .setScale(0, RoundingMode.HALF_UP).longValueExact());   // 12.340,5 → 12.341
}
```

## Reglas comerciales con nombre (defecto D2)

**Antes:** números mágicos y el descuento aplicado sobre el total con IVA.

```java
if (neto >= 3000000) { desc = neto * 0.08; } else if (neto >= 1000000) { desc = neto * 0.05; }
...
if (frecuente) { total = total - total * 0.02; }
```

**Después:** reglas en un solo lugar, y el 2 % calculado sobre el neto.

```java
Dinero descuentoVolumen = neto.porcentaje(ReglasComerciales.tasaDescuentoVolumen(neto));
Dinero descuentoFrecuente = solicitud.clienteFrecuente() ? neto.porcentaje(DESCUENTO_CLIENTE_FRECUENTE) : Dinero.CERO;
Dinero recargo = solicitud.urgente() ? neto.porcentaje(RECARGO_URGENCIA) : Dinero.CERO;
Dinero base = neto.menos(descuentoVolumen).menos(descuentoFrecuente).mas(recargo);
```

## Consultas al catálogo y texto del detalle (P1 y P2)

**Antes:** dos consultas por línea y concatenación dentro del bucle.

```java
double precio = catalogo.precioUnitario(sku).orElse(null);
detalle = detalle + sku + ";" + cant + ";" + (long) precio + ";"
        + (long) (catalogo.precioUnitario(sku).orElse(null) * cant) + "\n";
```

**Después:** una consulta por SKU distinto y `StringBuilder`.

```java
precios.computeIfAbsent(linea.sku(), sku -> catalogo.precioUnitario(sku)
        .map(Dinero::pesos)
        .orElseThrow(() -> new ProductoNoEncontradoException(sku)));
...
texto.append(linea.sku()).append(';').append(linea.cantidad()).append(';')...
```

## SKU inexistente (defecto D3)

**Antes:** `NullPointerException`, sin indicar qué falló.
**Después:** `ProductoNoEncontradoException` con el mensaje «No existe el producto «XXX-9999» en el catálogo.».

## Folio

**Antes:** `public static int contador = 0;` y `contador++`, que no es seguro con varios hilos.

**Después:**

```java
static GeneradorFolio secuencial() {
    AtomicLong contador = new AtomicLong();
    return () -> "COT-" + contador.incrementAndGet();
}
```

`ValidacionesTest` lo comprueba con 8 hilos y 4.000 folios únicos.

## Modelo

**Antes:** `List<String[]>` de entrada y un `String[]` de 6 posiciones de salida.
**Después:** `record` inmutables y validados: `LineaCotizacion`, `SolicitudCotizacion`, `DetalleLinea` y `Cotizacion`.
