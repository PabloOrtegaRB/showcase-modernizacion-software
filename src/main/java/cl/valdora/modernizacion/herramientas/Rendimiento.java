package cl.valdora.modernizacion.herramientas;

import cl.valdora.modernizacion.comun.CatalogoEnLista;
import cl.valdora.modernizacion.legado.CotizadorLegado;
import cl.valdora.modernizacion.moderno.FormatoDetalle;
import cl.valdora.modernizacion.moderno.GeneradorFolio;
import cl.valdora.modernizacion.moderno.Modelo.Cotizacion;
import cl.valdora.modernizacion.moderno.Modelo.LineaCotizacion;
import cl.valdora.modernizacion.moderno.Modelo.SolicitudCotizacion;
import cl.valdora.modernizacion.moderno.ServicioCotizacion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mide la versión heredada contra la modernizada y genera docs/04-rendimiento.md y docs/informe/index.html
 * con los valores REALES de la ejecución.
 *
 * <p>Uso (desde la raíz del proyecto): {@code java -cp target/classes cl.valdora.modernizacion.herramientas.Rendimiento docs <n.° de pruebas>}
 */
public final class Rendimiento {

    private static final int LINEAS = 5_000;
    private static final int PRODUCTOS = 400;
    private static final Locale CHILE = Locale.forLanguageTag("es-CL");

    private Rendimiento() {
    }

    public static void main(String[] args) throws IOException {
        Path salida = Path.of(args.length > 0 ? args[0] : "docs");
        String pruebas = args.length > 1 ? args[1] : "—";

        CatalogoEnLista catalogo = new CatalogoEnLista();
        for (int i = 1; i <= PRODUCTOS; i++) {
            catalogo.agregar(sku(i), 500L * (2 + (i * 37) % 600));
        }
        List<String[]> lineasLegado = new ArrayList<>();
        List<LineaCotizacion> lineasModerno = new ArrayList<>();
        for (int i = 0; i < LINEAS; i++) {
            String sku = sku(1 + (i * 7) % PRODUCTOS);
            int cantidad = 1 + i % 5;
            lineasLegado.add(new String[] {sku, String.valueOf(cantidad)});
            lineasModerno.add(new LineaCotizacion(sku, cantidad));
        }
        CotizadorLegado legado = new CotizadorLegado(catalogo);
        ServicioCotizacion moderno = new ServicioCotizacion(catalogo, evento -> { }, GeneradorFolio.secuencial());
        SolicitudCotizacion solicitud = new SolicitudCotizacion(lineasModerno, false, true);

        for (int i = 0; i < 2; i++) {
            legado.cotizar(lineasLegado, false, true);
        }
        for (int i = 0; i < 20; i++) {
            moderno.cotizar(solicitud);
        }
        catalogo.reiniciarConsultas();
        String[] resultadoLegado = legado.cotizar(lineasLegado, false, true);
        long consultasLegado = catalogo.consultas();
        catalogo.reiniciarConsultas();
        Cotizacion resultadoModerno = moderno.cotizar(solicitud);
        long consultasModerno = catalogo.consultas();

        double msLegado = medir(() -> legado.cotizar(lineasLegado, false, true), 5);
        double msModerno = medir(() -> moderno.cotizar(solicitud), 50);

        boolean netoIgual = Long.parseLong(resultadoLegado[1]) == resultadoModerno.neto().pesos();
        boolean detalleIgual = resultadoLegado[5].equals(FormatoDetalle.comoTexto(resultadoModerno.detalle()));
        long diferenciaTotal = resultadoModerno.total().pesos() - Long.parseLong(resultadoLegado[4]);

        Defectos defectos = Defectos.calcular();
        int lineasMetodoLegado = lineasDeMetodo(Path.of("src/main/java/cl/valdora/modernizacion/legado/CotizadorLegado.java"), "public String[] cotizar(");
        int lineasMetodoModerno = lineasDeMetodo(Path.of("src/main/java/cl/valdora/modernizacion/moderno/ServicioCotizacion.java"), "public Cotizacion cotizar(");

        Files.createDirectories(salida.resolve("informe"));
        Medicion m = new Medicion(consultasLegado, consultasModerno, msLegado, msModerno, netoIgual, detalleIgual,
                diferenciaTotal, lineasMetodoLegado, lineasMetodoModerno, pruebas, defectos);
        Files.writeString(salida.resolve("04-rendimiento.md"), markdown(m), StandardCharsets.UTF_8);
        Files.writeString(salida.resolve("informe/index.html"), html(m), StandardCharsets.UTF_8);
        System.out.println(markdown(m));
    }

    private static String sku(int i) {
        return String.format("SKU-%04d", i);
    }

    private static double medir(Runnable tarea, int repeticiones) {
        long inicio = System.nanoTime();
        for (int i = 0; i < repeticiones; i++) {
            tarea.run();
        }
        return (System.nanoTime() - inicio) / 1_000_000.0 / repeticiones;
    }

    /** Cuenta las líneas de un método (desde su firma hasta la llave que lo cierra). */
    static int lineasDeMetodo(Path archivo, String firma) throws IOException {
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        int inicio = -1;
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).contains(firma)) {
                inicio = i;
                break;
            }
        }
        if (inicio < 0) {
            return -1;
        }
        int profundidad = 0;
        for (int i = inicio; i < lineas.size(); i++) {
            for (char c : lineas.get(i).toCharArray()) {
                if (c == '{') {
                    profundidad++;
                } else if (c == '}') {
                    profundidad--;
                    if (profundidad == 0) {
                        return i - inicio + 1;
                    }
                }
            }
        }
        return -1;
    }

    private static String miles(long valor) {
        return String.format(CHILE, "%,d", valor);
    }

    private static String decimal(double valor) {
        return String.format(CHILE, "%,.1f", valor);
    }

    private static String markdown(Medicion m) {
        double reduccion = 100.0 * (m.consultasLegado - m.consultasModerno) / m.consultasLegado;
        return """
                # Rendimiento: versión heredada y versión modernizada

                Medición **real** ejecutada el %s con `Rendimiento`: una cotización de %s líneas sobre un catálogo de %s productos con búsqueda lineal (la estructura del sistema heredado).

                | Indicador | Heredada (v1) | Modernizada (v2) | Mejora |
                |---|---|---|---|
                | Consultas al catálogo por cotización | %s | %s | −%s %% |
                | Tiempo promedio por cotización | %s ms | %s ms | %s veces más rápida |
                | Líneas del método principal | %s | %s | Responsabilidades separadas en clases con nombre |

                **Equivalencia con las mismas entradas:** neto idéntico: %s · detalle línea por línea idéntico: %s · diferencia en el total: %s.

                **Por qué mejora:** la versión heredada consulta el catálogo **dos veces por línea** y concatena texto dentro del bucle (costo cuadrático). La modernizada consulta **una vez por SKU distinto** y usa `StringBuilder` (costo lineal).

                **Entorno:** Java %s · %s · %d núcleos. Promedio de 5 ejecuciones (v1) y 50 (v2), después de calentar la JVM. No es un benchmark JMH: sirve para comparar órdenes de magnitud, no para afinar milisegundos.
                """.formatted(LocalDate.now(), miles(LINEAS), miles(PRODUCTOS),
                miles(m.consultasLegado), miles(m.consultasModerno), decimal(reduccion),
                decimal(m.msLegado), decimal(m.msModerno), decimal(m.msLegado / m.msModerno),
                m.lineasLegado, m.lineasModerno,
                m.netoIgual ? "sí" : "no", m.detalleIgual ? "sí" : "no", textoDiferencia(m.diferenciaTotal),
                System.getProperty("java.version"), System.getProperty("os.name"), Runtime.getRuntime().availableProcessors());
    }

    private static String textoDiferencia(long diferencia) {
        return diferencia == 0 ? "ninguna" : diferencia + " peso(s), por el truncamiento de la versión heredada (D1)";
    }

    private static String html(Medicion m) {
        double pctTiempo = Math.max(1.5, 100.0 * m.msModerno / m.msLegado);
        double pctConsultas = Math.max(1.5, 100.0 * m.consultasModerno / m.consultasLegado);
        Defectos d = m.defectos;
        return """
                <!doctype html>
                <html lang="es">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Informe de modernización · Valdora (demostrativo)</title>
                <style>
                :root{--navy:#0a1f33;--navy2:#16395c;--teal:#0d8a7c;--teal2:#12a594;--soft:#e8f6f4;--amber:#fff6e8;--red:#b42318;--ink:#16222f;--muted:#566476;--line:#dde4ec}
                *{box-sizing:border-box}body{margin:0;font:15px/1.55 "Segoe UI",system-ui,Arial,sans-serif;color:var(--ink);background:#f3f6f9}
                .aviso{background:var(--amber);color:#6b4500;text-align:center;font-size:.85rem;padding:7px 16px;border-bottom:1px solid #f3dcb5}
                header{background:linear-gradient(135deg,var(--navy),var(--navy2));color:#fff;padding:36px 0 30px}
                .c{width:min(100%% - 32px,1100px);margin:auto}h1{margin:0 0 6px;font-size:1.9rem}header p{margin:0;color:#cfe0ee}
                .kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin:-22px auto 22px}
                .kpi{background:#fff;border:1px solid var(--line);border-top:3px solid var(--teal2);border-radius:12px;padding:16px 18px}
                .kpi b{display:block;font-size:1.7rem}.kpi span{color:var(--muted);font-size:.85rem}
                section{background:#fff;border:1px solid var(--line);border-radius:12px;padding:20px 22px;margin-bottom:18px}
                h2{margin:0 0 12px;font-size:1.1rem}table{width:100%%;border-collapse:collapse}th,td{text-align:left;padding:9px 10px;border-bottom:1px solid var(--line);vertical-align:top}
                th{font-size:.78rem;text-transform:uppercase;letter-spacing:.05em;color:var(--muted)}.mal{color:var(--red);font-weight:700}.bien{color:var(--teal);font-weight:700}
                .barra{display:grid;grid-template-columns:150px 1fr 110px;gap:10px;align-items:center;margin:8px 0}.pista{background:#eef2f6;border-radius:99px;height:14px;overflow:hidden}
                .pista i{display:block;height:100%%;border-radius:99px}.v1{background:#c0392b}.v2{background:var(--teal2)}
                footer{color:var(--muted);font-size:.85rem;text-align:center;padding:10px 0 28px}
                @media(max-width:760px){.kpis{grid-template-columns:1fr 1fr}.barra{grid-template-columns:1fr}}
                </style>
                </head>
                <body>
                <div class="aviso"><b>Proyecto demostrativo — desarrollo propio, no asociado a un cliente real.</b> Los defectos fueron introducidos a propósito y no corresponden a incidentes reales.</div>
                <header><div class="c"><h1>Informe de modernización del cotizador</h1><p>Valdora (empresa ficticia) · versión heredada v1 frente a versión modernizada v2 · generado el %s</p></div></header>
                <div class="c">
                <div class="kpis">
                <div class="kpi"><span>Defectos corregidos</span><b>3 de 3</b></div>
                <div class="kpi"><span>Consultas al catálogo</span><b>%s → %s</b></div>
                <div class="kpi"><span>Tiempo por cotización</span><b>%s veces</b><span>más rápida</span></div>
                <div class="kpi"><span>Pruebas automatizadas</span><b>%s</b></div>
                </div>
                <section><h2>Defectos encontrados y corregidos (valores calculados en esta ejecución)</h2>
                <table><thead><tr><th>Defecto</th><th>Caso</th><th>v1 heredada</th><th>v2 modernizada</th></tr></thead><tbody>
                <tr><td><b>D1</b> · trunca en vez de redondear</td><td>5 × filtro de aire ($12.990)</td><td class="mal">$%s</td><td class="bien">$%s</td></tr>
                <tr><td><b>D2</b> · descuento de cliente frecuente sobre el total</td><td>10 visitas, cliente frecuente y urgente</td><td class="mal">$%s</td><td class="bien">$%s</td></tr>
                <tr><td><b>D3</b> · SKU inexistente</td><td>«XXX-9999»</td><td class="mal">%s</td><td class="bien">%s</td></tr>
                </tbody></table></section>
                <section><h2>Rendimiento con %s líneas y %s productos</h2>
                <div class="barra"><span>Tiempo v1</span><div class="pista"><i class="v1" style="width:100%%"></i></div><b>%s ms</b></div>
                <div class="barra"><span>Tiempo v2</span><div class="pista"><i class="v2" style="width:%s%%"></i></div><b>%s ms</b></div>
                <div class="barra"><span>Consultas v1</span><div class="pista"><i class="v1" style="width:100%%"></i></div><b>%s</b></div>
                <div class="barra"><span>Consultas v2</span><div class="pista"><i class="v2" style="width:%s%%"></i></div><b>%s</b></div>
                <p>Mismo neto: <b>%s</b> · mismo detalle: <b>%s</b> · diferencia en el total: <b>%s</b>.</p></section>
                <section><h2>Calidad del código</h2><table><tbody>
                <tr><td>Líneas del método principal</td><td class="mal">%d</td><td class="bien">%d</td></tr>
                <tr><td>Dinero</td><td class="mal">double (errores de redondeo)</td><td class="bien">Dinero (long, redondeo comercial)</td></tr>
                <tr><td>Reglas comerciales</td><td class="mal">números mágicos repartidos</td><td class="bien">ReglasComerciales con nombre</td></tr>
                <tr><td>Folio</td><td class="mal">contador estático, no seguro con varios hilos</td><td class="bien">GeneradorFolio con AtomicLong</td></tr>
                </tbody></table></section>
                </div>
                <footer>Java %s · %s · medición real con Rendimiento.java · showcase-modernizacion-software</footer>
                </body>
                </html>
                """.formatted(LocalDate.now(), miles(m.consultasLegado), miles(m.consultasModerno),
                decimal(m.msLegado / m.msModerno), m.pruebas,
                miles(d.d1Legado), miles(d.d1Moderno), miles(d.d2Legado), miles(d.d2Moderno), d.d3Legado, d.d3Moderno,
                miles(LINEAS), miles(PRODUCTOS),
                decimal(m.msLegado), String.format(Locale.ROOT, "%.1f", pctTiempo), decimal(m.msModerno),
                miles(m.consultasLegado), String.format(Locale.ROOT, "%.1f", pctConsultas), miles(m.consultasModerno),
                m.netoIgual ? "sí" : "no", m.detalleIgual ? "sí" : "no", textoDiferencia(m.diferenciaTotal),
                m.lineasLegado, m.lineasModerno,
                System.getProperty("java.version"), System.getProperty("os.name"));
    }

    private record Medicion(long consultasLegado, long consultasModerno, double msLegado, double msModerno,
                            boolean netoIgual, boolean detalleIgual, long diferenciaTotal,
                            int lineasLegado, int lineasModerno, String pruebas, Defectos defectos) {
    }

    /** Recalcula en vivo los casos de los defectos documentados. */
    private record Defectos(long d1Legado, long d1Moderno, long d2Legado, long d2Moderno, String d3Legado, String d3Moderno) {

        static Defectos calcular() {
            CatalogoEnLista catalogo = CatalogoEnLista.deDemostracion();
            CotizadorLegado legado = new CotizadorLegado(catalogo);
            ServicioCotizacion moderno = new ServicioCotizacion(catalogo, evento -> { }, GeneradorFolio.secuencial());
            long d1v1 = Long.parseLong(legado.cotizar(List.<String[]>of(new String[] {"REP-0005", "5"}), false, false)[4]);
            long d1v2 = moderno.cotizar(new SolicitudCotizacion(List.of(new LineaCotizacion("REP-0005", 5)), false, false)).total().pesos();
            long d2v1 = Long.parseLong(legado.cotizar(List.<String[]>of(new String[] {"MAN-0001", "10"}), true, true)[4]);
            long d2v2 = moderno.cotizar(new SolicitudCotizacion(List.of(new LineaCotizacion("MAN-0001", 10)), true, true)).total().pesos();
            String d3v1;
            try {
                legado.cotizar(List.<String[]>of(new String[] {"XXX-9999", "1"}), false, false);
                d3v1 = "sin error";
            } catch (RuntimeException e) {
                d3v1 = e.getClass().getSimpleName();
            }
            String d3v2;
            try {
                moderno.cotizar(new SolicitudCotizacion(List.of(new LineaCotizacion("XXX-9999", 1)), false, false));
                d3v2 = "sin error";
            } catch (RuntimeException e) {
                d3v2 = e.getMessage();
            }
            return new Defectos(d1v1, d1v2, d2v1, d2v2, d3v1, d3v2);
        }
    }
}
