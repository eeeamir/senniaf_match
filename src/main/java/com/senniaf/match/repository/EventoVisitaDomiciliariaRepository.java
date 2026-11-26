package com.senniaf.match.repository;

import com.senniaf.match.model.EventoVisitaDomiciliaria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventoVisitaDomiciliariaRepository extends JpaRepository<EventoVisitaDomiciliaria, Long> {
    List<EventoVisitaDomiciliaria> findByVisitaVinculacionIdOrderByFechaDescIdDesc(Long vinculacionId);
}
