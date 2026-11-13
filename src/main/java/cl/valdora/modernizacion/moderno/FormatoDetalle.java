package cl.valdora.modernizacion.moderno;

import cl.valdora.modernizacion.moderno.Modelo.DetalleLinea;

import java.util.List;

/** Genera el detalle en texto (mismo formato que la versión heredada) sin concatenar dentro de un bucle. */
public final class FormatoDetalle {

    private FormatoDetalle() {
    }

    /** Una línea por ítem: {@code sku;cantidad;precioUnitario;subtotal}. */
    public static String comoTexto(List<DetalleLinea> detalle) {
        StringBuilder texto = new StringBuilder(detalle.size() * 32);
        for (DetalleLinea linea : detalle) {
            texto.append(linea.sku()).append(';')
                    .append(linea.cantidad()).append(';')
                    .append(linea.precioUnitario().pesos()).append(';')
                    .append(linea.subtotal().pesos()).append('\n');
        }
        return texto.toString();
    }
}
