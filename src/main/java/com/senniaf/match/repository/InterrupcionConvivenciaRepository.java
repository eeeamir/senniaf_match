package com.senniaf.match.repository;

import com.senniaf.match.model.InterrupcionConvivencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InterrupcionConvivenciaRepository extends JpaRepository<InterrupcionConvivencia,Long> {
    Optional<InterrupcionConvivencia> findByVinculacionId(Long id);
    java.util.List<InterrupcionConvivencia> findByVinculacionFamiliaCodigoCasoAndFechaRevisionIsNotNull(String codigo);
    boolean existsByVinculacionNinoCodigoCasoAndVinculacionFamiliaCodigoCaso(String nino,String familia);
    interface ParInterrumpido {String getNino();String getFamilia();}
    @org.springframework.data.jpa.repository.Query("select distinct i.vinculacion.nino.codigoCaso as nino,i.vinculacion.familia.codigoCaso as familia from InterrupcionConvivencia i")
    java.util.List<ParInterrumpido> paresInterrumpidos();
}
