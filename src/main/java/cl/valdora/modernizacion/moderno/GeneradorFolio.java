package cl.valdora.modernizacion.moderno;

import java.util.concurrent.atomic.AtomicLong;

/** Entrega folios únicos. Reemplaza el contador estático del sistema heredado. */
@FunctionalInterface
public interface GeneradorFolio {

    String siguiente();

    /** Implementación segura con varios hilos: COT-1, COT-2, ... */
    static GeneradorFolio secuencial() {
        AtomicLong contador = new AtomicLong();
        return () -> "COT-" + contador.incrementAndGet();
    }
}
