package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import com.senniaf.match.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** Motor académico y explicable. Una puntuación jamás constituye una asignación legal. */
@Service
public class ScoringService {
    private final PerfilNinoRepository ninos;
    private final PerfilFamiliaRepository familias;
    private final DecisionComiteRepository decisiones;
    private final PesosScoringRepository pesosRepo;
    private final EventoCasoRepository auditoria;
    private final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();

    public ScoringService(PerfilNinoRepository ninos, PerfilFamiliaRepository familias,
                          DecisionComiteRepository decisiones, PesosScoringRepository pesosRepo) {
        this(ninos, familias, decisiones, pesosRepo, null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public ScoringService(PerfilNinoRepository ninos, PerfilFamiliaRepository familias,
                          DecisionComiteRepository decisiones, PesosScoringRepository pesosRepo, EventoCasoRepository auditoria) {
        this.ninos=ninos; this.familias=familias; this.decisiones=decisiones; this.pesosRepo=pesosRepo; this.auditoria=auditoria;
    }

    public PerfilFamilia consultarExpedienteFamilia(String codigo) {
        return familias.findByCodigoCaso(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El expediente familiar no existe"));
    }

    public PesosScoring pesos() { return pesosRepo.findById(1L).orElseGet(PesosScoring::new); }

    public PesosScoring actualizarPesos(Map<String,Integer> valores,String actor) {
        return actualizarPesos(valores,actor,null,null);
    }
    @Transactional
    public PesosScoring actualizarPesos(Map<String, Integer> valores, String actor,String justificacion,Integer versionEsperada) {
        if(justificacion==null || justificacion.strip().length()<10 || justificacion.length()>300)
            throw new IllegalArgumentException("Explica el cambio de criterios en 10 a 300 caracteres");
        Set<String> claves = Set.of("edad", "hermanos", "salud", "apoyo", "logistica");
        if (valores == null || !valores.keySet().equals(claves) ||
                valores.values().stream().anyMatch(v -> v == null || v < 0 || v > 100) ||
                valores.values().stream().mapToInt(Integer::intValue).sum() != 100)
            throw new IllegalArgumentException("Los cinco pesos deben estar entre 0 y 100 y sumar exactamente 100");
        // El bloqueo serializa los ajustes; la versión evita confirmar un formulario desactualizado.
        PesosScoring p = pesosRepo.bloquear().orElseThrow(() -> new IllegalStateException("Falta la configuración inicial de pesos"));
        if(versionEsperada==null || versionEsperada!=p.getVersion())
            throw new IllegalStateException("Los criterios cambiaron mientras los revisabas. Recarga la página antes de confirmar");
        Map<String,Integer> anteriores = valoresPesos(p);
        String detalle=codificar(Map.of("versionAnterior",p.getVersion(),"versionNueva",p.getVersion()+1,
                "anteriores",anteriores,"nuevos",valores,"justificacion",justificacion.strip(),"rol","COMITE"));
        if(detalle.length()>1000)throw new IllegalArgumentException("Simplifica la justificación para conservarla completa en el historial");
        int versionAnterior = p.getVersion();
        p.setEdad(valores.get("edad")); p.setHermanos(valores.get("hermanos"));
        p.setSalud(valores.get("salud")); p.setApoyo(valores.get("apoyo"));
        p.setLogistica(valores.get("logistica")); p.setVersion(p.getVersion() + 1);
        pesosRepo.save(p);
        if (auditoria != null) {
            EventoCaso e = new EventoCaso(); e.setCodigoCaso("CONFIG_MATCH"); e.setActor(actor);
            e.setFecha(LocalDateTime.now()); e.setAccion("PESOS_ACTUALIZADOS");
            e.setDescripcion(detalle); auditoria.save(e);
        }
        return p;
    }

    public List<Map<String, Object>> casos() {
        PesosScoring p = pesos();
        List<PerfilFamilia> disponibles = familias.findAll().stream()
                .filter(f -> f.isPerfilCompleto() && !f.isReevaluacionPendiente())
                .sorted(Comparator.comparing(PerfilFamilia::getCodigoCaso)).toList();
        return ninos.findAll().stream().filter(n -> n.isPerfilCompleto() && !n.isBajaLogica())
                .sorted(Comparator.comparing(PerfilNino::getCodigoCaso))
                .map(n -> caso(n, disponibles, p)).toList();
    }

    public Map<String, Object> caso(String codigo) {
        PerfilNino n = ninos.findByCodigoCaso(codigo)
                .orElseThrow(() -> new IllegalArgumentException("No existe ese caso infantil"));
        if (!n.isPerfilCompleto() || n.isBajaLogica()) throw new IllegalArgumentException("El perfil infantil no está activo y completo");
        return caso(n, familias.findAll().stream().filter(f -> f.isPerfilCompleto() && !f.isReevaluacionPendiente()).toList(), pesos());
    }

    private Map<String, Object> caso(PerfilNino n, List<PerfilFamilia> disponibles, PesosScoring p) {
        List<Map<String, Object>> ranking = disponibles.stream().map(f -> candidato(n, f, p))
                .sorted(Comparator.<Map<String, Object>>comparingInt(m -> (int)m.get("puntaje"))
                        .reversed().thenComparing(m -> (String)m.get("codigo"))).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("codigo", n.getCodigoCaso()); result.put("edad", n.getEdad());
        result.put("escolar", n.getNivelEscolar() == null ? "Sin indicar" : n.getNivelEscolar().name());
        List<String> necesidades = new ArrayList<>();
        necesidades.add(n.getNecesidadesApoyo() == null ? "Evaluar apoyos" : n.getNecesidadesApoyo());
        if (n.getTieneHermanos() == SiNo.SI) necesidades.add("Vínculo entre hermanos: " + n.getCantidadHermanos());
        if (n.isAlertaMedica()) necesidades.add("Atención: revisar necesidades médicas con un profesional");
        result.put("necesidades", necesidades);
        result.put("familias", ranking);
        result.put("historial", historial(n.getCodigoCaso()));
        result.put("versionPesos", p.getVersion());
        return result;
    }

    /** Cinco puntajes enteros, acumulados sobre 100 puntos de máximo. */
    public Map<String, Object> candidato(PerfilNino n, PerfilFamilia f, PesosScoring p) {
        Map<String, Object> factores = new LinkedHashMap<>();
        boolean edadDentro = n.getEdad() != null && f.getEdadMinimaAceptada() != null && f.getEdadMaximaAceptada() != null
                && n.getEdad() >= f.getEdadMinimaAceptada() && n.getEdad() <= f.getEdadMaximaAceptada();
        int edad = edadDentro ? p.getEdad() : 0;
        factores.put("edad", List.of(edad, edadDentro ? "Edad dentro del rango declarado" : "Edad fuera del rango declarado"));

        int hermanos = n.getTieneHermanos() != SiNo.SI ? p.getHermanos() : switch (f.getDisposicionHermanos()) {
            case DISPUESTA -> p.getHermanos();
            case DISPUESTA_CON_DUDAS -> Math.round(p.getHermanos() * .5f);
            case PREFIERE_UN_SOLO_NINO -> 0;
        };
        factores.put("hermanos", List.of(hermanos, n.getTieneHermanos() != SiNo.SI ? "No se declaró grupo de hermanos" : "Disposición a preservar vínculos familiares"));

        List<CondicionSalud> condiciones = n.getCondicionesSalud() == null ? List.of() : n.getCondicionesSalud().stream()
                .filter(c -> c != CondicionSalud.NINGUNA_CONOCIDA).toList();
        double fraccionSalud = condiciones.isEmpty() ? 1 : condiciones.stream().mapToDouble(c ->
                contiene(f.getCondicionesPuedeAcompanarHoy(), c) ? 1 : contiene(f.getCondicionesConApoyo(), c) ? .5 : 0).average().orElse(0);
        int salud = (int)Math.round(p.getSalud() * fraccionSalud);
        factores.put("salud", List.of(salud, condiciones.isEmpty() ? "No se declararon condiciones específicas" : "Capacidad declarada para acompañar necesidades de salud; requiere revisión técnica"));

        boolean apoyoReal = f.getRedApoyo() != null && f.getRedApoyo().stream().anyMatch(a -> a != FuenteApoyo.NINGUNA_POR_AHORA);
        int apoyo = apoyoReal ? p.getApoyo() : 0;
        factores.put("apoyo", List.of(apoyo, apoyoReal ? "La familia declara una red de apoyo" : "No declara red de apoyo actual"));

        int logistica = f.getDisponibilidadViaje() == DisponibilidadViaje.ALTA ? p.getLogistica()
                : f.getDisponibilidadViaje() == DisponibilidadViaje.MEDIA ? Math.round(p.getLogistica() * .6f)
                : Math.round(p.getLogistica() * .3f);
        factores.put("logistica", List.of(logistica, "Disponibilidad declarada para traslados y visitas"));

        int total = edad + hermanos + salud + apoyo + logistica;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("codigo", f.getCodigoCaso());
        result.put("provincia", f.getProvincia() == null ? "Sin indicar" : f.getProvincia().name());
        result.put("puntaje", total); result.put("f", factores);
        result.put("reevaluaciones", f.getNumeroReevaluaciones());
        result.put("alertaRevisionTecnica", f.getNumeroReevaluaciones() >= 3);
        return result;
    }

    private static <T> boolean contiene(List<T> elementos, T valor) {
        return elementos != null && elementos.contains(valor);
    }

    public List<Map<String, Object>> historial(String codigo) {
        return decisiones.findByCodigoNinoOrderByFechaAsc(codigo).stream().map(d -> {
            Map<String,Object> r = new LinkedHashMap<>();
            r.put("familia", d.getCodigoFamilia()); r.put("decision", d.getDecision());
            r.put("motivo", d.getMotivo()); r.put("por", d.getFuncionario()); r.put("fecha", d.getFecha()==null?null:d.getFecha().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
            r.put("puntaje", d.getPuntaje()); r.put("versionPesos", d.getVersionPesos());
            r.put("contextoScoring", d.getContextoScoring() == null ? null : decodificar(d.getContextoScoring()));
            return r;
        }).toList();
    }

    @Transactional
    public Map<String, Object> decidir(String codigoNino, String codigoFamilia, String decision,
                                        String motivo, String funcionario) {
        if (!Set.of("ACEPTADA", "DESCARTADA", "REEVALUACION").contains(decision))
            throw new IllegalArgumentException("Decisión desconocida");
        if (motivo == null || motivo.trim().length() < 10 || motivo.length() > 2000)
            throw new IllegalArgumentException("El motivo debe contener entre 10 y 2000 caracteres");
        PerfilNino n = ninos.bloquear(codigoNino)
                .orElseThrow(() -> new IllegalArgumentException("Caso infantil inexistente"));
        PerfilFamilia f = familias.findByCodigoCaso(codigoFamilia)
                .orElseThrow(() -> new IllegalArgumentException("Caso familiar inexistente"));
        if (!n.isPerfilCompleto() || n.isBajaLogica() || !f.isPerfilCompleto() || f.isReevaluacionPendiente())
            throw new IllegalStateException("Solo se revisan casos completos y sin reevaluación pendiente");
        if (decisiones.existsByCodigoNinoAndDecision(codigoNino, "ACEPTADA"))
            throw new IllegalStateException("Este caso ya tiene una aceptación registrada");
        PesosScoring p = pesos();
        DecisionComite d = new DecisionComite();
        d.setCodigoNino(codigoNino); d.setCodigoFamilia(codigoFamilia); d.setDecision(decision);
        d.setMotivo(motivo.trim()); d.setFuncionario(funcionario); d.setFecha(LocalDateTime.now());
        Map<String,Object> calculo = candidato(n,f,p);
        d.setPuntaje((int)calculo.get("puntaje")); d.setVersionPesos(p.getVersion());
        Map<String,Object> contexto = new LinkedHashMap<>();
        contexto.put("pesos", valoresPesos(p)); contexto.put("factores",calculo.get("f"));
        contexto.put("edadNino",n.getEdad()); contexto.put("edadMinimaFamilia",f.getEdadMinimaAceptada());
        contexto.put("edadMaximaFamilia",f.getEdadMaximaAceptada()); contexto.put("version",p.getVersion());
        d.setContextoScoring(codificar(contexto));
        decisiones.save(d);
        if (decision.equals("REEVALUACION")) {
            // La solicitud se audita ahora, pero solo cuenta cuando la familia
            // envía una entrevista corregida completa.
            f.setReevaluacionPendiente(true);
            familias.save(f);
        }
        return Map.of("mensaje", "Decisión guardada con trazabilidad", "decision", decision,
                "numeroReevaluaciones", f.getNumeroReevaluaciones(),
                "alertaRevisionTecnica", f.getNumeroReevaluaciones() >= 3);
    }

    public Map<String,Integer> valoresPesos(PesosScoring p) {
        return Map.of("edad",p.getEdad(),"hermanos",p.getHermanos(),"salud",p.getSalud(),"apoyo",p.getApoyo(),"logistica",p.getLogistica());
    }
    public List<Map<String,Object>> historialPesos() {
        return auditoria.findByCodigoCasoOrderByFechaAsc("CONFIG_MATCH").stream().map(e -> {
            Map<String,Object> r = new LinkedHashMap<>(); r.put("autor",e.getActor());r.put("fecha",e.getFecha()==null?null:e.getFecha().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
            r.put("cambio",decodificar(e.getDescripcion()));return r;
        }).toList();
    }
    private String codificar(Object value) {
        try { return json.writeValueAsString(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("No se pudo conservar el fundamento de la operación",e); }
    }
    private Object decodificar(String value) {
        try { return json.readValue(value,Object.class); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("No se pudo leer el fundamento histórico",e); }
    }
}
