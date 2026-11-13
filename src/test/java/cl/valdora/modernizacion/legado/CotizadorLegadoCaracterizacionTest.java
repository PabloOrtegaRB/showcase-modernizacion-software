package cl.valdora.modernizacion.legado;

import cl.valdora.modernizacion.comun.CatalogoEnLista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static cl.valdora.modernizacion.Lineas.legado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PRUEBAS DE CARACTERIZACIÓN: fijan el comportamiento ACTUAL del sistema heredado, defectos incluidos,
 * antes de tocarlo. Así cualquier cambio de comportamiento queda a la vista.
 * Resultado: {folio, neto, descuento, iva, total, detalle}.
 */
class CotizadorLegadoCaracterizacionTest {

    private CotizadorLegado cotizador;

    @BeforeEach
    void preparar() {
        CotizadorLegado.contador = 0;
        cotizador = new CotizadorLegado(CatalogoEnLista.deDemostracion());
    }

    @Test
    @DisplayName("Caso normal sin descuentos: neto, IVA, total y detalle")
    void casoNormalSinDescuentos() {
        String[] r = cotizador.cotizar(legado("MAN-0001", 2, "CLI-0002", 3), false, false);
        assertThat(r[1]).isEqualTo("297500");
        assertThat(r[3]).isEqualTo("56525");
        assertThat(r[4]).isEqualTo("354025");
        assertThat(r[5]).isEqualTo("MAN-0001;2;85000;170000\nCLI-0002;3;42500;127500\n");
    }

    @Test
    @DisplayName("Descuento por volumen del 5 % desde $1.000.000")
    void descuentoPorVolumenDel5() {
        String[] r = cotizador.cotizar(legado("INS-0004", 4), false, false);
        assertThat(r[2]).isEqualTo("62000");
        assertThat(r[4]).isEqualTo("1401820");
    }

    @Test
    @DisplayName("Descuento por volumen del 8 % desde $3.000.000")
    void descuentoPorVolumenDel8() {
        String[] r = cotizador.cotizar(legado("INS-0004", 10), false, false);
        assertThat(r[2]).isEqualTo("248000");
        assertThat(r[4]).isEqualTo("3393880");
    }

    @Test
    @DisplayName("DEFECTO D1: trunca el total (77.290,5 queda en 77.290; debería ser 77.291)")
    void defectoD1TruncaEnVezDeRedondear() {
        String[] r = cotizador.cotizar(legado("REP-0005", 5), false, false);
        assertThat(r[4]).isEqualTo("77290");
    }

    @Test
    @DisplayName("DEFECTO D2: el 2 % de cliente frecuente se descuenta del total con recargo e IVA")
    void defectoD2DescuentoFrecuenteSobreElTotal() {
        String[] r = cotizador.cotizar(legado("MAN-0001", 10), true, true);
        assertThat(r[4]).isEqualTo("1139960"); // la regla correcta da $1.142.995
    }

    @Test
    @DisplayName("DEFECTO D3: un SKU inexistente provoca NullPointerException")
    void defectoD3NullPointerConSkuInexistente() {
        assertThatThrownBy(() -> cotizador.cotizar(legado("XXX-9999", 1), false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("El folio depende de un contador estático compartido")
    void elFolioUsaUnContadorGlobal() {
        assertThat(cotizador.cotizar(legado("MAN-0001", 1), false, false)[0]).isEqualTo("COT-1");
        CotizadorLegado otro = new CotizadorLegado(CatalogoEnLista.deDemostracion());
        assertThat(otro.cotizar(legado("MAN-0001", 1), false, false)[0]).isEqualTo("COT-2");
    }
}
