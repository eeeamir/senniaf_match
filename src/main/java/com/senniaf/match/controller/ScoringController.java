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
    public ScoringController(ScoringService scoring) { this.scoring = scoring; }
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
    @GetMapping("/pesos")
    public PesosScoring pesos(@RequestHeader("X-User-Role") String rol) {
        comite(rol); return scoring.pesos();
    }
    @PutMapping("/pesos")
    public PesosScoring pesos(@RequestHeader("X-User-Role") String rol, @RequestBody Map<String, Integer> valores) {
        comite(rol); return scoring.actualizarPesos(valores);
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
