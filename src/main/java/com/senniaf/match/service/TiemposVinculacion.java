package com.senniaf.match.service;

import com.senniaf.match.model.Vinculacion;
import java.time.*;
import java.time.temporal.ChronoUnit;

final class TiemposVinculacion {
    static final ZoneId PANAMA = ZoneId.of("America/Panama");
    static final int DIAS_REFERENCIA_ACOGIMIENTO = 30;
    static final int VISITAS_REFERENCIA_ACOGIMIENTO = 3;

    record Indicadores(Long vinculacion, Long respuesta, Long autorizacion, Long inicio, Long convivencia,
                       LocalDate fechaInicio, int diasReferencia, boolean convivenciaInterrumpida,
                       boolean convivenciaFinalizada) {}

    static Indicadores calcular(Vinculacion vinculacion, String respuesta, boolean rechazadaDefinitiva,
                                boolean interrumpida, LocalDate fechaInterrupcion) {
        LocalDate hoy = LocalDate.now(PANAMA);
        LocalDate propuesta = fechaCivil(vinculacion.getDecision().getFecha());
        LocalDate fechaRespuesta = fechaCivil(vinculacion.getFechaRespuesta());
        LocalDate emision = vinculacion.getFechaEmisionAutorizacion();
        LocalDate inicio = vinculacion.getFechaInicioConvivencia();
        boolean cerrada = vinculacion.getEstado() == Vinculacion.Estado.CERRADA;
        boolean pendiente = vinculacion.getEstado() == Vinculacion.Estado.PENDIENTE_RESPUESTA;
        LocalDate finVinculacion = cerrada ? vinculacion.getFechaCierre() : rechazadaDefinitiva ? fechaRespuesta : hoy;
        LocalDate finConvivencia = interrumpida ? fechaInterrupcion : cerrada ? vinculacion.getFechaCierre() : hoy;
        if (interrumpida && cerrada && fechaInterrupcion != null && vinculacion.getFechaCierre() != null
                && vinculacion.getFechaCierre().isBefore(fechaInterrupcion)) {
            finConvivencia = vinculacion.getFechaCierre();
        }
        return new Indicadores(dias(propuesta, finVinculacion, hoy),
                dias(propuesta, pendiente ? hoy : fechaRespuesta, hoy),
                "ACEPTADA".equals(respuesta) ? dias(fechaRespuesta, emision, hoy) : null,
                dias(emision, inicio, hoy), dias(inicio, finConvivencia, hoy), inicio,
                DIAS_REFERENCIA_ACOGIMIENTO, interrumpida, cerrada);
    }

    static LocalDate fechaCivil(LocalDateTime fecha) {
        return fecha == null ? null : fecha.atZone(ZoneId.systemDefault()).withZoneSameInstant(PANAMA).toLocalDate();
    }

    private static Long dias(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        return desde == null || hasta == null || hasta.isBefore(desde) || hasta.isAfter(hoy)
                ? null : ChronoUnit.DAYS.between(desde, hasta);
    }

    private TiemposVinculacion() {}
}
