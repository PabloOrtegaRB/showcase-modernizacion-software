package cl.valdora.modernizacion.moderno;

import cl.valdora.modernizacion.moderno.Modelo.CotizacionInvalidaException;
import cl.valdora.modernizacion.moderno.Modelo.LineaCotizacion;
import cl.valdora.modernizacion.moderno.Modelo.SolicitudCotizacion;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionesTest {

    @Test
    void unaCantidadCeroONegativaEsInvalida() {
        assertThatThrownBy(() -> new LineaCotizacion("MAN-0001", 0)).isInstanceOf(CotizacionInvalidaException.class)
                .hasMessageContaining("mayor que cero");
    }

    @Test
    void unSkuVacioEsInvalidoYSeNormalizaAMayusculas() {
        assertThatThrownBy(() -> new LineaCotizacion("  ", 1)).isInstanceOf(CotizacionInvalidaException.class);
        assertThat(new LineaCotizacion(" man-0001 ", 1).sku()).isEqualTo("MAN-0001");
    }

    @Test
    void unaCotizacionSinLineasEsInvalida() {
        assertThatThrownBy(() -> new SolicitudCotizacion(List.of(), false, false)).isInstanceOf(CotizacionInvalidaException.class);
    }

    @Test
    void elDineroNoAdmiteNegativosYSeMuestraEnFormatoChileno() {
        assertThatThrownBy(() -> Dinero.pesos(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Dinero.pesos(1_234_567)).hasToString("$1.234.567");
        assertThat(Dinero.pesos(64_950).porcentaje(ReglasComerciales.TASA_IVA).pesos()).isEqualTo(12_341);
    }

    @Test
    void losFoliosSonUnicosConVariosHilos() throws Exception {
        GeneradorFolio folios = GeneradorFolio.secuencial();
        ExecutorService hilos = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> pedidos = new java.util.ArrayList<>();
            for (int i = 0; i < 4_000; i++) {
                pedidos.add(hilos.submit(folios::siguiente));
            }
            Set<String> unicos = new HashSet<>();
            for (Future<String> pedido : pedidos) {
                unicos.add(pedido.get());
            }
            assertThat(unicos).hasSize(4_000);
        } finally {
            hilos.shutdown();
        }
    }
}
