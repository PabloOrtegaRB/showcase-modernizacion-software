package cl.valdora.modernizacion.legado;

import cl.valdora.modernizacion.comun.CatalogoPrecios;

import java.util.List;

/**
 * VERSIÓN HEREDADA (v1) del cotizador, conservada sin cambios para comparar.
 *
 * <p>Contiene olores de código y TRES DEFECTOS CONTROLADOS, introducidos a propósito para esta
 * demostración. No corresponden a incidentes reales. El diagnóstico está en docs/01-diagnostico.md.
 */
public class CotizadorLegado {

    // OLOR: estado global mutable; el contador no es seguro con varios hilos.
    public static int contador = 0;

    private CatalogoPrecios catalogo;

    public CotizadorLegado(CatalogoPrecios catalogo) {
        this.catalogo = catalogo;
    }

    /**
     * Devuelve {folio, neto, descuento, iva, total, detalle}.
     * OLOR: método largo, números mágicos, arreglos de String como modelo y dinero en double.
     */
    public String[] cotizar(List<String[]> lineas, boolean frecuente, boolean urgente) {
        contador++;
        double neto = 0;
        String detalle = "";
        for (int i = 0; i < lineas.size(); i++) {
            String sku = lineas.get(i)[0];
            int cant = Integer.parseInt(lineas.get(i)[1]);
            // DEFECTO D3: si el SKU no existe, orElse(null) provoca NullPointerException al convertir a double.
            double precio = catalogo.precioUnitario(sku).orElse(null);
            neto = neto + precio * cant;
            // OLOR: segunda consulta al catálogo por línea y concatenación de String dentro del bucle (O(n²)).
            detalle = detalle + sku + ";" + cant + ";" + (long) precio + ";"
                    + (long) (catalogo.precioUnitario(sku).orElse(null) * cant) + "\n";
        }
        double desc = 0;
        if (neto >= 3000000) {
            desc = neto * 0.08;
        } else if (neto >= 1000000) {
            desc = neto * 0.05;
        }
        double rec = 0;
        if (urgente) {
            rec = neto * 0.15;
        }
        double base = neto - desc + rec;
        double iva = base * 0.19;
        double total = base + iva;
        if (frecuente) {
            // DEFECTO D2: el 2 % de cliente frecuente se descuenta del total (incluye recargo e IVA);
            // la regla comercial dice que se calcula solo sobre el neto de los servicios.
            total = total - total * 0.02;
        }
        // DEFECTO D1: trunca los decimales en vez de redondear al peso.
        long totalFinal = (long) total;
        return new String[] {"COT-" + contador, String.valueOf((long) neto), String.valueOf((long) desc),
                String.valueOf((long) iva), String.valueOf(totalFinal), detalle};
    }
}
