package com.senniaf.match.repository;
import com.senniaf.match.model.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria,Long>{
    List<RegistroAuditoria> findByCodigoCasoOrderByFechaDesc(String codigoCaso);
}
