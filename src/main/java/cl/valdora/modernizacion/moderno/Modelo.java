package cl.valdora.modernizacion.moderno;

import java.util.List;
import java.util.Locale;

/** Modelo inmutable de la cotización: reemplaza los arreglos de String del sistema heredado. */
public final class Modelo {

    private Modelo() {
    }

    /** Línea pedida por el cliente. Se valida al crearla. */
    public record LineaCotizacion(String sku, int cantidad) {
        public LineaCotizacion {
            if (sku == null || sku.isBlank()) {
                throw new CotizacionInvalidaException("Cada línea necesita un SKU.");
            }
            sku = sku.trim().toUpperCase(Locale.ROOT);
            if (cantidad <= 0) {
                throw new CotizacionInvalidaException("La cantidad de " + sku + " debe ser mayor que cero.");
            }
        }
    }

    public record SolicitudCotizacion(List<LineaCotizacion> lineas, boolean clienteFrecuente, boolean urgente) {
        public SolicitudCotizacion {
            if (lineas == null || lineas.isEmpty()) {
                throw new CotizacionInvalidaException("La cotización necesita al menos una línea.");
            }
            lineas = List.copyOf(lineas);
        }
    }

    public record DetalleLinea(String sku, int cantidad, Dinero precioUnitario, Dinero subtotal) {
    }

    public record Cotizacion(String folio, List<DetalleLinea> detalle, Dinero neto, Dinero descuentoVolumen,
                             Dinero descuentoClienteFrecuente, Dinero recargoUrgencia, Dinero baseImponible,
                             Dinero iva, Dinero total) {
    }

    /** Datos de entrada inválidos (cantidades, líneas vacías, etc.). */
    public static class CotizacionInvalidaException extends RuntimeException {
        public CotizacionInvalidaException(String mensaje) {
            super(mensaje);
        }
    }

    /** El SKU pedido no existe en el catálogo: reemplaza el NullPointerException del sistema heredado. */
    public static class ProductoNoEncontradoException extends RuntimeException {
        private final String sku;

        public ProductoNoEncontradoException(String sku) {
            super("No existe el producto «" + sku + "» en el catálogo.");
            this.sku = sku;
        }

        public String getSku() {
            return sku;
        }
    }
}
