package com.senniaf.match.repository;

import com.senniaf.match.model.IncidenciaAsignacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidenciaAsignacionRepository extends JpaRepository<IncidenciaAsignacion, Long> {
    List<IncidenciaAsignacion> findByVinculacionIdOrderByIdDesc(Long vinculacionId);
}
