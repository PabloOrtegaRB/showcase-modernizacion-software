package cl.valdora.modernizacion.moderno;

import cl.valdora.modernizacion.comun.CatalogoPrecios;
import cl.valdora.modernizacion.comun.RegistroAuditoria;
import cl.valdora.modernizacion.moderno.Modelo.Cotizacion;
import cl.valdora.modernizacion.moderno.Modelo.ProductoNoEncontradoException;
import cl.valdora.modernizacion.moderno.Modelo.SolicitudCotizacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static cl.valdora.modernizacion.Lineas.moderno;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de la versión modernizada. Mockito aísla el catálogo y la auditoría. */
@ExtendWith(MockitoExtension.class)
class ServicioCotizacionTest {

    @Mock
    private CatalogoPrecios catalogo;

    @Mock
    private RegistroAuditoria auditoria;

    private ServicioCotizacion servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioCotizacion(catalogo, auditoria, () -> "COT-PRUEBA");
        lenient().when(catalogo.precioUnitario("MAN-0001")).thenReturn(Optional.of(85_000L));
        lenient().when(catalogo.precioUnitario("CLI-0002")).thenReturn(Optional.of(42_500L));
        lenient().when(catalogo.precioUnitario("REP-0005")).thenReturn(Optional.of(12_990L));
        lenient().when(catalogo.precioUnitario("INS-0004")).thenReturn(Optional.of(310_000L));
    }

    @Test
    @DisplayName("Caso normal: mismo resultado que la versión heredada")
    void casoNormal() {
        Cotizacion c = servicio.cotizar(new SolicitudCotizacion(moderno("MAN-0001", 2, "CLI-0002", 3), false, false));
        assertThat(c.folio()).isEqualTo("COT-PRUEBA");
        assertThat(c.neto().pesos()).isEqualTo(297_500);
        assertThat(c.iva().pesos()).isEqualTo(56_525);
        assertThat(c.total().pesos()).isEqualTo(354_025);
    }

    @Test
    @DisplayName("Corrige D1: redondea al peso (77.290,5 → 77.291)")
    void corrigeD1() {
        Cotizacion c = servicio.cotizar(new SolicitudCotizacion(moderno("REP-0005", 5), false, false));
        assertThat(c.iva().pesos()).isEqualTo(12_341);
        assertThat(c.total().pesos()).isEqualTo(77_291);
    }

    @Test
    @DisplayName("Corrige D2: el 2 % de cliente frecuente se calcula sobre el neto")
    void corrigeD2() {
        Cotizacion c = servicio.cotizar(new SolicitudCotizacion(moderno("MAN-0001", 10), true, true));
        assertThat(c.descuentoClienteFrecuente().pesos()).isEqualTo(17_000);
        assertThat(c.recargoUrgencia().pesos()).isEqualTo(127_500);
        assertThat(c.baseImponible().pesos()).isEqualTo(960_500);
        assertThat(c.iva().pesos()).isEqualTo(182_495);
        assertThat(c.total().pesos()).isEqualTo(1_142_995);
    }

    @Test
    @DisplayName("Corrige D3: un SKU inexistente da un error claro y no se audita nada")
    void corrigeD3() {
        when(catalogo.precioUnitario("XXX-9999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.cotizar(new SolicitudCotizacion(moderno("XXX-9999", 1), false, false)))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessageContaining("XXX-9999");
        verifyNoInteractions(auditoria);
    }

    @Test
    @DisplayName("Consulta el catálogo una sola vez por SKU, aunque se repita en varias líneas")
    void consultaElCatalogoUnaVezPorSku() {
        servicio.cotizar(new SolicitudCotizacion(moderno("MAN-0001", 1, "MAN-0001", 2, "CLI-0002", 1, "MAN-0001", 3), false, false));
        verify(catalogo, times(1)).precioUnitario("MAN-0001");
        verify(catalogo, times(1)).precioUnitario("CLI-0002");
        verifyNoMoreInteractions(catalogo);
    }

    @Test
    @DisplayName("Registra la cotización emitida en la auditoría")
    void registraLaAuditoria() {
        servicio.cotizar(new SolicitudCotizacion(moderno("MAN-0001", 2, "CLI-0002", 3), false, false));
        verify(auditoria).registrar(argThat(evento -> evento.contains("COT-PRUEBA") && evento.contains("$354.025")));
    }

    @ParameterizedTest(name = "neto ${0} → descuento por volumen {1}")
    @CsvSource({"999999, 0", "1000000, 0.05", "2999999, 0.05", "3000000, 0.08", "9000000, 0.08"})
    void tramosDeDescuentoPorVolumen(long neto, String tasaEsperada) {
        assertThat(ReglasComerciales.tasaDescuentoVolumen(Dinero.pesos(neto))).isEqualByComparingTo(new BigDecimal(tasaEsperada));
        verifyNoInteractions(catalogo);
    }

    @Test
    @DisplayName("Una cotización con volumen y urgencia combina bien las reglas")
    void volumenYUrgencia() {
        Cotizacion c = servicio.cotizar(new SolicitudCotizacion(moderno("INS-0004", 4), false, true));
        assertThat(c.descuentoVolumen().pesos()).isEqualTo(62_000);
        assertThat(c.recargoUrgencia().pesos()).isEqualTo(186_000);
        assertThat(c.baseImponible().pesos()).isEqualTo(1_364_000);
        assertThat(c.total().pesos()).isEqualTo(1_623_160);
        verify(catalogo, times(1)).precioUnitario(anyString());
    }
}
