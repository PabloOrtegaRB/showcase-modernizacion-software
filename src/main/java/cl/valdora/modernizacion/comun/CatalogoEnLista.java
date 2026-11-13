package cl.valdora.modernizacion.comun;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Catálogo guardado en una lista con búsqueda lineal, tal como en el sistema heredado.
 * Cuenta cada consulta para poder comparar cuántas veces la pide cada versión del cotizador.
 */
public class CatalogoEnLista implements CatalogoPrecios {

    private final List<Map.Entry<String, Long>> productos = new ArrayList<>();
    private final AtomicLong consultas = new AtomicLong();

    public CatalogoEnLista agregar(String sku, long precio) {
        productos.add(Map.entry(sku, precio));
        return this;
    }

    @Override
    public Optional<Long> precioUnitario(String sku) {
        consultas.incrementAndGet();
        for (Map.Entry<String, Long> producto : productos) {
            if (producto.getKey().equals(sku)) {
                return Optional.of(producto.getValue());
            }
        }
        return Optional.empty();
    }

    public long consultas() {
        return consultas.get();
    }

    public void reiniciarConsultas() {
        consultas.set(0);
    }

    /** Catálogo ficticio usado en las pruebas y en la documentación. */
    public static CatalogoEnLista deDemostracion() {
        return new CatalogoEnLista()
                .agregar("MAN-0001", 85_000)   // visita de mantención preventiva
                .agregar("CLI-0002", 42_500)   // limpieza de equipo de clima
                .agregar("ELE-0003", 120_000)  // revisión de tablero eléctrico
                .agregar("INS-0004", 310_000)  // termografía
                .agregar("REP-0005", 12_990);  // filtro de aire
    }
}
