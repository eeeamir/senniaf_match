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

    public ScoringService(PerfilNinoRepository ninos, PerfilFamiliaRepository familias,
                          DecisionComiteRepository decisiones, PesosScoringRepository pesosRepo) {
        this.ninos = ninos; this.familias = familias; this.decisiones = decisiones; this.pesosRepo = pesosRepo;
    }

    public PesosScoring pesos() { return pesosRepo.findById(1L).orElseGet(PesosScoring::new); }

    @Transactional
    public PesosScoring actualizarPesos(Map<String, Integer> valores) {
        Set<String> claves = Set.of("edad", "hermanos", "salud", "apoyo", "logistica");
        if (valores == null || !valores.keySet().equals(claves) ||
                valores.values().stream().anyMatch(v -> v == null || v < 0 || v > 100) ||
                valores.values().stream().mapToInt(Integer::intValue).sum() != 100)
            throw new IllegalArgumentException("Los cinco pesos deben estar entre 0 y 100 y sumar exactamente 100");
        PesosScoring p = pesos();
        p.setEdad(valores.get("edad")); p.setHermanos(valores.get("hermanos"));
        p.setSalud(valores.get("salud")); p.setApoyo(valores.get("apoyo"));
        p.setLogistica(valores.get("logistica")); p.setVersion(p.getVersion() + 1);
        return pesosRepo.save(p);
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
        int edad = n.getEdad() != null && f.getEdadMinimaAceptada() != null && f.getEdadMaximaAceptada() != null
                && n.getEdad() >= f.getEdadMinimaAceptada() && n.getEdad() <= f.getEdadMaximaAceptada() ? p.getEdad() : 0;
        factores.put("edad", List.of(edad, edad == p.getEdad() ? "Edad dentro del rango declarado" : "Edad fuera del rango declarado"));

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
            r.put("motivo", d.getMotivo()); r.put("por", d.getFuncionario()); r.put("fecha", d.getFecha());
            r.put("puntaje", d.getPuntaje()); r.put("versionPesos", d.getVersionPesos());
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
        PerfilNino n = ninos.findByCodigoCaso(codigoNino)
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
        d.setPuntaje((int)candidato(n, f, p).get("puntaje")); d.setVersionPesos(p.getVersion());
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
}
