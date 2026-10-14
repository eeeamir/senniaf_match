package com.senniaf.match.repository;

import com.senniaf.match.model.DecisionComite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DecisionComiteRepository extends JpaRepository<DecisionComite, Long> {
    List<DecisionComite> findByCodigoNinoOrderByFechaAscIdAsc(String codigoNino);
    List<DecisionComite> findByDecision(String decision);
    Optional<DecisionComite> findFirstByCodigoFamiliaAndDecisionOrderByFechaDescIdDesc(String codigoFamilia, String decision);
    Optional<DecisionComite> findFirstByCodigoFamiliaOrderByFechaDescIdDesc(String codigoFamilia);
    boolean existsByCodigoFamiliaAndDecisionAndCodigoNinoNot(String codigoFamilia, String decision, String codigoNino);
}
