package com.senniaf.match.repository;

import com.senniaf.match.model.PerfilNino;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PerfilNinoRepository extends JpaRepository<PerfilNino, Long> {
    Optional<PerfilNino> findByCodigoCaso(String codigoCaso);
    boolean existsByCodigoCaso(String codigoCaso);
}
