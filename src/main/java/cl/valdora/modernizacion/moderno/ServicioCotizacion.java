package cl.valdora.modernizacion.moderno;

import cl.valdora.modernizacion.comun.CatalogoPrecios;
import cl.valdora.modernizacion.comun.RegistroAuditoria;
import cl.valdora.modernizacion.moderno.Modelo.Cotizacion;
import cl.valdora.modernizacion.moderno.Modelo.DetalleLinea;
import cl.valdora.modernizacion.moderno.Modelo.LineaCotizacion;
import cl.valdora.modernizacion.moderno.Modelo.ProductoNoEncontradoException;
import cl.valdora.modernizacion.moderno.Modelo.SolicitudCotizacion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cl.valdora.modernizacion.moderno.ReglasComerciales.DESCUENTO_CLIENTE_FRECUENTE;
import static cl.valdora.modernizacion.moderno.ReglasComerciales.RECARGO_URGENCIA;
import static cl.valdora.modernizacion.moderno.ReglasComerciales.TASA_IVA;

/** VERSIÓN MODERNIZADA (v2) del cotizador. */
public class ServicioCotizacion {

    private final CatalogoPrecios catalogo;
    private final RegistroAuditoria auditoria;
    private final GeneradorFolio folios;

    public ServicioCotizacion(CatalogoPrecios catalogo, RegistroAuditoria auditoria, GeneradorFolio folios) {
        this.catalogo = catalogo;
        this.auditoria = auditoria;
        this.folios = folios;
    }

    public Cotizacion cotizar(SolicitudCotizacion solicitud) {
        Map<String, Dinero> precios = resolverPrecios(solicitud.lineas());
        List<DetalleLinea> detalle = solicitud.lineas().stream()
                .map(l -> new DetalleLinea(l.sku(), l.cantidad(), precios.get(l.sku()), precios.get(l.sku()).por(l.cantidad())))
                .toList();
        Dinero neto = detalle.stream().map(DetalleLinea::subtotal).reduce(Dinero.CERO, Dinero::mas);
        Dinero descuentoVolumen = neto.porcentaje(ReglasComerciales.tasaDescuentoVolumen(neto));
        Dinero descuentoFrecuente = solicitud.clienteFrecuente() ? neto.porcentaje(DESCUENTO_CLIENTE_FRECUENTE) : Dinero.CERO;
        Dinero recargo = solicitud.urgente() ? neto.porcentaje(RECARGO_URGENCIA) : Dinero.CERO;
        Dinero base = neto.menos(descuentoVolumen).menos(descuentoFrecuente).mas(recargo);
        Dinero iva = base.porcentaje(TASA_IVA);
        Cotizacion cotizacion = new Cotizacion(folios.siguiente(), detalle, neto, descuentoVolumen, descuentoFrecuente,
                recargo, base, iva, base.mas(iva));
        auditoria.registrar("Cotización " + cotizacion.folio() + " emitida por " + cotizacion.total());
        return cotizacion;
    }

    /** Una sola consulta al catálogo por SKU distinto, aunque se repita en varias líneas. */
    private Map<String, Dinero> resolverPrecios(List<LineaCotizacion> lineas) {
        Map<String, Dinero> precios = new HashMap<>();
        for (LineaCotizacion linea : lineas) {
            precios.computeIfAbsent(linea.sku(), sku -> catalogo.precioUnitario(sku)
                    .map(Dinero::pesos)
                    .orElseThrow(() -> new ProductoNoEncontradoException(sku)));
        }
        return precios;
    }
}
