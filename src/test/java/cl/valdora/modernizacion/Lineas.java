package cl.valdora.modernizacion;

import cl.valdora.modernizacion.moderno.Modelo.LineaCotizacion;

import java.util.ArrayList;
import java.util.List;

/** Ayuda de pruebas: las mismas líneas en el formato heredado (String[]) y en el moderno (record). */
public final class Lineas {

    private Lineas() {
    }

    /** {@code de("MAN-0001", 2, "CLI-0002", 3)}. */
    public static List<String[]> legado(Object... skuYCantidad) {
        List<String[]> lineas = new ArrayList<>();
        for (int i = 0; i < skuYCantidad.length; i += 2) {
            lineas.add(new String[] {(String) skuYCantidad[i], String.valueOf(skuYCantidad[i + 1])});
        }
        return lineas;
    }

    public static List<LineaCotizacion> moderno(Object... skuYCantidad) {
        List<LineaCotizacion> lineas = new ArrayList<>();
        for (int i = 0; i < skuYCantidad.length; i += 2) {
            lineas.add(new LineaCotizacion((String) skuYCantidad[i], (Integer) skuYCantidad[i + 1]));
        }
        return lineas;
    }
}
