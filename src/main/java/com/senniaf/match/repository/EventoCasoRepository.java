package com.senniaf.match.repository;

import com.senniaf.match.model.EventoCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventoCasoRepository extends JpaRepository<EventoCaso, Long> {
    List<EventoCaso> findByCodigoCasoOrderByFechaAsc(String codigoCaso);
}
