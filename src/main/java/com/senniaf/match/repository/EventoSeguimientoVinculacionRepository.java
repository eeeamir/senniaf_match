package com.senniaf.match.repository;

import com.senniaf.match.model.EventoSeguimientoVinculacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface EventoSeguimientoVinculacionRepository extends JpaRepository<EventoSeguimientoVinculacion, Long> {
    List<EventoSeguimientoVinculacion> findByVinculacionIdAndRolInOrderByFechaDescIdDesc(Long vinculacionId, Collection<String> roles);
}
