package com.senniaf.match.repository;

import com.senniaf.match.model.CitaHistorial;
import java.util.List;
import org.springframework.data.repository.Repository;

/** Solo inserción y lectura; la migración protege también contra UPDATE/DELETE. */
public interface CitaHistorialRepository extends Repository<CitaHistorial,Long> {
    boolean existsByCitaIdAndAccion(Long cita, String accion);
    CitaHistorial save(CitaHistorial evento);
    List<CitaHistorial> findByCitaIdOrderByIdAsc(Long cita);
}
