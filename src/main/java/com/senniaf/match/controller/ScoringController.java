package com.senniaf.match.controller;
import com.senniaf.match.service.*;
import com.senniaf.match.model.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/scoring")
public class ScoringController{
    private final ScoringService scoring;private final AutenticacionService auth;
    public ScoringController(ScoringService scoring,AutenticacionService auth){this.scoring=scoring;this.auth=auth;}
    private Usuario comite(String header){return auth.requerir(header,"COMITE");}
    @GetMapping("/casos") public Object casos(@RequestHeader(value="Authorization",required=false) String header){comite(header);return scoring.casos();}
    @GetMapping("/casos/{codigo}") public Object caso(@PathVariable String codigo,@RequestHeader(value="Authorization",required=false) String header){comite(header);return scoring.ranking(codigo);}
    @GetMapping("/pesos") public Object pesos(@RequestHeader(value="Authorization",required=false) String header){comite(header);return scoring.pesos();}
    @PutMapping("/pesos") public Object pesos(@RequestBody Map<String,Integer> pesos,@RequestHeader(value="Authorization",required=false) String header){return scoring.cambiarPesos(pesos,comite(header).getIdentificador());}
    public record DecisionRequest(String familia,String decision,String motivo){}
    @PostMapping("/casos/{codigo}/decisiones") public Object decidir(@PathVariable String codigo,@RequestBody DecisionRequest d,@RequestHeader(value="Authorization",required=false) String header){return scoring.decidir(codigo,d.familia(),d.decision(),d.motivo(),comite(header).getIdentificador());}
}
