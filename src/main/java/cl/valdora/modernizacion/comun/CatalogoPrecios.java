package cl.valdora.modernizacion.comun;

import java.util.Optional;

/** Puerto hacia el catálogo de precios (en un sistema real, una base de datos o un servicio externo). */
public interface CatalogoPrecios {

    /** Precio unitario en pesos chilenos, o vacío si el SKU no existe. */
    Optional<Long> precioUnitario(String sku);
}
