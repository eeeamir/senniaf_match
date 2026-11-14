package com.senniaf.match.repository;
import com.senniaf.match.model.ProgramacionPesos;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProgramacionPesosRepository extends JpaRepository<ProgramacionPesos,Long>{
 Optional<ProgramacionPesos> findFirstByEstadoOrderByIdAsc(String estado);
}
