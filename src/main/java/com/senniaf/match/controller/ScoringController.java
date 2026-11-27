package com.senniaf.match.controller;

import com.senniaf.match.model.PesosScoring;
import com.senniaf.match.security.RolNoAutorizadoException;
import com.senniaf.match.service.ScoringService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scoring")
public class ScoringController {
    private final ScoringService scoring;
    @org.springframework.beans.factory.annotation.Autowired
    private com.senniaf.match.service.VinculacionService vinculaciones;
    private final com.senniaf.match.service.PerfilFamiliaService familias;
    public ScoringController(ScoringService scoring) { this(scoring,null); }
    @org.springframework.beans.factory.annotation.Autowired
    public ScoringController(ScoringService scoring,com.senniaf.match.service.PerfilFamiliaService familias) { this.scoring = scoring;this.familias=familias; }
    @GetMapping("/reevaluaciones-pendientes")
    public List<String> pendientes(@RequestHeader("X-User-Role") String rol){comite(rol);return familias.reevaluacionesPendientes();}
    @GetMapping("/reevaluaciones")
    public List<Map<String,Object>> reevaluaciones(@RequestHeader("X-User-Role") String rol){comite(rol);return familias.alertasReevaluaciones();}
    private void comite(String rol) {
        if (!"COMITE".equals(rol)) throw new RolNoAutorizadoException("Solo el Comité puede revisar el motor");
    }
    @GetMapping("/casos")
    public List<Map<String, Object>> casos(@RequestHeader("X-User-Role") String rol) {
        comite(rol); return scoring.casos();
    }
    @GetMapping("/casos/{codigo}")
    public Map<String, Object> caso(@RequestHeader("X-User-Role") String rol, @PathVariable String codigo) {
        comite(rol); return scoring.caso(codigo);
    }
    @GetMapping("/casos/{codigo}/historial")
    public List<Map<String,Object>> historial(@RequestHeader("X-User-Role") String rol, @PathVariable String codigo) {
        comite(rol); return scoring.historial(codigo);
    }
    @GetMapping("/familias/{codigo}/expediente")
    public Map<String,Object> expedienteFamilia(@RequestHeader("X-User-Role") String rol,
                                                                 @PathVariable String codigo) {
        comite(rol);
        var perfil=scoring.consultarExpedienteFamilia(codigo);
        Map<String,Object> r=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).convertValue(perfil,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
        r.put("antecedenteInterrupcion",scoring.antecedenteInterrupcion(codigo));
        if (vinculaciones != null) {
            r.put("antecedenteRevocacion", vinculaciones.antecedenteRevocacion(perfil.getId()));
            r.put("antecedenteAdopcion", vinculaciones.antecedenteAdopcion(perfil.getId()));
        }
        if(perfil.isReevaluacionPendiente() && familias!=null)familias.consultarPorCedula(perfil.getRegistradoPor()).ifPresent(revision->{
            r.put("apartadosReevaluacion",revision.getApartadosReevaluacion());
            r.put("observacionReevaluacion",revision.getObservacionReevaluacion());
        });
        return r;
    }
    @GetMapping("/pesos")
    public PesosScoring pesos(@RequestHeader("X-User-Role") String rol) {
        comite(rol); return scoring.pesos();
    }
    @PutMapping("/pesos")
    public Map<String,Object> pesos(@RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor, @RequestBody Map<String, Object> valores) {
        comite(rol);
        if(valores==null || !(valores.get("justificacion") instanceof String justificacion)
                || !(valores.get("versionEsperada") instanceof Integer version))
            throw new IllegalArgumentException("Confirma los criterios e indica la justificación del cambio");
        Map<String,Integer> enteros = new java.util.LinkedHashMap<>();
        valores.forEach((k,v) -> {
            if(k.equals("justificacion") || k.equals("versionEsperada"))return;
            if (!(v instanceof Integer)) throw new IllegalArgumentException("Los pesos deben ser números enteros, sin decimales ni texto");
            enteros.put(k,(Integer)v);
        });
        return scoring.actualizarPesos(enteros,actor,justificacion,version);
    }
    @GetMapping("/pesos/programacion")
    public Map<String,Object> programacion(@RequestHeader("X-User-Role") String rol){comite(rol);return scoring.programacion();}
    @GetMapping("/pesos/historial")
    public List<Map<String,Object>> historialPesos(@RequestHeader("X-User-Role") String rol) {
        comite(rol); return scoring.historialPesos();
    }
    public record DecisionRequest(String familia, String decision, String motivo, @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.senniaf.match.dto.PerfilNinoRequest.EnteroEstricto.class) Integer versionPesos, List<String> apartados, String categoria) {}
    @PostMapping("/casos/{codigo}/decisiones")
    public Map<String, Object> decidir(@RequestHeader("X-User-Role") String rol,
                                       @RequestHeader("X-User-Id") String funcionario,
                                       @PathVariable String codigo, @RequestBody DecisionRequest request) {
        comite(rol);
        if (request == null || request.familia() == null || request.decision() == null)
            throw new IllegalArgumentException("Se requiere familia y decisión");
        if("REEVALUACION".equals(request.decision()) && (request.apartados()==null || request.categoria()==null))throw new IllegalArgumentException("Selecciona apartados y motivo de reevaluación");
        return scoring.decidir(codigo, request.familia(), request.decision(), request.motivo(), funcionario,request.versionPesos(),request.apartados(),request.categoria());
    }
}
