package com.senniaf.match.repository;

import com.senniaf.match.model.PerfilFamilia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PerfilFamiliaRepository extends JpaRepository<PerfilFamilia, Long> {
    @org.springframework.data.jpa.repository.Query(value="select 1 from pg_advisory_xact_lock(7362112202)", nativeQuery=true)
    Integer bloquearAltas();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PerfilFamilia p where p.codigoCaso=:codigo")
    Optional<PerfilFamilia> bloquear(@org.springframework.data.repository.query.Param("codigo") String codigo);
    Optional<PerfilFamilia> findByCodigoCaso(String codigoCaso);
    boolean existsByCodigoCaso(String codigoCaso);
    Optional<PerfilFamilia> findByRegistradoPor(String cedula);
    boolean existsByRegistradoPor(String cedula);
}
