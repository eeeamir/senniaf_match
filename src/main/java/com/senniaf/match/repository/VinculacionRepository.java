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
    @Query("select count(v)>0 from Vinculacion v where v.nino.codigoCaso=:codigo and v.reservaVigente=true and v.ninoLiberado=false")
    boolean existsByNinoCodigoCasoAndReservaVigenteTrue(@Param("codigo") String codigo);
    @Query("select count(v)>0 from Vinculacion v where v.familia.codigoCaso=:codigo and v.reservaVigente=true and v.familiaLiberada=false")
    boolean existsByFamiliaCodigoCasoAndReservaVigenteTrue(@Param("codigo") String codigo);
}
