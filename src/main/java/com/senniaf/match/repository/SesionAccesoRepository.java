package com.senniaf.match.repository;

import com.senniaf.match.model.SesionAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SesionAccesoRepository extends JpaRepository<SesionAcceso, Long> {
    Optional<SesionAcceso> findByTokenHash(String tokenHash);
    void deleteByTokenHash(String tokenHash);
}
