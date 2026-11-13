package cl.valdora.modernizacion.comun;

/** Puerto hacia el registro de auditoría (en un sistema real, un log centralizado o una tabla). */
public interface RegistroAuditoria {

    void registrar(String evento);
}
