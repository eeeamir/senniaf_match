package com.senniaf.match.service;
import com.senniaf.match.dto.*;
import com.senniaf.match.model.PerfilNino;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.PerfilNinoRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class PerfilNinoService {
    private final PerfilNinoRepository repo;
    private final AuditoriaService auditoria;
    public PerfilNinoService(PerfilNinoRepository repo, AuditoriaService auditoria){this.repo=repo;this.auditoria=auditoria;}
    @Transactional
    public synchronized PerfilNinoResponse registrarPerfil(PerfilNinoRequest r,String actor){
        PerfilNino p=new PerfilNino();
        // El código no se reutiliza si otro caso se eliminó.
        String codigo;
        do {codigo="NNA-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);}while(repo.existsByCodigoCaso(codigo));
        p.setCodigoCaso(codigo);p.setRegistradoPor(actor);p.setFechaRegistro(LocalDateTime.now());
        aplicar(p,r);repo.save(p);auditoria.registrar(actor,"CREAR_PERFIL",codigo,"Registro del perfil integral");
        return respuesta(p,"Perfil registrado correctamente");
    }
    public List<PerfilNino> listarPerfiles(String actor){return repo.findByRegistradoPorAndEliminadoFalseOrderByFechaRegistroDesc(actor);}
    @Transactional(readOnly=true)
    public PerfilNino consultar(String codigo,String actor){return propietario(codigo,actor);}
    @Transactional
    public PerfilNinoResponse editar(String codigo, PerfilNinoRequest r, String actor){
        PerfilNino p=propietario(codigo,actor);aplicar(p,r);repo.save(p);
        auditoria.registrar(actor,"EDITAR_PERFIL",codigo,"Actualización del perfil integral");
        return respuesta(p,"Perfil actualizado correctamente");
    }
    @Transactional
    public void eliminar(String codigo,String actor){
        PerfilNino p=propietario(codigo,actor);p.setEliminado(true);repo.save(p);
        auditoria.registrar(actor,"ELIMINAR_PERFIL",codigo,"Baja lógica del caso, excluido del scoring");
    }
    private PerfilNino propietario(String codigo,String actor){
        PerfilNino p=repo.findByCodigoCasoAndEliminadoFalse(codigo).orElseThrow(()->new NoSuchElementException("Caso no encontrado."));
        if(!p.getRegistradoPor().equals(actor)) throw new com.senniaf.match.security.RolNoAutorizadoException("Solo puedes gestionar los perfiles que registraste.");
        return p;
    }
    private void aplicar(PerfilNino p,PerfilNinoRequest r){
        if(r.getFechaDeclaratoria()!=null && r.getFechaDeclaratoria().isAfter(LocalDate.now()))
           throw new IllegalArgumentException("La fecha de declaratoria no puede ser futura.");
        p.setFechaDeclaratoria(r.getFechaDeclaratoria()==null?null:r.getFechaDeclaratoria().atStartOfDay());
        p.setEdad(r.getEdad());p.setNivelEscolar(r.getNivelEscolar());p.setGradoEscolar(r.getGradoEscolar());
        p.setRendimientoAcademico(r.getRendimientoAcademico());p.setHabilidades(r.getHabilidades());
        p.setHabilidadesDetalle(r.getHabilidadesDetalle());p.setGustos(r.getGustos());p.setGustosDetalle(r.getGustosDetalle());
        p.setPersonalidad(r.getPersonalidad());p.setPersonalidadDetalle(r.getPersonalidadDetalle());
        p.setAlergias(r.getAlergias());p.setAlergiasDetalle(r.getAlergiasDetalle());p.setIntoleranciaLactosa(r.getIntoleranciaLactosa());
        p.setCondicionesSalud(r.getCondicionesSalud());p.setCondicionesSaludDetalle(r.getCondicionesSaludDetalle());
        p.setNecesidadesApoyo(r.getNecesidadesApoyo());p.setTieneHermanos(r.getTieneHermanos());
        p.setCantidadHermanos(r.getCantidadHermanos());p.setHermanosDetalle(r.getHermanosDetalle());
        p.setContextoCultural(r.getContextoCultural());
        p.setPerfilCompleto(r.getEdad()!=null && r.getNivelEscolar()!=null && r.getRendimientoAcademico()!=null
          && llena(r.getHabilidades()) && llena(r.getGustos()) && llena(r.getPersonalidad())
          && llena(r.getAlergias()) && r.getIntoleranciaLactosa()!=null && llena(r.getCondicionesSalud())
          && r.getNecesidadesApoyo()!=null && !r.getNecesidadesApoyo().isBlank() && r.getTieneHermanos()!=null);
        p.setAlertaMedica(relevante(r.getAlergias(),Alergia.NINGUNA_CONOCIDA)
          || relevante(r.getCondicionesSalud(),CondicionSalud.NINGUNA_CONOCIDA)
          || r.getIntoleranciaLactosa()==RespuestaSiNoNoSabe.SI);
    }
    private boolean llena(List<?> lista){return lista!=null && !lista.isEmpty();}
    private <T> boolean relevante(List<T> lista,T ninguno){return lista!=null && lista.stream().anyMatch(c->c!=ninguno);}
    private PerfilNinoResponse respuesta(PerfilNino p,String mensaje){
        return new PerfilNinoResponse(mensaje+(p.isPerfilCompleto()?"":" (incompleto, no se incluirá en el scoring)")+(p.isAlertaMedica()?". Requiere atención del Comité por una necesidad médica declarada.":""),p.getCodigoCaso(),p.isPerfilCompleto(),p.isAlertaMedica());
    }
}
