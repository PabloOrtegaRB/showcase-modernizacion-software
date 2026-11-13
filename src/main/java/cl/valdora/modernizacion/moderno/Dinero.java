package cl.valdora.modernizacion.moderno;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Monto en pesos chilenos, sin decimales. Inmutable y sin errores de punto flotante. */
public record Dinero(long pesos) implements Comparable<Dinero> {

    public static final Dinero CERO = new Dinero(0);
    private static final Locale CHILE = Locale.forLanguageTag("es-CL");

    public Dinero {
        if (pesos < 0) {
            throw new IllegalArgumentException("Un monto no puede ser negativo: " + pesos);
        }
    }

    public static Dinero pesos(long pesos) {
        return new Dinero(pesos);
    }

    public Dinero mas(Dinero otro) {
        return new Dinero(Math.addExact(pesos, otro.pesos));
    }

    public Dinero menos(Dinero otro) {
        return new Dinero(Math.subtractExact(pesos, otro.pesos));
    }

    public Dinero por(int cantidad) {
        return new Dinero(Math.multiplyExact(pesos, cantidad));
    }

    /** Aplica una tasa (por ejemplo 0.19) y redondea al peso: la mitad sube (regla comercial). */
    public Dinero porcentaje(BigDecimal tasa) {
        return new Dinero(BigDecimal.valueOf(pesos).multiply(tasa).setScale(0, RoundingMode.HALF_UP).longValueExact());
    }

    public boolean esMayorOIgualA(Dinero otro) {
        return pesos >= otro.pesos;
    }

    @Override
    public int compareTo(Dinero otro) {
        return Long.compare(pesos, otro.pesos);
    }

    @Override
    public String toString() {
        return "$" + String.format(CHILE, "%,d", pesos);
    }
}
