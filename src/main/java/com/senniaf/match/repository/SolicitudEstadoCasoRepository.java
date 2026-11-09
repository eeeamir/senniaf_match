package com.senniaf.match.repository;

import com.senniaf.match.model.SolicitudEstadoCaso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SolicitudEstadoCasoRepository extends JpaRepository<SolicitudEstadoCaso, Long> {
    long countByExpedienteIdAndEstado(Long id, SolicitudEstadoCaso.Estado estado);
    List<SolicitudEstadoCaso> findByEstadoOrderByFechaSolicitudAscIdAsc(SolicitudEstadoCaso.Estado estado);
    boolean existsByExpedienteIdAndEstado(Long id, SolicitudEstadoCaso.Estado estado);
    List<SolicitudEstadoCaso> findByExpedienteCodigoCasoOrderByFechaSolicitudDescIdDesc(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudEstadoCaso s where s.id = :id")
    Optional<SolicitudEstadoCaso> bloquear(@Param("id") Long id);
}
