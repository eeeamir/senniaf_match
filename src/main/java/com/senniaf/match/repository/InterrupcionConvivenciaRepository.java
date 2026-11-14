package com.senniaf.match.repository;

import com.senniaf.match.model.InterrupcionConvivencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InterrupcionConvivenciaRepository extends JpaRepository<InterrupcionConvivencia,Long> {
    Optional<InterrupcionConvivencia> findByVinculacionId(Long id);
}
