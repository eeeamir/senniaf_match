package com.senniaf.match.service;
import com.senniaf.match.model.RegistroAuditoria;
import com.senniaf.match.repository.RegistroAuditoriaRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class AuditoriaService{
    private final RegistroAuditoriaRepository repo;
    public AuditoriaService(RegistroAuditoriaRepository repo){this.repo=repo;}
    public void registrar(String actor, String accion, String codigo, String detalle){
       RegistroAuditoria e=new RegistroAuditoria(); e.setFecha(LocalDateTime.now()); e.setActor(actor); e.setAccion(accion); e.setCodigoCaso(codigo); e.setDetalle(detalle); repo.save(e);
    }
    public List<RegistroAuditoria> porCaso(String codigo){return repo.findByCodigoCasoOrderByFechaDesc(codigo);}
}
