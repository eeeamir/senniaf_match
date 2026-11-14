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
    private final com.senniaf.match.service.PerfilFamiliaService familias;
    public ScoringController(ScoringService scoring) { this(scoring,null); }
    @org.springframework.beans.factory.annotation.Autowired
    public ScoringController(ScoringService scoring,com.senniaf.match.service.PerfilFamiliaService familias) { this.scoring = scoring;this.familias=familias; }
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
    public com.senniaf.match.model.PerfilFamilia expedienteFamilia(@RequestHeader("X-User-Role") String rol,
                                                                 @PathVariable String codigo) {
        comite(rol); return scoring.consultarExpedienteFamilia(codigo);
    }
    @GetMapping("/pesos")
    public PesosScoring pesos(@RequestHeader("X-User-Role") String rol) {
        comite(rol); return scoring.pesos();
    }
    @PutMapping("/pesos")
    public PesosScoring pesos(@RequestHeader("X-User-Role") String rol, @RequestHeader("X-User-Id") String actor, @RequestBody Map<String, Object> valores) {
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
    @GetMapping("/pesos/historial")
    public List<Map<String,Object>> historialPesos(@RequestHeader("X-User-Role") String rol) {
        comite(rol); return scoring.historialPesos();
    }
    public record DecisionRequest(String familia, String decision, String motivo) {}
    @PostMapping("/casos/{codigo}/decisiones")
    public Map<String, Object> decidir(@RequestHeader("X-User-Role") String rol,
                                       @RequestHeader("X-User-Id") String funcionario,
                                       @PathVariable String codigo, @RequestBody DecisionRequest request) {
        comite(rol);
        if (request == null || request.familia() == null || request.decision() == null)
            throw new IllegalArgumentException("Se requiere familia y decisión");
        return scoring.decidir(codigo, request.familia(), request.decision(), request.motivo(), funcionario);
    }
}
