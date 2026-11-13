package cl.valdora.modernizacion.moderno;

import java.math.BigDecimal;

/** Reglas comerciales de Valdora (empresa ficticia) en un solo lugar y con nombre, en vez de números mágicos. */
public final class ReglasComerciales {

    public static final Dinero UMBRAL_VOLUMEN_ALTO = Dinero.pesos(3_000_000);
    public static final Dinero UMBRAL_VOLUMEN = Dinero.pesos(1_000_000);
    public static final BigDecimal DESCUENTO_VOLUMEN_ALTO = new BigDecimal("0.08");
    public static final BigDecimal DESCUENTO_VOLUMEN = new BigDecimal("0.05");
    /** Se calcula solo sobre el neto de los servicios (no descuenta el recargo ni el IVA). */
    public static final BigDecimal DESCUENTO_CLIENTE_FRECUENTE = new BigDecimal("0.02");
    public static final BigDecimal RECARGO_URGENCIA = new BigDecimal("0.15");
    public static final BigDecimal TASA_IVA = new BigDecimal("0.19");

    private ReglasComerciales() {
    }

    public static BigDecimal tasaDescuentoVolumen(Dinero neto) {
        if (neto.esMayorOIgualA(UMBRAL_VOLUMEN_ALTO)) {
            return DESCUENTO_VOLUMEN_ALTO;
        }
        if (neto.esMayorOIgualA(UMBRAL_VOLUMEN)) {
            return DESCUENTO_VOLUMEN;
        }
        return BigDecimal.ZERO;
    }
}
