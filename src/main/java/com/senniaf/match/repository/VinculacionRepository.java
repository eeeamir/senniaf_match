package com.senniaf.match.repository;

import com.senniaf.match.model.Vinculacion;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface VinculacionRepository extends JpaRepository<Vinculacion,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vinculacion v where v.id=:id")
    Optional<Vinculacion> bloquear(@Param("id") Long id);
    List<Vinculacion> findByFamiliaRegistradoPorOrderByIdDesc(String actor);
    List<Vinculacion> findAllByOrderByIdDesc();
    boolean existsByNinoCodigoCasoAndReservaVigenteTrue(String codigo);
    boolean existsByFamiliaCodigoCasoAndReservaVigenteTrue(String codigo);
}
