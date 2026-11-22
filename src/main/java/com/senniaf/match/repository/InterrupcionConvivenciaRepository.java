package com.senniaf.match.repository;

import com.senniaf.match.model.InterrupcionConvivencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InterrupcionConvivenciaRepository extends JpaRepository<InterrupcionConvivencia,Long> {
    Optional<InterrupcionConvivencia> findByVinculacionId(Long id);
    java.util.List<InterrupcionConvivencia> findByVinculacionFamiliaCodigoCasoAndFechaRevisionIsNotNull(String codigo);
    boolean existsByVinculacionNinoCodigoCasoAndVinculacionFamiliaCodigoCaso(String nino,String familia);
    @org.springframework.data.jpa.repository.Query("select count(i)>0 from InterrupcionConvivencia i where i.vinculacion.nino.codigoCaso=:nino and i.vinculacion.familia.codigoCaso=:familia and (i.vinculacion.estado<>'CERRADA' or i.vinculacion.ninoLiberado=false or i.vinculacion.familiaLiberada=false)")
    boolean existeInterrupcionSinReingresoAutorizado(String nino,String familia);
    interface ParInterrumpido {String getNino();String getFamilia();}
    @org.springframework.data.jpa.repository.Query("select distinct i.vinculacion.nino.codigoCaso as nino,i.vinculacion.familia.codigoCaso as familia from InterrupcionConvivencia i where i.vinculacion.estado<>'CERRADA' or i.vinculacion.ninoLiberado=false or i.vinculacion.familiaLiberada=false")
    java.util.List<ParInterrumpido> paresConInterrupcionNoHabilitada();
}
