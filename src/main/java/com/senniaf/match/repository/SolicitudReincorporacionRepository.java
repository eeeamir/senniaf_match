package com.senniaf.match.repository;

import com.senniaf.match.model.SolicitudReincorporacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudReincorporacionRepository extends JpaRepository<SolicitudReincorporacion, Long> {
    @org.springframework.data.jpa.repository.Query("select s.vinculacion.id from SolicitudReincorporacion s where s.id = :id")
    Optional<Long> vinculacionId(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select s from SolicitudReincorporacion s where s.id = :id")
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<SolicitudReincorporacion> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<SolicitudReincorporacion> findByVinculacionId(Long vinculacionId);
    List<SolicitudReincorporacion> findAllByOrderBySolicitadaDescIdDesc();
}
