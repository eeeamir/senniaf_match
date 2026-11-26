package com.senniaf.match.repository;

import com.senniaf.match.model.SeguimientoVinculacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SeguimientoVinculacionRepository extends JpaRepository<SeguimientoVinculacion, Long> {
    List<SeguimientoVinculacion> findByVinculacionIdAndRolInOrderByFechaAtencionDescIdDesc(Long vinculacionId, Collection<String> roles);
    Optional<SeguimientoVinculacion> findByVinculacionIdAndId(Long vinculacionId, Long id);
    Optional<SeguimientoVinculacion> findByVinculacionIdAndAutorAndClaveRegistro(Long vinculacionId, String autor, UUID claveRegistro);
}
