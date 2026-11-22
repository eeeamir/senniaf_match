package com.senniaf.match.repository;

import com.senniaf.match.model.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CitaVinculacionRepository extends JpaRepository<CitaVinculacion,Long> {
    @Query("select v from Vinculacion v where v.nino.trabajadorSocialAsignado=:actor order by v.id desc")
    List<Vinculacion> vinculacionesDelProfesional(@Param("actor") String actor);
    @EntityGraph(attributePaths={"vinculacion","vinculacion.familia","vinculacion.nino"})
    List<CitaVinculacion> findByVinculacionFamiliaRegistradoPorOrderByActualizadaDescIdDesc(String actor);
    @EntityGraph(attributePaths={"vinculacion","vinculacion.familia","vinculacion.nino"})
    List<CitaVinculacion> findByVinculacionNinoTrabajadorSocialAsignadoOrderByActualizadaDescIdDesc(String actor);
    // Bloqueos escalares: no propagan FOR UPDATE a decisiones o evaluaciones históricas.
    @Query(value="select id from citas_vinculacion where id=:id for update",nativeQuery=true)
    Optional<Long> bloquearSolicitud(@Param("id") Long id);
    @Query(value="select id from vinculaciones where id=:id for update",nativeQuery=true)
    Optional<Long> bloquearVinculo(@Param("id") Long id);
    @Query(value="select id from perfiles_nino where id=:id for update",nativeQuery=true)
    Optional<Long> bloquearExpediente(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.identificador=:actor and u.rol='TRABAJADOR_SOCIAL' and u.verificado=true")
    Optional<Usuario> bloquearAgenda(@Param("actor") String actor);
    @Query("select count(c)>0 from CitaVinculacion c where c.vinculacion.id=:vin and c.estado in (com.senniaf.match.model.CitaVinculacion.Estado.PENDIENTE,com.senniaf.match.model.CitaVinculacion.Estado.CAMBIO_PROPUESTO)")
    boolean existeAbierta(@Param("vin") Long vin);
    @Query("select count(c)>0 from CitaVinculacion c where c.profesional=:prof and c.estado=com.senniaf.match.model.CitaVinculacion.Estado.CONFIRMADA and c.inicio<:fin and c.fin>:inicio and (:excluir is null or c.id<>:excluir)")
    boolean hayConflicto(@Param("prof") String profesional,@Param("inicio") LocalDateTime inicio,
                        @Param("fin") LocalDateTime fin,@Param("excluir") Long excluir);
}
