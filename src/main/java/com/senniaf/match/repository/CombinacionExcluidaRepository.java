package com.senniaf.match.repository;
public interface CombinacionExcluidaRepository extends org.springframework.data.jpa.repository.JpaRepository<com.senniaf.match.model.CombinacionExcluida,Long>{
 boolean existsByCodigoNinoAndCodigoFamilia(String n,String f);
}
