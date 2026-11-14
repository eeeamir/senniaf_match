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
    @org.springframework.beans.factory.annotation.Autowired
    private VinculacionService vinculaciones;
    @org.springframework.beans.factory.annotation.Autowired
    private VinculacionRepository vinculacionesRepo;
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
                .filter(this::familiaDisponible)
                .sorted(Comparator.comparing(PerfilFamilia::getCodigoCaso)).toList();
        return ninos.findAll().stream().filter(n -> n.isPerfilCompleto() && !n.isBajaLogica() && !n.isCierrePorAdopcion())
                .sorted(Comparator.comparing(PerfilNino::getCodigoCaso))
                .map(n -> caso(n, disponibles, p)).toList();
    }

    public Map<String, Object> caso(String codigo) {
        PerfilNino n = ninos.findByCodigoCaso(codigo)
                .orElseThrow(() -> new IllegalArgumentException("No existe ese caso infantil"));
        if (!n.isPerfilCompleto() || n.isBajaLogica() || n.isCierrePorAdopcion()) throw new IllegalArgumentException("El perfil infantil no está activo y completo");
        return caso(n, familias.findAll().stream().filter(this::familiaDisponible).toList(), pesos());
    }

    private Map<String, Object> caso(PerfilNino n, List<PerfilFamilia> disponibles, PesosScoring p) {
        boolean aceptada = ninoReservado(n.getCodigoCaso());
        List<Map<String, Object>> ranking = (aceptada ? java.util.stream.Stream.<PerfilFamilia>empty() : disponibles.stream()).map(f -> candidato(n, f, p))
                .sorted(Comparator.<Map<String, Object>>comparingInt(m -> (int)m.get("puntaje"))
                        .reversed().thenComparing(m -> (String)m.get("codigo"))).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("codigo", n.getCodigoCaso()); result.put("edad", n.getEdad());
        result.put("unidadEdad", n.getUnidadEdad());
        result.put("escolar", n.getNivelEscolar() == null ? "Sin indicar" : n.getNivelEscolar().name());
        List<String> necesidades = new ArrayList<>();
        necesidades.add(n.getNecesidadesApoyo() == null ? "Evaluar apoyos" : n.getNecesidadesApoyo());
        if (n.getTieneHermanos() == SiNo.SI) necesidades.add("Vínculo entre hermanos: " + n.getCantidadHermanos());
        if (n.isAlertaMedica()) necesidades.add("Atención: revisar necesidades médicas con un profesional");
        result.put("necesidades", necesidades);
        result.put("familias", ranking);
        result.put("propuestaAceptada", aceptada);
        result.put("historial", historial(n.getCodigoCaso()));
        result.put("versionPesos", p.getVersion());
        return result;
    }

    private boolean ninoReservado(String codigo) {
        return vinculacionesRepo == null ? decisiones.existsByCodigoNinoAndDecision(codigo,"ACEPTADA")
                : vinculacionesRepo.existsByNinoCodigoCasoAndReservaVigenteTrue(codigo);
    }
    private boolean familiaReservada(String codigo) {
        return vinculacionesRepo == null ? decisiones.existsByCodigoFamiliaAndDecision(codigo,"ACEPTADA")
                : vinculacionesRepo.existsByFamiliaCodigoCasoAndReservaVigenteTrue(codigo);
    }
    private boolean familiaDisponible(PerfilFamilia f) {
        return f.isPerfilCompleto() && !f.isReevaluacionPendiente()
                && !familiaReservada(f.getCodigoCaso());
    }

    /**
     * Misma escala conservadora de 100 y mismos pesos. Un dato desconocido o no aplicable
     * no acredita compatibilidad ni reparte sus puntos entre los otros factores.
     * Se informa el máximo aplicable para no confundir falta de evidencia con incompatibilidad.
     */
    public Map<String, Object> candidato(PerfilNino n, PerfilFamilia f, PesosScoring p) {
        Map<String, Object> factores = new LinkedHashMap<>();
        Map<String, String> estados = new LinkedHashMap<>();
        boolean edadConocida = n.getEdad() != null && f.getEdadMinimaAceptada() != null && f.getEdadMaximaAceptada() != null
                && f.getEdadMinimaAceptada() <= f.getEdadMaximaAceptada();
        // El rango familiar se expresa en años cumplidos; todos los menores de un año
        // pertenecen al tramo 0, sin sustituir su edad ni inventar precisión histórica.
        int aniosCumplidos = n.getEdad() == null ? -1 : n.getUnidadEdad() == com.senniaf.match.model.enums.UnidadEdad.MESES ? 0 : n.getEdad();
        boolean edadDentro = edadConocida && aniosCumplidos >= f.getEdadMinimaAceptada() && aniosCumplidos <= f.getEdadMaximaAceptada();
        int edad = edadDentro ? p.getEdad() : 0;
        factor(factores, estados, "edad", edad, edadConocida ? "EVALUADO" : "DESCONOCIDO",
                !edadConocida ? "Información de edad o rango no disponible" : edadDentro ? "Edad dentro del rango declarado" : "Edad fuera del rango declarado");

        int hermanos = 0;
        boolean sinHermanos = n.getTieneHermanos() == SiNo.NO && (n.getCantidadHermanos() == null || n.getCantidadHermanos() == 0);
        boolean grupoConocido = n.getTieneHermanos() == SiNo.SI && n.getCantidadHermanos() != null && n.getCantidadHermanos() > 0;
        boolean disposicionConocida = f.getDisposicionHermanos() == DisposicionHermanos.DISPUESTA
                || f.getDisposicionHermanos() == DisposicionHermanos.DISPUESTA_CON_DUDAS
                || f.getDisposicionHermanos() == DisposicionHermanos.PREFIERE_UN_SOLO_NINO;
        String estadoHermanos = sinHermanos ? "NO_APLICA" : grupoConocido && disposicionConocida ? "EVALUADO" : "DESCONOCIDO";
        if (grupoConocido) {
            if (f.getDisposicionHermanos() == DisposicionHermanos.DISPUESTA) hermanos = p.getHermanos();
            else if (f.getDisposicionHermanos() == DisposicionHermanos.DISPUESTA_CON_DUDAS) hermanos = Math.round(p.getHermanos() * .5f);
        }
        factor(factores, estados, "hermanos", hermanos, estadoHermanos, sinHermanos
                ? "No aplica: se confirmó que no tiene hermanos; no se acreditan puntos"
                : !grupoConocido ? "No se conoce un grupo de hermanos confirmado"
                : !disposicionConocida ? "La disposición requiere valoración del Comité; no se interpreta como afirmativa"
                : "Disposición declarada ante un grupo de hermanos; requiere revisión del vínculo");

        var datosSalud = n.getCondicionesSalud();
        boolean saludDesconocida = datosSalud == null || datosSalud.isEmpty() || datosSalud.stream().anyMatch(Objects::isNull)
                || datosSalud.contains(CondicionSalud.INFORMACION_NO_DISPONIBLE)
                || datosSalud.contains(CondicionSalud.NINGUNA_CONOCIDA) && datosSalud.size() > 1;
        boolean sinCondiciones = !saludDesconocida && datosSalud.size() == 1 && datosSalud.contains(CondicionSalud.NINGUNA_CONOCIDA);
        var condiciones = saludDesconocida || sinCondiciones ? List.<CondicionSalud>of() : datosSalud;
        boolean capacidadesConocidas = f.getCondicionesPuedeAcompanarHoy() != null && f.getCondicionesConApoyo() != null;
        int salud = capacidadesConocidas ? (int)Math.round(p.getSalud() * condiciones.stream().mapToDouble(c ->
                contiene(f.getCondicionesPuedeAcompanarHoy(), c) ? 1 : contiene(f.getCondicionesConApoyo(), c) ? .5 : 0).average().orElse(0)) : 0;
        factor(factores, estados, "salud", salud, sinCondiciones ? "NO_APLICA" : saludDesconocida || !capacidadesConocidas ? "DESCONOCIDO" : "EVALUADO",
                saludDesconocida ? "Información médica no disponible o contradictoria; no equivale a ausencia de condiciones"
                : sinCondiciones ? "No aplica a condiciones específicas: ninguna conocida; no se acreditan puntos. Revisar alergias y apoyos por separado"
                : !capacidadesConocidas ? "No se conocen las capacidades de acompañamiento de la familia"
                : "Capacidad declarada para condiciones documentadas; con apoyo adicional se acredita la mitad. Requiere revisión técnica");

        boolean redConocida = f.getRedApoyo() != null && !f.getRedApoyo().isEmpty() && !f.getRedApoyo().stream().anyMatch(Objects::isNull)
                && !(f.getRedApoyo().contains(FuenteApoyo.NINGUNA_POR_AHORA) && f.getRedApoyo().size() > 1);
        boolean apoyoReal = redConocida && f.getRedApoyo().stream().anyMatch(a -> a != FuenteApoyo.NINGUNA_POR_AHORA);
        int apoyo = apoyoReal ? p.getApoyo() : 0;
        factor(factores, estados, "apoyo", apoyo, redConocida ? "EVALUADO" : "DESCONOCIDO", !redConocida
                ? "Red de apoyo no disponible o contradictoria" : apoyoReal ? "La familia declara fuentes concretas de apoyo" : "La familia declara que aún no tiene red de apoyo");

        int logistica = f.getDisponibilidadViaje() == DisponibilidadViaje.ALTA ? p.getLogistica()
                : f.getDisponibilidadViaje() == DisponibilidadViaje.MEDIA ? Math.round(p.getLogistica() * .6f)
                : f.getDisponibilidadViaje() == DisponibilidadViaje.BAJA ? Math.round(p.getLogistica() * .3f) : 0;
        factor(factores, estados, "logistica", logistica, f.getDisponibilidadViaje() == null ? "DESCONOCIDO" : "EVALUADO",
                f.getDisponibilidadViaje() == null ? "Disponibilidad de traslado no conocida; no acredita puntos" : "Disponibilidad declarada para traslados y visitas");

        int total = edad + hermanos + salud + apoyo + logistica;
        int noAplicables = valoresPesos(p).entrySet().stream().filter(e -> "NO_APLICA".equals(estados.get(e.getKey()))).mapToInt(Map.Entry::getValue).sum();
        int pesoEvaluado = valoresPesos(p).entrySet().stream().filter(e -> "EVALUADO".equals(estados.get(e.getKey()))).mapToInt(Map.Entry::getValue).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("codigo", f.getCodigoCaso());
        result.put("provincia", f.getProvincia() == null ? "Sin indicar" : f.getProvincia().name());
        result.put("puntaje", total); result.put("f", factores); result.put("estadosFactores", estados);
        result.put("maximoAplicable", 100 - noAplicables); result.put("pesoEvaluado", pesoEvaluado);
        result.put("reglas", "EVIDENCIA_2");
        result.put("reevaluaciones", f.getNumeroReevaluaciones());
        result.put("alertaRevisionTecnica", f.getNumeroReevaluaciones() >= 3 || estados.containsValue("DESCONOCIDO"));
        return result;
    }

    private void factor(Map<String,Object> factores, Map<String,String> estados, String clave, int puntos, String estado, String razon) {
        factores.put(clave, List.of(puntos, razon)); estados.put(clave, estado);
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
        PerfilFamilia f = familias.bloquear(codigoFamilia)
                .orElseThrow(() -> new IllegalArgumentException("Caso familiar inexistente"));
        if (!n.isPerfilCompleto() || n.isBajaLogica() || n.isCierrePorAdopcion() || !f.isPerfilCompleto() || f.isReevaluacionPendiente())
            throw new IllegalStateException("Solo se revisan casos completos y sin reevaluación pendiente");
        if (ninoReservado(codigoNino))
            throw new IllegalStateException("Este caso ya tiene una aceptación registrada");
        if (familiaReservada(codigoFamilia))
            throw new IllegalStateException("La familia ya tiene una propuesta aceptada; no participa en nuevas propuestas");
        PesosScoring p = pesos();
        DecisionComite d = new DecisionComite();
        d.setCodigoNino(codigoNino); d.setCodigoFamilia(codigoFamilia); d.setDecision(decision);
        d.setMotivo(motivo.trim()); d.setFuncionario(funcionario); d.setFecha(LocalDateTime.now());
        Map<String,Object> calculo = candidato(n,f,p);
        d.setPuntaje((int)calculo.get("puntaje")); d.setVersionPesos(p.getVersion());
        Map<String,Object> contexto = new LinkedHashMap<>();
        contexto.put("reglas", calculo.get("reglas"));
        contexto.put("estadosFactores", calculo.get("estadosFactores"));
        contexto.put("maximoAplicable", calculo.get("maximoAplicable"));
        contexto.put("pesoEvaluado", calculo.get("pesoEvaluado"));
        contexto.put("pesos", valoresPesos(p)); contexto.put("factores",calculo.get("f"));
        contexto.put("edadNino",n.getEdad()); contexto.put("edadMinimaFamilia",f.getEdadMinimaAceptada());
        contexto.put("unidadEdadNino",n.getUnidadEdad());
        contexto.put("edadMaximaFamilia",f.getEdadMaximaAceptada()); contexto.put("version",p.getVersion());
        d.setContextoScoring(codificar(contexto));
        decisiones.save(d);
        if (decision.equals("ACEPTADA")) vinculaciones.crear(d,n,f);
        if (decision.equals("REEVALUACION")) {
            // La solicitud se audita ahora, pero solo cuenta cuando la familia
            // envía una entrevista corregida completa.
            f.setReevaluacionPendiente(true);
            familias.save(f);
        }
        return Map.of("mensaje", decision.equals("ACEPTADA") ? "La propuesta se envió a la familia" : "La decisión quedó registrada", "decision", decision,
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
