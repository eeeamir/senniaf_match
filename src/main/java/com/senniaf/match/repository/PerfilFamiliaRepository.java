package com.senniaf.match.repository;

import com.senniaf.match.model.PerfilFamilia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PerfilFamiliaRepository extends JpaRepository<PerfilFamilia, Long> {
    Optional<PerfilFamilia> findByCodigoCaso(String codigoCaso);
    boolean existsByCodigoCaso(String codigoCaso);
    Optional<PerfilFamilia> findByRegistradoPor(String cedula);
    boolean existsByRegistradoPor(String cedula);
}
