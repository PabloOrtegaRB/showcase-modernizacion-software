package cl.valdora.modernizacion;

import cl.valdora.modernizacion.comun.CatalogoEnLista;
import cl.valdora.modernizacion.legado.CotizadorLegado;
import cl.valdora.modernizacion.moderno.FormatoDetalle;
import cl.valdora.modernizacion.moderno.GeneradorFolio;
import cl.valdora.modernizacion.moderno.Modelo.Cotizacion;
import cl.valdora.modernizacion.moderno.Modelo.LineaCotizacion;
import cl.valdora.modernizacion.moderno.Modelo.SolicitudCotizacion;
import cl.valdora.modernizacion.moderno.ServicioCotizacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La refactorización conserva el comportamiento: con 500 cotizaciones aleatorias (semilla fija) la versión
 * heredada y la moderna entregan el mismo neto y el mismo detalle. En el total, la única diferencia
 * posible es de 1 peso por el truncamiento de la versión heredada (defecto D1).
 */
class EquivalenciaTest {

    private static final String[] SKUS = {"MAN-0001", "CLI-0002", "ELE-0003", "INS-0004"};

    @Test
    @DisplayName("500 cotizaciones aleatorias: mismo neto y detalle; total igual o 1 peso arriba (D1)")
    void laVersionModernaConservaElComportamiento() {
        CatalogoEnLista catalogo = CatalogoEnLista.deDemostracion();
        CotizadorLegado legado = new CotizadorLegado(catalogo);
        ServicioCotizacion moderno = new ServicioCotizacion(catalogo, evento -> { }, GeneradorFolio.secuencial());
        Random azar = new Random(20261113L);
        int diferenciasDeUnPeso = 0;

        for (int caso = 0; caso < 500; caso++) {
            List<String[]> lineasLegado = new ArrayList<>();
            List<LineaCotizacion> lineasModerno = new ArrayList<>();
            int cantidadLineas = 1 + azar.nextInt(8);
            for (int i = 0; i < cantidadLineas; i++) {
                String sku = SKUS[azar.nextInt(SKUS.length)];
                int cantidad = 1 + azar.nextInt(12);
                lineasLegado.add(new String[] {sku, String.valueOf(cantidad)});
                lineasModerno.add(new LineaCotizacion(sku, cantidad));
            }
            boolean urgente = azar.nextBoolean();

            String[] v1 = legado.cotizar(lineasLegado, false, urgente);
            Cotizacion v2 = moderno.cotizar(new SolicitudCotizacion(lineasModerno, false, urgente));

            assertThat(v2.neto().pesos()).as("neto, caso %d", caso).isEqualTo(Long.parseLong(v1[1]));
            assertThat(FormatoDetalle.comoTexto(v2.detalle())).as("detalle, caso %d", caso).isEqualTo(v1[5]);
            long diferencia = v2.total().pesos() - Long.parseLong(v1[4]);
            assertThat(diferencia).as("total, caso %d", caso).isBetween(0L, 1L);
            if (diferencia == 1) {
                diferenciasDeUnPeso++;
            }
        }
        System.out.printf("Equivalencia: 500 casos, %d con 1 peso de diferencia por D1%n", diferenciasDeUnPeso);
    }

    @Test
    @DisplayName("La versión moderna pide al catálogo 50 veces menos datos en una cotización de 500 líneas")
    void reduceLasConsultasAlCatalogo() {
        CatalogoEnLista catalogo = new CatalogoEnLista();
        for (int i = 1; i <= 20; i++) {
            catalogo.agregar(String.format("SKU-%04d", i), 1_000L * i);
        }
        List<String[]> lineasLegado = new ArrayList<>();
        List<LineaCotizacion> lineasModerno = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            String sku = String.format("SKU-%04d", 1 + i % 20);
            lineasLegado.add(new String[] {sku, "1"});
            lineasModerno.add(new LineaCotizacion(sku, 1));
        }

        new CotizadorLegado(catalogo).cotizar(lineasLegado, false, false);
        long consultasLegado = catalogo.consultas();
        catalogo.reiniciarConsultas();
        new ServicioCotizacion(catalogo, evento -> { }, GeneradorFolio.secuencial())
                .cotizar(new SolicitudCotizacion(lineasModerno, false, false));

        assertThat(consultasLegado).isEqualTo(1_000);
        assertThat(catalogo.consultas()).isEqualTo(20);
    }
}
