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
    @org.springframework.beans.factory.annotation.Autowired private PesosProgramadosService programados;
    @org.springframework.beans.factory.annotation.Autowired private CombinacionExcluidaRepository excluidas;
    private final PerfilNinoRepository ninos;
    private final PerfilFamiliaRepository familias;
    private final DecisionComiteRepository decisiones;
    private final PesosScoringRepository pesosRepo;
    private final EventoCasoRepository auditoria;
    private final AnalizadorTextoCompatibilidad analizador = new AnalizadorTextoCompatibilidad();
    @org.springframework.beans.factory.annotation.Autowired
    private EvaluacionPsicologicaRepository evaluaciones;
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

    public PesosScoring pesos() { return programados==null?pesosRepo.findById(1L).orElseGet(PesosScoring::new):programados.vigentes(); }
    @Transactional public Map<String,Object> actualizarPesos(Map<String,Integer> v,String actor,String motivo,Integer version){return programados.programar(v,actor,motivo,version);}
    @org.springframework.beans.factory.annotation.Autowired private com.senniaf.match.repository.InterrupcionConvivenciaRepository interrupciones;
    public Map<String,Object> programacion(){return programados.estado();}
    private boolean excluida(String n,String f){return decisiones.existsByCodigoNinoAndCodigoFamiliaAndDecision(n,f,"DESCARTADA") || excluidas!=null&&excluidas.existsByCodigoNinoAndCodigoFamilia(n,f) || interrupciones!=null&&interrupciones.existeInterrupcionSinReingresoAutorizado(n,f);}
    public List<Map<String, Object>> casos() {
        PesosScoring p = pesos();
        Set<Par> rechazadas=paresExcluidos();
        List<PerfilFamilia> disponibles = familias.findAll().stream()
                .filter(this::familiaDisponible)
                .sorted(Comparator.comparing(PerfilFamilia::getCodigoCaso)).toList();
        var capacidadesFamilias = analizarFamilias(disponibles);
        Map<String, Map<String, Object>> antecedentes = new HashMap<>();
        return ninos.findAll().stream().filter(n -> n.isPerfilCompleto() && !n.isBajaLogica() && !n.isCierrePorAdopcion() && !ninoReservado(n.getCodigoCaso()))
                .sorted(Comparator.comparing(PerfilNino::getCodigoCaso))
                .map(n -> caso(n, disponibles, p, rechazadas, capacidadesFamilias, false, antecedentes)).toList();
    }

    public Map<String, Object> caso(String codigo) {
        PerfilNino n = ninos.findByCodigoCaso(codigo)
                .orElseThrow(() -> new IllegalArgumentException("No existe ese caso infantil"));
        if (!n.isPerfilCompleto() || n.isBajaLogica() || n.isCierrePorAdopcion()) throw new IllegalArgumentException("El perfil infantil no está activo y completo");
        return caso(n, familias.findAll().stream().filter(this::familiaDisponible).toList(), pesos(),paresExcluidos());
    }

    private Map<PerfilFamilia, AnalizadorTextoCompatibilidad.Analisis> analizarFamilias(List<PerfilFamilia> familias) {
        Map<PerfilFamilia, AnalizadorTextoCompatibilidad.Analisis> resultado = new IdentityHashMap<>();
        familias.forEach(familia -> resultado.put(familia, analizador.capacidades(familia)));
        return resultado;
    }

    private Map<String, Object> caso(PerfilNino n, List<PerfilFamilia> disponibles, PesosScoring p,
                                    Set<Par> rechazadas) {
        return caso(n, disponibles, p, rechazadas, analizarFamilias(disponibles),
                ninoReservado(n.getCodigoCaso()), new HashMap<>());
    }

    private Map<String, Object> caso(PerfilNino n, List<PerfilFamilia> disponibles, PesosScoring p,
                                    Set<Par> rechazadas,
                                    Map<PerfilFamilia, AnalizadorTextoCompatibilidad.Analisis> capacidadesFamilias,
                                    boolean aceptada, Map<String, Map<String, Object>> antecedentes) {
        var analisisNecesidades = analizarNecesidades(n);
        var candidatas = aceptada ? java.util.stream.Stream.<PerfilFamilia>empty() : disponibles.stream();
        List<Map<String, Object>> ranking = candidatas
                .filter(f -> !rechazadas.contains(new Par(n.getCodigoCaso(), f.getCodigoCaso())))
                .map(f -> candidato(n, f, p, analisisNecesidades, capacidadesFamilias.get(f),
                        antecedentes.computeIfAbsent(f.getCodigoCaso(), this::antecedenteInterrupcion)))
                .sorted(Comparator.<Map<String, Object>>comparingInt(m -> (int) m.get("puntaje"))
                        .reversed().thenComparing(m -> (String) m.get("codigo"))).toList();
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
        result.put("familiasRechazadas",rechazadas.stream().filter(par->par.nino().equals(n.getCodigoCaso())).map(Par::familia).sorted().toList());
        result.put("propuestaAceptada", aceptada);
        result.put("historial", historial(n.getCodigoCaso()));
        result.put("versionPesos", p.getVersion());
        return result;
    }

    private record Par(String nino,String familia){}
    /** Se cargan los pares una sola vez por ranking, sin consultas por cada candidato. */
    private Set<Par> paresExcluidos(){
        Set<Par> pares=new HashSet<>();
        decisiones.findByDecision("DESCARTADA").forEach(d->pares.add(new Par(d.getCodigoNino(),d.getCodigoFamilia())));
        if(excluidas!=null)excluidas.findAll().forEach(e->pares.add(new Par(e.codigoNino,e.codigoFamilia)));
        if(interrupciones!=null)interrupciones.paresConInterrupcionNoHabilitada().forEach(i->pares.add(new Par(i.getNino(),i.getFamilia())));
        return pares;
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
        return candidato(n, f, p, analizarNecesidades(n), analizador.capacidades(f), antecedenteInterrupcion(f.getCodigoCaso()));
    }

    private AnalizadorTextoCompatibilidad.Analisis analizarNecesidades(PerfilNino n) {
        var informes = evaluaciones == null ? List.<EvaluacionPsicologica>of()
                : evaluaciones.findByExpedienteCodigoCasoOrderByFechaDescIdDesc(n.getCodigoCaso());
        return analizador.necesidades(n, informes);
    }

    private Map<String, Object> candidato(PerfilNino n, PerfilFamilia f, PesosScoring p,
                                         AnalizadorTextoCompatibilidad.Analisis necesidades,
                                         AnalizadorTextoCompatibilidad.Analisis capacidades, Map<String, Object> antecedente) {
        Map<String, Object> factores = new LinkedHashMap<>();
        Map<String, String> estados = new LinkedHashMap<>();
        boolean edadConocida = n.getEdad() != null && f.getEdadMinimaAceptada() != null && f.getEdadMaximaAceptada() != null
                && f.getEdadMinimaAceptada() <= f.getEdadMaximaAceptada();
        // El rango familiar se expresa en años cumplidos; todos los menores de un año
        // pertenecen al tramo 0, sin sustituir su edad ni inventar precisión histórica.
        int aniosCumplidos = n.getEdad() == null ? -1 : n.getUnidadEdad() == com.senniaf.match.model.enums.UnidadEdad.MESES ? 0 : n.getEdad();
        boolean edadDentro = edadConocida && aniosCumplidos >= f.getEdadMinimaAceptada() && aniosCumplidos <= f.getEdadMaximaAceptada();
        int edad = edadDentro ? p.getEdad() : 0;
        String edadRegistrada = n.getEdad() == null ? "La edad infantil está sin registrar"
                : "La edad registrada es de " + n.getEdad() + (n.getUnidadEdad() == UnidadEdad.MESES
                        ? (n.getEdad() == 1 ? " mes" : " meses") : (n.getEdad() == 1 ? " año" : " años"));
        String rango = f.getEdadMinimaAceptada() == null || f.getEdadMaximaAceptada() == null
                ? "el rango familiar está incompleto" : "la familia declaró un rango de "
                + f.getEdadMinimaAceptada() + " a " + f.getEdadMaximaAceptada() + " años cumplidos";
        factor(factores, estados, "edad", edad, edadConocida ? "EVALUADO" : "DESCONOCIDO",
                edadRegistrada + " y " + rango + ". " + (!edadConocida ? "Se requiere confirmar los datos de edad o del rango."
                        : edadDentro ? "La edad está dentro del rango y se acredita el peso del criterio."
                        : "La edad está fuera del rango declarado y el criterio no acredita puntos."));

        EvaluacionCriterio evaluacionHermanos = evaluarHermanos(n, necesidades, capacidades, p.getHermanos());
        EvaluacionCriterio evaluacionSalud = evaluarSalud(necesidades, capacidades, p.getSalud());
        EvaluacionCriterio evaluacionApoyo = evaluarApoyo(f, necesidades, capacidades, p.getApoyo());
        EvaluacionCriterio evaluacionLogistica = evaluarLogistica(f, capacidades, p.getLogistica());
        registrarFactor(factores, estados, "hermanos", evaluacionHermanos);
        registrarFactor(factores, estados, "salud", evaluacionSalud);
        registrarFactor(factores, estados, "apoyo", evaluacionApoyo);
        registrarFactor(factores, estados, "logistica", evaluacionLogistica);
        int hermanos = evaluacionHermanos.puntos();
        int salud = evaluacionSalud.puntos();
        int apoyo = evaluacionApoyo.puntos();
        int logistica = evaluacionLogistica.puntos();

        int total = edad + hermanos + salud + apoyo + logistica;
        int maximoAplicable = valoresPesos(p).entrySet().stream().filter(e -> !"NO_APLICA".equals(estados.get(e.getKey()))).mapToInt(Map.Entry::getValue).sum();
        int pesoEvaluado = valoresPesos(p).entrySet().stream().filter(e -> "EVALUADO".equals(estados.get(e.getKey()))).mapToInt(Map.Entry::getValue).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("codigo", f.getCodigoCaso());
        result.put("provincia", f.getProvincia() == null ? "Sin indicar" : f.getProvincia().name());
        result.put("puntaje", total); result.put("f", factores); result.put("estadosFactores", estados);
        result.put("maximoAplicable", maximoAplicable); result.put("pesoEvaluado", pesoEvaluado);
        result.put("reglas", "EVIDENCIA_TEXTO_8");
        result.put("evidenciasCompatibilidad", Map.of("necesidades", necesidades, "capacidades", capacidades));
        result.put("reevaluaciones", f.getNumeroReevaluaciones());
        result.put("antecedenteInterrupcion", antecedente);
        result.put("alertaRevisionTecnica", f.getNumeroReevaluaciones() >= 3 || estados.containsValue("DESCONOCIDO"));
        return result;
    }

    private void factor(Map<String,Object> factores, Map<String,String> estados, String clave, int puntos, String estado, String razon) {
        factores.put(clave, List.of(puntos, razon)); estados.put(clave, estado);
    }

    private record EvaluacionCriterio(int puntos, String estado, String razon) {}

    private void registrarFactor(Map<String, Object> factores, Map<String, String> estados,
                                  String criterio, EvaluacionCriterio evaluacion) {
        factor(factores, estados, criterio, evaluacion.puntos(), evaluacion.estado(), evaluacion.razon());
    }

    private boolean es(AnalizadorTextoCompatibilidad.Hallazgo hallazgo,
                       AnalizadorTextoCompatibilidad.Declaracion tipo) {
        return hallazgo != null && hallazgo.declaracion() == tipo;
    }

    private EvaluacionCriterio evaluarSalud(AnalizadorTextoCompatibilidad.Analisis necesidades,
                                           AnalizadorTextoCompatibilidad.Analisis capacidades,
                                           int peso) {
        var pendientes = necesidades.necesidadesSalud();
        if (pendientes.isEmpty()) {
            return new EvaluacionCriterio(peso, "EVALUADO",
                    "El menor no presenta condiciones de salud conocidas ni otras necesidades de cuidado específicas registradas. Se acreditan " + peso + " puntos.");
        }
        var general = capacidades.obtener("COBERTURA_GENERAL_SALUD");
        double cubiertas = 0;
        boolean desconocido = false;
        List<String> razones = new ArrayList<>();
        for (var necesidad : pendientes) {
            var capacidad = capacidades.obtener(necesidad.concepto());
            String cobertura;
            if (es(necesidad, AnalizadorTextoCompatibilidad.Declaracion.AMBIGUA)) {
                desconocido = true;
                cobertura = "el expediente requiere aclarar esta necesidad antes de acreditar su cobertura";
            } else if (es(capacidad, AnalizadorTextoCompatibilidad.Declaracion.ACTUAL)) {
                cubiertas += 1;
                cobertura = "la familia declaró disponibilidad actual para atender esta necesidad y se acredita su cobertura";
            } else if (es(capacidad, AnalizadorTextoCompatibilidad.Declaracion.CON_APOYO)) {
                cubiertas += .5;
                cobertura = "la familia declaró disponibilidad con apoyo adicional y se acredita media cobertura";
            } else if (es(capacidad, AnalizadorTextoCompatibilidad.Declaracion.AMBIGUA)) {
                desconocido = true;
                cobertura = "la declaración familiar es ambigua o contradictoria y requiere aclaración";
            } else if (es(capacidad, AnalizadorTextoCompatibilidad.Declaracion.NEGATIVA)) {
                cobertura = "la familia declaró que no puede atender esta necesidad y no se acredita su cobertura";
            } else if ((capacidad == null || es(capacidad, AnalizadorTextoCompatibilidad.Declaracion.INSUFICIENTE))
                    && AnalizadorTextoCompatibilidad.condicionMedica(necesidad.concepto())
                    && es(general, AnalizadorTextoCompatibilidad.Declaracion.ACTUAL)) {
                cubiertas += 1;
                cobertura = "la familia declaró capacidad general para atender condiciones de salud";
            } else {
                cobertura = "la familia no declaró disponibilidad específica; se requiere aclaración";
                desconocido = true;
            }
            razones.add("Para " + necesidad.nombre().toLowerCase(Locale.ROOT) + ", " + cobertura);
        }
        int puntos = (int) Math.round(peso * cubiertas / pendientes.size());
        return new EvaluacionCriterio(puntos, desconocido ? "DESCONOCIDO" : "EVALUADO",
                String.join(". ", razones) + ". Se acreditan " + puntos + " de " + peso + " puntos.");
    }

    private EvaluacionCriterio evaluarHermanos(PerfilNino n,
                                               AnalizadorTextoCompatibilidad.Analisis necesidades,
                                               AnalizadorTextoCompatibilidad.Analisis capacidades,
                                               int peso) {
        var presencia = necesidades.obtener("PRESENCIA_HERMANOS");
        boolean ausenciaConfirmada = n.getTieneHermanos() == SiNo.NO
                && (n.getCantidadHermanos() == null || n.getCantidadHermanos() == 0);
        if (es(presencia, AnalizadorTextoCompatibilidad.Declaracion.AMBIGUA)
                || ausenciaConfirmada && es(presencia, AnalizadorTextoCompatibilidad.Declaracion.NECESIDAD)
                || n.getTieneHermanos() == SiNo.SI && es(presencia, AnalizadorTextoCompatibilidad.Declaracion.NEGATIVA)) {
            return new EvaluacionCriterio(0, "DESCONOCIDO",
                    "La ficha y el detalle requieren una aclaración sobre la presencia de hermanos. El peso se mantiene en el máximo.");
        }
        if (ausenciaConfirmada) {
            return new EvaluacionCriterio(0, "NO_APLICA",
                    "La ausencia de hermanos está confirmada. Se excluye el peso de este criterio del máximo.");
        }
        if (n.getTieneHermanos() != SiNo.SI || n.getCantidadHermanos() == null || n.getCantidadHermanos() <= 0) {
            return new EvaluacionCriterio(0, "DESCONOCIDO", "Se requiere confirmar si el niño tiene hermanos y cuántos son.");
        }
        var disposicion = capacidades.obtener("HERMANOS");
        int puntos = 0;
        String estado = "EVALUADO";
        String razon;
        if (es(disposicion, AnalizadorTextoCompatibilidad.Declaracion.ACTUAL)) {
            puntos = peso;
            razon = "La familia declaró disposición para recibir hermanos conjuntamente.";
        } else if (es(disposicion, AnalizadorTextoCompatibilidad.Declaracion.CON_APOYO)) {
            puntos = Math.round(peso * .5f);
            razon = "La familia declaró dudas, condiciones o límites para recibir hermanos conjuntamente.";
        } else if (es(disposicion, AnalizadorTextoCompatibilidad.Declaracion.NEGATIVA)) {
            razon = "La familia declaró que prefiere recibir a un solo niño o que no puede recibir hermanos conjuntamente.";
        } else {
            estado = "DESCONOCIDO";
            razon = "Se requiere aclarar la disposición familiar para recibir hermanos conjuntamente.";
        }
        var contacto = capacidades.obtener("CONTACTO_HERMANOS");
        if (es(contacto, AnalizadorTextoCompatibilidad.Declaracion.ACTUAL)) {
            razon += " La familia también declaró disposición para mantener el contacto entre hermanos.";
        } else if (es(contacto, AnalizadorTextoCompatibilidad.Declaracion.NEGATIVA)) {
            razon += " La familia declaró una negativa respecto al contacto entre hermanos.";
        } else if (contacto != null && !es(contacto, AnalizadorTextoCompatibilidad.Declaracion.INSUFICIENTE)) {
            razon += " La disposición para mantener el contacto entre hermanos requiere aclaración.";
        }
        var conjunta = necesidades.obtener("ACOGIDA_CONJUNTA");
        if (es(conjunta, AnalizadorTextoCompatibilidad.Declaracion.NECESIDAD)) {
            razon += " El expediente menciona la acogida conjunta.";
        }
        return new EvaluacionCriterio(puntos, estado, "Hermanos registrados: " + n.getCantidadHermanos()
                + ". " + razon + " Se acreditan " + puntos + " de " + peso + " puntos.");
    }

    private EvaluacionCriterio evaluarApoyo(PerfilFamilia familia, AnalizadorTextoCompatibilidad.Analisis necesidades,
                                            AnalizadorTextoCompatibilidad.Analisis capacidades, int peso) {
        var red = capacidades.obtener("RED_APOYO");
        if (red == null || es(red, AnalizadorTextoCompatibilidad.Declaracion.AMBIGUA)
                || es(red, AnalizadorTextoCompatibilidad.Declaracion.INSUFICIENTE)) {
            return new EvaluacionCriterio(0, "DESCONOCIDO", "Se requiere aclarar con qué personas o recursos de apoyo cuenta la familia.");
        }
        if (es(red, AnalizadorTextoCompatibilidad.Declaracion.NEGATIVA)) {
            return new EvaluacionCriterio(0, "EVALUADO", "La familia declaró que actualmente no cuenta con una red de apoyo. El criterio no acredita puntos.");
        }
        String fuentes = familia.getRedApoyo() == null ? "" : familia.getRedApoyo().stream()
                .filter(Objects::nonNull).map(fuente -> switch (fuente) {
                    case PAREJA -> "pareja";
                    case FAMILIA_EXTENDIDA -> "familia extendida";
                    case AMISTADES_O_COMUNIDAD -> "amistades o comunidad";
                    case NINGUNA_POR_AHORA -> "";
                }).filter(fuente -> !fuente.isEmpty()).collect(java.util.stream.Collectors.joining(", "));
        String razon = fuentes.isEmpty() ? "La familia describió una red de apoyo en la entrevista."
                : "La familia declaró apoyo de " + fuentes + ".";
        if (red.evidencias().stream().anyMatch(e -> AnalizadorTextoCompatibilidad.apoyoContinuo(e.original()))) {
            razon += " La nota menciona apoyo habitual o permanente.";
        }
        if (red.evidencias().stream().anyMatch(e -> AnalizadorTextoCompatibilidad.disponibilidadLimitada(e.original()))) {
            razon += " La nota describe apoyo ocasional o con restricciones de disponibilidad.";
        } else if (es(red, AnalizadorTextoCompatibilidad.Declaracion.CON_APOYO)) {
            razon += " El apoyo descrito está sujeto a una condición.";
        }
        var cuidado = necesidades.obtener("CUIDADOS_DIARIOS");
        if (es(cuidado, AnalizadorTextoCompatibilidad.Declaracion.NECESIDAD)) {
            var ayuda = capacidades.obtener("CUIDADOS_DIARIOS");
            if (es(ayuda, AnalizadorTextoCompatibilidad.Declaracion.ACTUAL)) {
                razon += ". Cuidados diarios: apoyo correspondiente declarado";
            } else if (es(ayuda, AnalizadorTextoCompatibilidad.Declaracion.CON_APOYO)) {
                razon += ". Cuidados diarios: apoyo correspondiente condicionado";
            } else {
                razon += ". Cuidados diarios: sin apoyo específico equivalente; se conserva la valoración general de la red";
            }
        }
        return new EvaluacionCriterio(peso, "EVALUADO", razon + " Se acreditan " + peso
                + " puntos por la red general declarada.");
    }

    private EvaluacionCriterio evaluarLogistica(PerfilFamilia f,
                                                AnalizadorTextoCompatibilidad.Analisis capacidades,
                                                int peso) {
        int puntos = 0;
        if (f.getDisponibilidadViaje() == DisponibilidadViaje.ALTA) puntos = peso;
        if (f.getDisponibilidadViaje() == DisponibilidadViaje.MEDIA) puntos = Math.round(peso * .6f);
        if (f.getDisponibilidadViaje() == DisponibilidadViaje.BAJA) puntos = Math.round(peso * .3f);
        String estado = f.getDisponibilidadViaje() == null ? "DESCONOCIDO" : "EVALUADO";
        String razon = f.getDisponibilidadViaje() == null ? "Falta confirmar la categoría de disponibilidad familiar."
                : "La familia registró disponibilidad " + f.getDisponibilidadViaje().name().toLowerCase(Locale.ROOT)
                + " para traslados o recepción de visitas.";
        var viaje = capacidades.obtener("VIAJE");
        var visitas = capacidades.obtener("VISITAS");
        boolean noViaja = es(viaje, AnalizadorTextoCompatibilidad.Declaracion.NEGATIVA);
        boolean noRecibe = es(visitas, AnalizadorTextoCompatibilidad.Declaracion.NEGATIVA);
        boolean viaja = es(viaje, AnalizadorTextoCompatibilidad.Declaracion.ACTUAL)
                || es(viaje, AnalizadorTextoCompatibilidad.Declaracion.CON_APOYO);
        boolean recibe = es(visitas, AnalizadorTextoCompatibilidad.Declaracion.ACTUAL)
                || es(visitas, AnalizadorTextoCompatibilidad.Declaracion.CON_APOYO);
        if (noViaja && noRecibe) {
            return new EvaluacionCriterio(0, "EVALUADO",
                    "La familia declaró que no puede trasladarse ni recibir visitas. El criterio no acredita puntos.");
        }
        if (es(viaje, AnalizadorTextoCompatibilidad.Declaracion.AMBIGUA)
                || es(visitas, AnalizadorTextoCompatibilidad.Declaracion.AMBIGUA)
                || noViaja && !recibe || noRecibe && !viaja) {
            return new EvaluacionCriterio(0, "DESCONOCIDO",
                    "La nota restringe o contradice la disponibilidad declarada; sin puntos hasta aclararla");
        }
        if (noViaja && recibe) razon += " La nota restringe la disponibilidad a recibir visitas.";
        if (noRecibe && viaja) razon += " La nota restringe la disponibilidad a los traslados.";
        if (es(viaje, AnalizadorTextoCompatibilidad.Declaracion.CON_APOYO)
                || es(visitas, AnalizadorTextoCompatibilidad.Declaracion.CON_APOYO)) {
            razon += " La nota describe condiciones de horario, transporte o disponibilidad.";
        }
        return new EvaluacionCriterio(puntos, estado, razon + (f.getDisponibilidadViaje() == null ? ""
                : " La categoría registrada acredita " + puntos + " de " + peso + " puntos."));
    }

    static Integer maximoHistorico(com.fasterxml.jackson.databind.JsonNode contexto) {
        if (contexto == null) return null;
        var maximo = contexto.path("maximoAplicable");
        if (maximo.isInt() && maximo.intValue() >= 0) return maximo.intValue();
        return null;
    }

    public List<Map<String, Object>> historial(String codigo) {
        return decisiones.findByCodigoNinoOrderByFechaAsc(codigo).stream().map(d -> {
            Map<String,Object> r = new LinkedHashMap<>();
            r.put("familia", d.getCodigoFamilia()); r.put("decision", d.getDecision());
            r.put("motivo", d.getMotivo()); r.put("por", d.getFuncionario()); r.put("fecha", d.getFecha()==null?null:d.getFecha().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
            Object contextoGuardado = d.getContextoScoring() == null ? null : decodificar(d.getContextoScoring());
            boolean sinCalculo = "REEVALUACION".equals(d.getDecision()) && contextoGuardado instanceof Map<?,?> contexto
                    && Boolean.TRUE.equals(contexto.get("sinCalculo"));
            r.put("sinCalculo", sinCalculo);
            r.put("puntaje", sinCalculo ? null : d.getPuntaje()); r.put("versionPesos", d.getVersionPesos());
            r.put("contextoScoring", sinCalculo ? null : contextoGuardado);
            r.put("maximoAplicable", sinCalculo ? null : maximoHistorico(json.valueToTree(contextoGuardado)));
            return r;
        }).toList();
    }

    @Transactional
    public Map<String, Object> decidir(String codigoNino, String codigoFamilia, String decision,
                                        String motivo, String funcionario) {
        return decidir(codigoNino,codigoFamilia,decision,motivo,funcionario,pesos().getVersion());
    }
    @Transactional
    public Map<String,Object> decidir(String codigoNino,String codigoFamilia,String decision,String motivo,String funcionario,Integer versionEsperada){
        return decidir(codigoNino,codigoFamilia,decision,motivo,funcionario,versionEsperada,null,null);
    }
    @Transactional
    public Map<String,Object> decidir(String codigoNino,String codigoFamilia,String decision,String motivo,String funcionario,Integer versionEsperada,List<String> apartados,String categoria){
        if("REEVALUACION".equals(decision) && (apartados!=null || categoria!=null)){
            if(apartados==null || apartados.isEmpty() || apartados.size()>5 || apartados.stream().anyMatch(java.util.Objects::isNull) || new HashSet<>(apartados).size()!=apartados.size() || !Set.of("Hogar y red de apoyo","Preferencias familiares","Salud y acompañamiento","Disponibilidad y entorno","Apoyos culturales y comunitarios").containsAll(apartados) || categoria==null || !Set.of("INCOMPLETA","DESACTUALIZADA","ACLARACION","INCONSISTENCIAS").contains(categoria))throw new IllegalArgumentException("Selecciona apartados y un motivo de reevaluación válidos");
        }else if(apartados!=null || categoria!=null)throw new IllegalArgumentException("Los apartados corresponden únicamente a una reevaluación");
        PesosScoring vigentes=pesos();
        if(versionEsperada==null||versionEsperada!=vigentes.getVersion())throw new IllegalStateException("Los criterios de compatibilidad cambiaron mientras revisabas esta propuesta. Actualiza el análisis para continuar.");
        if (!Set.of("ACEPTADA", "DESCARTADA", "REEVALUACION").contains(decision))
            throw new IllegalArgumentException("Decisión desconocida");
        if (motivo == null || motivo.trim().length() < 10 || motivo.length() > 2000)
            throw new IllegalArgumentException("El motivo debe contener entre 10 y 2000 caracteres");
        PerfilNino n = ninos.bloquear(codigoNino)
                .orElseThrow(() -> new IllegalArgumentException("Caso infantil inexistente"));
        PerfilFamilia f = familias.bloquear(codigoFamilia)
                .orElseThrow(() -> new IllegalArgumentException("Caso familiar inexistente"));
        if(excluida(codigoNino,codigoFamilia))throw new IllegalStateException("Esta combinación tiene una restricción vigente o una interrupción pendiente de cierre y revisión; no está disponible para una nueva propuesta automática.");
        if (!n.isPerfilCompleto() || n.isBajaLogica() || n.isCierrePorAdopcion() || !f.isPerfilCompleto() || f.isReevaluacionPendiente())
            throw new IllegalStateException("Solo se revisan casos completos y sin reevaluación pendiente");
        if (ninoReservado(codigoNino))
            throw new IllegalStateException("Este caso ya tiene una aceptación registrada");
        if (familiaReservada(codigoFamilia))
            throw new IllegalStateException("La familia ya tiene una propuesta aceptada; no participa en nuevas propuestas");
        PesosScoring p = vigentes;
        DecisionComite d = new DecisionComite();
        d.setCodigoNino(codigoNino); d.setCodigoFamilia(codigoFamilia); d.setDecision(decision);
        d.setApartadosReevaluacion(apartados==null?null:List.copyOf(apartados));d.setCategoriaReevaluacion(categoria);
        d.setMotivo(motivo.trim()); d.setFuncionario(funcionario); d.setFecha(LocalDateTime.now());
        if ("REEVALUACION".equals(decision)) {
            // El esquema exige un entero; la proyección lo expone como no calculado, nunca como cero puntos.
            d.setPuntaje(0);
            d.setVersionPesos(p.getVersion());
            d.setContextoScoring(codificar(Map.of("tipo","SOLICITUD_REEVALUACION","sinCalculo",true)));
        } else {
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
        }
        decisiones.save(d);
        if (decision.equals("ACEPTADA")) vinculaciones.crear(d,n,f);
        if (decision.equals("REEVALUACION")) {
            // La solicitud se audita ahora, pero solo cuenta cuando la familia
            // envía una entrevista corregida completa.
            f.setReevaluacionPendiente(true);
            familias.save(f);
        }
        return Map.of("mensaje", decision.equals("ACEPTADA") ? "La propuesta se envió a la familia" : decision.equals("DESCARTADA")?"El rechazo de esta propuesta quedó registrado. Esta combinación no volverá a aparecer en las recomendaciones automáticas.":"La decisión quedó registrada", "decision", decision,
                "numeroReevaluaciones", f.getNumeroReevaluaciones(),
                "alertaRevisionTecnica", f.getNumeroReevaluaciones() >= 3);
    }

    /** Metadatos institucionales de registros reales revisados; nunca se modifica la puntuación. */
    public Map<String,Object> antecedenteInterrupcion(String codigo){
        if(interrupciones==null)return Map.of("revisada",false,"cierrePendiente",false);
        var previas=interrupciones.findByVinculacionFamiliaCodigoCasoAndFechaRevisionIsNotNull(codigo);
        return Map.of("revisada",!previas.isEmpty(),"cierrePendiente",previas.stream().anyMatch(i->i.getVinculacion().getEstado()!=com.senniaf.match.model.Vinculacion.Estado.CERRADA));
    }

    public Map<String,Integer> valoresPesos(PesosScoring p) {
        return Map.of("edad",p.getEdad(),"hermanos",p.getHermanos(),"salud",p.getSalud(),"apoyo",p.getApoyo(),"logistica",p.getLogistica());
    }
    public List<Map<String,Object>> historialPesos() {
        pesos();
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
