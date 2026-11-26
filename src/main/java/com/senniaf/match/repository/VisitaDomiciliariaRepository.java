package com.senniaf.match.repository;

import com.senniaf.match.model.VisitaDomiciliaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public interface VisitaDomiciliariaRepository extends JpaRepository<VisitaDomiciliaria, Long> {
    List<VisitaDomiciliaria> findByVinculacionIdOrderByFechaAscInicioAscIdAsc(Long vinculacionId);
    Optional<VisitaDomiciliaria> findByVinculacionIdAndId(Long vinculacionId, Long id);
    Optional<VisitaDomiciliaria> findByVinculacionIdAndAutorAndClaveRegistro(Long vinculacionId, String autor, UUID claveRegistro);

    @Query("""
            select count(v) > 0 from VisitaDomiciliaria v
            where v.vinculacionId = :vinculacion and v.fecha = :fecha and v.id <> :excluir
                and v.inicio < :fin and v.fin > :inicio
            """)
    boolean existeSuperposicion(Long vinculacion, LocalDate fecha, LocalTime inicio, LocalTime fin, Long excluir);
}
