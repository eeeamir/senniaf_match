package com.senniaf.match.repository;

import com.senniaf.match.model.EvaluacionPsicologica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvaluacionPsicologicaRepository extends JpaRepository<EvaluacionPsicologica, Long> {
    boolean existsByRevisionDeId(Long id);
    List<EvaluacionPsicologica> findByExpedienteCodigoCasoOrderByFechaDescIdDesc(String codigo);
}
