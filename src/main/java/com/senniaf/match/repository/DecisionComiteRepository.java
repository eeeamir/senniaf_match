package com.senniaf.match.repository;

import com.senniaf.match.model.DecisionComite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DecisionComiteRepository extends JpaRepository<DecisionComite, Long> {
    List<DecisionComite> findByCodigoNinoOrderByFechaAsc(String codigoNino);
    boolean existsByCodigoNinoAndDecision(String codigoNino, String decision);
}
