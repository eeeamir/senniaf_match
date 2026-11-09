package com.senniaf.match.repository;

import com.senniaf.match.model.PesosScoring;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesosScoringRepository extends JpaRepository<PesosScoring, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "insert into pesos_scoring (id,edad,hermanos,salud,apoyo,logistica,version) values (1,20,25,35,15,5,1) on conflict (id) do nothing", nativeQuery = true)
    void inicializarSiFalta();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PesosScoring p where p.id=1")
    java.util.Optional<PesosScoring> bloquear();
}
