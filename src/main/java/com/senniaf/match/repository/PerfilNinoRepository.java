package com.senniaf.match.repository;

import com.senniaf.match.model.PerfilNino;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PerfilNinoRepository extends JpaRepository<PerfilNino, Long> {
    boolean existsByNumeroExpedienteInstitucional(String numero);
    // Serializa únicamente las altas hasta commit, conservando la generación NNA-XXX.
    @org.springframework.data.jpa.repository.Query(value="select 1 from pg_advisory_xact_lock(7362112201)", nativeQuery=true)
    Integer bloquearAltas();
    Optional<PerfilNino> findByCodigoCaso(String codigoCaso);
    long countByPsicologoAsignado(String identificador);
    boolean existsByCodigoCaso(String codigoCaso);
    @org.springframework.data.jpa.repository.Query("""
            select p from PerfilNino p where (:incluir = true or p.bajaLogica = false)
            and ((:rol = 'TRABAJADOR_SOCIAL' and p.trabajadorSocialAsignado = :actor)
              or (:rol = 'PSICOLOGO' and p.psicologoAsignado = :actor))
            order by p.fechaRegistro desc, p.id desc
            """)
    java.util.List<PerfilNino> findAsignados(@org.springframework.data.repository.query.Param("actor") String actor,
            @org.springframework.data.repository.query.Param("rol") String rol,
            @org.springframework.data.repository.query.Param("incluir") boolean incluir);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PerfilNino p where p.codigoCaso = :codigo")
    Optional<PerfilNino> bloquear(@org.springframework.data.repository.query.Param("codigo") String codigo);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PerfilNino p where p.id = :id")
    Optional<PerfilNino> bloquearPorId(@org.springframework.data.repository.query.Param("id") Long id);
}
