package com.senniaf.match.service;

import com.senniaf.match.model.PerfilFamilia;
import com.senniaf.match.model.PerfilNino;
import com.senniaf.match.model.EvaluacionPsicologica;
import com.senniaf.match.model.enums.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public final class AnalizadorTextoCompatibilidad {
    public enum Dominio { SALUD, HERMANOS, RED, LOGISTICA }
    public enum Declaracion { NECESIDAD, ACTUAL, CON_APOYO, NEGATIVA, AMBIGUA, INSUFICIENTE }
    public enum Origen { ESTRUCTURADO, TEXTO }

    public record Evidencia(String campo, String original, Declaracion declaracion, Origen origen) {}
    public record Hallazgo(String concepto, String nombre, Dominio dominio,
                           Declaracion declaracion, List<Evidencia> evidencias) {}
    public record Analisis(Map<String, Hallazgo> conceptos, List<String> limitaciones,
                           List<Evidencia> sinCorrespondencia) {
        public Hallazgo obtener(String concepto) {
            return conceptos.get(concepto);
        }

        public List<Hallazgo> necesidadesSalud() {
            return conceptos.values().stream()
                    .filter(h -> h.dominio() == Dominio.SALUD)
                    .filter(h -> h.declaracion() == Declaracion.NECESIDAD
                            || h.declaracion() == Declaracion.AMBIGUA)
                    .toList();
        }
    }

    private record Concepto(String id, String nombre, Dominio dominio, Pattern expresion) {}
    private static final Map<String, Concepto> VOCABULARIO = vocabulario();
    private static final Set<String> ACOMPANAMIENTO_COTIDIANO = Set.of("RUTINA_ESTABLE",
            "ACOMPANAMIENTO_EMOCIONAL", "APOYO_ADAPTACION", "SUPERVISION_COTIDIANA",
            "SUPERVISION_PERMANENTE", "APOYO_EDUCATIVO_INDIVIDUALIZADO", "APOYO_EDUCATIVO_ESPECIALIZADO",
            "AYUDA_TAREAS", "SEGUIMIENTO_ESCOLAR", "REFUERZO_ESCOLAR");
    private static final Pattern NECESIDAD_EXPLICITA = Pattern.compile(
            "\\b(?:necesita|requiere|precisa|necesidad de|continuar|se recomienda(?:n)?)\\b");
    private static final Pattern RECOMENDACION_HIPOTETICA = Pattern.compile(
            "\\b(?:valorar|evaluar si|posible|posiblemente|quizas|quiza|se sospecha|hipotesis|por confirmar)\\b");
    private static final Pattern NECESIDAD_DESCARTADA = Pattern.compile(
            "\\b(?:se descarto|descartad[oa]|ya no (?:requiere|necesita)|dejo de (?:necesitar|requerir))\\b");
    private static final Pattern RECOMENDACION_CONDICIONAL = Pattern.compile("\\bsi\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISPONIBILIDAD_LIMITADA = Pattern.compile(
            "\\b(?:no siempre|solo a veces|solamente a veces|a veces|no en todo momento|no todos los dias)\\b");
    private static final Pattern CONTEXTO_TEMPORAL = Pattern.compile(
            "\\b(antes|en el pasado|anteriormente|ahora|actualmente|hoy|en la actualidad)\\b");
    private static final Pattern CONDICION_DEL_CASO = Pattern.compile("\\bsi (?:tiene|presenta|padece)\\s+");
    private static final Pattern PRESTACION_PSICOLOGICA = Pattern.compile(
            "\\b(?:realizar|dar|ofrecer|brindar)\\s+(?:la |el )?(?:terapias? psicologicas?|"
                    + "(?:atencion|tratamiento) psicologic[oa])\\b");
    private static final String ACCION_INFINITIVA = "(?:acompanar|atender|cuidar|apoyar|asistir|llevar|"
            + "seguir|administrar|ayudar|recibir|acoger|criar|viajar|trasladar|desplazar|facilitar|ofrecer|mantener|proporcionar|realizar|"
            + "brindar|dar|organizar|vigilar|supervisar|garantizar|ir|hacer(?:me|nos|se)? cargo de|"
            + "estar pendientes de|echar(?:le)? una mano con)(?:l[oa]s?|les?|me|nos)?";
    private static final Pattern CLAUSULAS = Pattern.compile(
            "\\b(?:pero|sin embargo|aunque)\\b|(?:,|\\by\\b)\\s*(?=(?:(?:ahora|actualmente)\\s+)?(?:no\\s+)?"
                    + "(?:puedo|podemos|podria|podriamos|necesita|requiere|tiene|presenta|valorar|evaluar|continuar|el nino|la nina)\\b)"
                    + "|,\\s*(?=no\\b)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DUDA = Pattern.compile(
            "\\b(?:tal vez|quizas|quiza|puede que|no se si|no se como|no sabemos si|no sabemos como|no tengo claro|no tenemos claro|no estoy segur[oa]|"
                    + "no estamos seguros|posible|posiblemente|se sospecha|por confirmar|descartar|si fuera posible|"
                    + "si pudiera|intentaria|nos gustaria|me gustaria)\\b");
    private static final Pattern CONDICION = Pattern.compile(
            "\\b(?:con (?:apoyo|ayuda|capacitacion|formacion|asistencia)"
                    + "|si (?:contamos|cuento|recibimos|recibo|tenemos|tengo|disponemos|dispongo|hay)\\b.{3,})");
    private static final Pattern CAPACIDAD = Pattern.compile(
            "\\b(?:(no|nunca|jamas|tampoco)\\s+)?(?:(?:puedo|podemos|podria|podriamos|podre|podremos|"
                    + "estoy dispuest[oa] a|estamos dispuest[oa]s a|"
                    + "estoy preparad[oa] para|estamos preparad[oa]s para|"
                    + "tengo capacidad para|tenemos capacidad para|tengo disponibilidad para|tenemos disponibilidad para|"
                    + "estamos disponibles para|estoy disponible para|tenemos disposicion para|tengo disposicion para|"
                    + "estamos en condiciones de|estoy en condiciones de|nos sentimos capaces de|me siento capaz de|"
                    + "me comprometo a|nos comprometemos a|nuestra familia puede|la familia puede|"
                    + "no (?:tengo|tenemos) (?:dificultades|impedimentos|ningun impedimento|ninguna dificultad) para)\\s+"
                    + ACCION_INFINITIVA
                    + "|acompano|acompanamos|atiendo|atendemos|cuido|cuidamos|apoyamos|"
                    + "asistimos|llevamos|ayudamos|recibimos|viajo|viajamos|mantengo|mantenemos|"
                    + "ofrezco|ofrecemos|proporciono|proporcionamos)\\b");
    private static final Pattern ACCION_COORDINADA = Pattern.compile(
            "\\b(y|ni)\\s+(?:(no|nunca|jamas|tampoco)\\s+|(no siempre|solo a veces|a veces)\\s+)?"
                    + ACCION_INFINITIVA + "\\b");
    private static final Pattern TERCERO = Pattern.compile(
            "\\b(?:mis?|sus?|nuestros?|nuestras?|el|la) (?:herman[oa]s?|madres?|padres?|hij[oa]s?|sobrin[oa]s?|tios?|tias?|abuel[oa]s?|pareja|amig[oa]s?)\\b"
                    + "|\\b(?:otra persona|otro nino|un familiar)\\b");
    private static final Pattern PREDICADO_NECESIDAD = Pattern.compile(
            "\\b((?:no|nunca|jamas)\\s+)?(?:tiene|presenta|requiere|necesita|precisa|padece)\\b");
    private static final Pattern NEGACION_DIRECTA = Pattern.compile(
            "\\b(?:sin|excepto|no|ni)\\s+(?:(?:el|la|los|las|diagnostico de)\\s+)?$");
    private static final Pattern SUJETO_CLINICO = Pattern.compile(
            "\\b([a-z]+)\\s+(?:tiene|presenta|padece)\\b");
    private static final Set<String> CONTEXTO_INFANTIL = Set.of(
            "nino", "nina", "adolescente", "menor", "paciente", "no", "nunca", "jamas",
            "actualmente", "ahora", "hoy", "tambien", "ademas", "que", "se");
    private static final Pattern NEGACION_RED = Pattern.compile(
            "\\b(?:no|nunca|jamas|tampoco)\\s+(?:(?:contamos|cuento|disponemos|dispongo|tenemos|tengo)"
                    + "|nos (?:ayuda|apoya|ayudan|apoyan)|(?:puede|pueden) (?:ayudarnos|apoyarnos))\\b");
    private static final Pattern FAMILIAR_CONCRETO = Pattern.compile(
            "\\b(?:mi|mis|nuestro|nuestra|nuestros|nuestras) (?:herman[oa]s?|madres?|padres?|"
                    + "sobrin[oa]s?|tios?|tias?|abuel[oa]s?)\\b");
    private static final Pattern NEGACION_RED_GLOBAL = Pattern.compile(
            "\\b(?:no|nunca|jamas|tampoco) (?:contamos|cuento|disponemos|dispongo|tenemos|tengo)\\b");
    private static final Pattern CONDICION_NEGADA = Pattern.compile(
            "\\bsi (?:no|nunca|jamas|tampoco)\\s+(?:contamos|cuento|recibimos|recibo|tenemos|tengo|"
                    + "disponemos|dispongo|hay)\\b");
    private static final Pattern CONDICION_INCIERTA = Pattern.compile(
            "\\b(?:si|siempre que|a condicion de|solo cuando|a menos que|salvo que)\\b");
    private static final Pattern ANAFORA = Pattern.compile(
            "\\b(?:acompanarl[oa]s?|atenderl[oa]s?|cuidarl[oa]s?|esas terapias|esa condicion)\\b");
    private static final Pattern RED_DECLARADA = Pattern.compile(
            "\\b(?:(?:no|nunca|jamas|tampoco)\\s+)?(?:contamos|cuento|disponemos|dispongo) con\\b"
                    + "|\\b(?:(?:no|nunca|jamas|tampoco)\\s+)?(?:tenemos|tengo) (?:una )?red de apoyo\\b"
                    + "|\\bnos (?:ayuda|apoya|ayudan|apoyan)\\b"
                    + "|\\b(?:puede|pueden) (?:ayudarnos|apoyarnos)\\b"
                    + "|\\b(?:tenemos|tengo) personas? que nos pueden ayudar\\b");
    private static final Pattern CAPACIDAD_CUIDADOS_RED = Pattern.compile(
            "\\b(?:(no|nunca|jamas|tampoco)\\s+)?(puede|pueden|podria|podrian)\\s+"
                    + "(?:acompanar|ayudar|cuidar|atender)(?:l[oa]s?|nos)?\\b");

    public Analisis necesidades(PerfilNino n) {
        return necesidades(n, List.of());
    }

    public Analisis necesidades(PerfilNino n, List<EvaluacionPsicologica> evaluaciones) {
        Acumulador datos = new Acumulador();
        agregarNecesidades(datos, n.getCondicionesSalud(), "condicionesSalud",
                n.getCondicionesSaludDetalle(), "NECESIDAD_SIN_PRECISAR");
        agregarNecesidades(datos, n.getAlergias(), "alergias",
                n.getAlergiasDetalle(), "NECESIDAD_SIN_PRECISAR");
        if (n.getIntoleranciaLactosa() == RespuestaSiNoNoSabe.SI) {
            datos.estructurado("INTOLERANCIA_LACTOSA", "intoleranciaLactosa", Declaracion.NECESIDAD, "SI");
        } else if (n.getIntoleranciaLactosa() == null
                || n.getIntoleranciaLactosa() == RespuestaSiNoNoSabe.NO_SABE) {
            datos.estructurado("INTOLERANCIA_LACTOSA", "intoleranciaLactosa", Declaracion.AMBIGUA,
                    String.valueOf(n.getIntoleranciaLactosa()));
        }
        analizarCampo(datos, "condicionesSaludDetalle", n.getCondicionesSaludDetalle(),
                EnumSet.of(Dominio.SALUD), true);
        analizarCampo(datos, "alergiasDetalle", n.getAlergiasDetalle(), EnumSet.of(Dominio.SALUD), true);
        analizarCampo(datos, "necesidadesApoyo", n.getNecesidadesApoyo(),
                EnumSet.of(Dominio.SALUD, Dominio.RED), true);
        analizarCampo(datos, "hermanosDetalle", n.getHermanosDetalle(),
                EnumSet.of(Dominio.HERMANOS), true);
        analizarRecomendaciones(datos, evaluaciones);
        comprobarAusenciaDeclarada(datos, n.getCondicionesSalud(), CondicionSalud.NINGUNA_CONOCIDA,
                "condicionesSalud", EnumSet.of(Dominio.SALUD), false);
        comprobarAusenciaDeclarada(datos, n.getAlergias(), Alergia.NINGUNA_CONOCIDA,
                "alergias", EnumSet.of(Dominio.SALUD), true);
        if (n.getIntoleranciaLactosa() == RespuestaSiNoNoSabe.NO
                && datos.tieneNecesidad("INTOLERANCIA_LACTOSA")) {
            datos.estructurado("INTOLERANCIA_LACTOSA", "intoleranciaLactosa", Declaracion.NEGATIVA, "NO");
        }
        completarOtra(datos, n.getCondicionesSalud(), "condicionesSaludDetalle", "NECESIDAD_SIN_PRECISAR", false);
        completarOtra(datos, n.getAlergias(), "alergiasDetalle", "NECESIDAD_SIN_PRECISAR", true);
        return datos.resultado();
    }

    private void analizarRecomendaciones(Acumulador datos, List<EvaluacionPsicologica> evaluaciones) {
        if (evaluaciones == null || evaluaciones.isEmpty()) return;
        Set<Long> sustituidas = new HashSet<>();
        evaluaciones.stream().filter(e -> e.getRevisionDe() != null)
                .forEach(e -> sustituidas.add(e.getRevisionDe().getId()));
        for (var evaluacion : evaluaciones) {
            if (evaluacion.getId() == null || sustituidas.contains(evaluacion.getId())) continue;
            String campo = "recomendacionesPsicologicas:" + evaluacion.getId() + ":v"
                    + (evaluacion.getNumeroVersion() == null ? 1 : evaluacion.getNumeroVersion());
            Acumulador recomendacion = new Acumulador();
            analizarCampo(recomendacion, campo, evaluacion.getRecomendaciones(), EnumSet.of(Dominio.SALUD), true);
            recomendacion.datos.forEach((concepto, evidencias) -> evidencias.forEach(e -> {
                String texto = normalizar(e.original());
                if ((!NECESIDAD_EXPLICITA.matcher(texto).find() && !NECESIDAD_DESCARTADA.matcher(texto).find())
                        || necesidadNoConfirmada(e.original(), texto)) return;
                Declaracion tipo = NECESIDAD_DESCARTADA.matcher(texto).find() ? Declaracion.NEGATIVA : e.declaracion();
                if (tipo == Declaracion.INSUFICIENTE) return;
                // El resultado conserva concepto y procedencia, nunca párrafos del informe clínico.
                datos.agregar(concepto, new Evidencia(campo, concepto.nombre(), tipo, Origen.TEXTO));
            }));
        }
    }

    private void completarOtra(Acumulador datos, List<? extends Enum<?>> valores,
                                String campo, String pendiente, boolean alergia) {
        if (valores == null || valores.stream().noneMatch(v -> v != null && v.name().equals("OTRA"))) return;
        boolean descrita = datos.datos.entrySet().stream()
                .filter(e -> !alergia || e.getKey().id().startsWith("ALERGIA_"))
                .anyMatch(e -> e.getValue().stream().anyMatch(f -> f.campo().equals(campo)
                        && f.declaracion() == Declaracion.NECESIDAD));
        if (!descrita) datos.estructurado(pendiente, campo.replace("Detalle", ""), Declaracion.AMBIGUA, valores.toString());
    }

    public Analisis capacidades(PerfilFamilia f) {
        Acumulador datos = new Acumulador();
        agregarCapacidades(datos, f.getCondicionesPuedeAcompanarHoy(),
                "condicionesPuedeAcompanarHoy", Declaracion.ACTUAL);
        agregarCapacidades(datos, f.getCondicionesConApoyo(), "condicionesConApoyo", Declaracion.CON_APOYO);
        if (f.getDisposicionHermanos() == DisposicionHermanos.DISPUESTA) {
            datos.estructurado("HERMANOS", "disposicionHermanos", Declaracion.ACTUAL, f.getDisposicionHermanos().name());
        } else if (f.getDisposicionHermanos() == DisposicionHermanos.DISPUESTA_CON_DUDAS) {
            datos.estructurado("HERMANOS", "disposicionHermanos", Declaracion.CON_APOYO, f.getDisposicionHermanos().name());
        } else if (f.getDisposicionHermanos() == DisposicionHermanos.PREFIERE_UN_SOLO_NINO) {
            datos.estructurado("HERMANOS", "disposicionHermanos", Declaracion.NEGATIVA, f.getDisposicionHermanos().name());
        }
        if (f.getRedApoyo() != null && !f.getRedApoyo().isEmpty()) {
            boolean ninguna = f.getRedApoyo().contains(FuenteApoyo.NINGUNA_POR_AHORA);
            datos.estructurado("RED_APOYO", "redApoyo",
                    ninguna ? Declaracion.NEGATIVA : Declaracion.ACTUAL, f.getRedApoyo().toString());
            if (f.getRedApoyo().stream().anyMatch(Objects::isNull) || ninguna && f.getRedApoyo().size() > 1) {
                datos.estructurado("RED_APOYO", "redApoyo", Declaracion.AMBIGUA, f.getRedApoyo().toString());
            }
        }
        analizarCampo(datos, "notaCondicionesSalud", f.getNotaCondicionesSalud(),
                EnumSet.of(Dominio.SALUD), false);
        analizarAcompanamientoCotidiano(datos, "notaRedApoyo", f.getNotaRedApoyo());
        analizarAcompanamientoCotidiano(datos, "notaEstructuraHogar", f.getNotaEstructuraHogar());
        analizarCampo(datos, "notaHermanos", f.getNotaHermanos(), EnumSet.of(Dominio.HERMANOS), false);
        analizarRed(datos, f);
        analizarCampo(datos, "notaDistancia", f.getNotaDistancia(), EnumSet.of(Dominio.LOGISTICA), false);
        return datos.resultado();
    }

    private void analizarAcompanamientoCotidiano(Acumulador datos, String campo, String texto) {
        Acumulador notas = new Acumulador();
        analizarCampo(notas, campo, texto, EnumSet.of(Dominio.SALUD), false);
        notas.datos.forEach((concepto, evidencias) -> {
            if (ACOMPANAMIENTO_COTIDIANO.contains(concepto.id())) {
                evidencias.forEach(e -> datos.agregar(concepto, e));
            }
        });
    }

    private void analizarRed(Acumulador datos, PerfilFamilia familia) {
        Acumulador notas = new Acumulador();
        analizarCampo(notas, "notaRedApoyo", familia.getNotaRedApoyo(), EnumSet.of(Dominio.RED), false);
        boolean fuenteIndependiente = familia.getRedApoyo() != null && familia.getRedApoyo().stream()
                .anyMatch(f -> f == FuenteApoyo.PAREJA || f == FuenteApoyo.AMISTADES_O_COMUNIDAD);
        notas.datos.forEach((concepto, evidencias) -> evidencias.forEach(evidencia -> {
            String texto = normalizar(evidencia.original());
            boolean limitacionParticular = concepto.id().equals("RED_APOYO") && fuenteIndependiente
                    && FAMILIAR_CONCRETO.matcher(texto).find()
                    && !NEGACION_RED_GLOBAL.matcher(texto).find()
                    && evidencia.declaracion() != Declaracion.ACTUAL;
            // La limitación de un familiar no invalida otra fuente de apoyo declarada.
            datos.agregar(concepto, limitacionParticular
                    ? new Evidencia(evidencia.campo(), evidencia.original(), Declaracion.INSUFICIENTE, evidencia.origen())
                    : evidencia);
        }));
        notas.sinCorrespondencia.forEach(e -> datos.sinCorrespondencia(e.campo(), e.original()));
    }

    public Analisis analizarTexto(String campo, String original, Dominio dominio, boolean necesidad) {
        Acumulador datos = new Acumulador();
        analizarCampo(datos, campo, original, EnumSet.of(dominio), necesidad);
        return datos.resultado();
    }

    private void agregarNecesidades(Acumulador datos, List<? extends Enum<?>> valores,
                                    String campo, String detalle, String desconocido) {
        if (valores == null || valores.isEmpty() || valores.stream().anyMatch(Objects::isNull)
                || valores.stream().anyMatch(v -> v != null && v.name().equals("INFORMACION_NO_DISPONIBLE"))) {
            datos.estructurado(desconocido, campo, Declaracion.AMBIGUA, String.valueOf(valores));
        }
        if (valores == null) return;
        for (Enum<?> valor : valores) {
            if (valor == null) continue;
            String id = valor instanceof Alergia ? "ALERGIA_" + valor.name() : valor.name();
            if (VOCABULARIO.containsKey(id)) datos.estructurado(id, campo, Declaracion.NECESIDAD, valor.name());
            if (valor.name().equals("OTRA") && conceptosEn(detalle, EnumSet.of(Dominio.SALUD)).isEmpty()) {
                datos.estructurado(desconocido, campo, Declaracion.AMBIGUA, String.valueOf(valores));
            }
        }
    }

    private void agregarCapacidades(Acumulador datos, List<CondicionSalud> valores,
                                     String campo, Declaracion declaracion) {
        if (valores == null) return;
        boolean invalida = valores.stream().anyMatch(c -> c == null
                || c == CondicionSalud.NINGUNA_CONOCIDA
                || c == CondicionSalud.INFORMACION_NO_DISPONIBLE);
        for (CondicionSalud valor : valores) {
            if (valor != null && VOCABULARIO.containsKey(valor.name())) {
                datos.estructurado(valor.name(), campo, invalida ? Declaracion.AMBIGUA : declaracion);
            }
        }
    }

    private void comprobarAusenciaDeclarada(Acumulador datos, List<?> valores, Object ninguna,
                                            String campo, Set<Dominio> dominios, boolean alergias) {
        if (valores == null || !valores.contains(ninguna)) return;
        for (Concepto concepto : VOCABULARIO.values()) {
            boolean delCatalogo = alergias ? concepto.id().startsWith("ALERGIA_")
                    : Arrays.stream(CondicionSalud.values()).anyMatch(c -> c.name().equals(concepto.id()));
            if (dominios.contains(concepto.dominio()) && delCatalogo && datos.tieneNecesidad(concepto.id())) {
                datos.estructurado(concepto.id(), campo, Declaracion.NEGATIVA, ninguna.toString());
            }
        }
    }

    private void analizarCampo(Acumulador datos, String campo, String original,
                               Set<Dominio> dominios, boolean necesidad) {
        if (original == null || original.isBlank()) return;
        // La anáfora solo conserva un concepto de la misma oración y del mismo sujeto.
        for (String oracion : original.split("(?<=[.!?;\\n])")) {
            List<Concepto> anteriores = List.of();
            boolean antecedentePropio = false;
            boolean condicionPrevia = oracion.stripLeading().matches("(?i)^si\\b.*");
            boolean condicionIdentificada = CONDICION.matcher(normalizar(oracion)).find()
                    && !CONDICION_NEGADA.matcher(normalizar(oracion)).find();
            for (String fragmento : CLAUSULAS.split(normalizarSeparadores(oracion))) {
                String texto = normalizar(fragmento);
                if (texto.isBlank()) continue;
                List<Concepto> conceptos = conceptosEn(texto, dominios);
                if (conceptos.isEmpty() && antecedentesValidos(texto, anteriores, antecedentePropio)) {
                    conceptos = anteriores;
                }
                boolean tercero = TERCERO.matcher(texto).find();
                if (necesidad) {
                    var sujeto = SUJETO_CLINICO.matcher(texto);
                    while (sujeto.find()) {
                        if (!CONTEXTO_INFANTIL.contains(sujeto.group(1))) tercero = true;
                    }
                }
                for (Concepto concepto : conceptos) {
                    Declaracion tipo = necesidad && necesidadNoConfirmada(fragmento, texto)
                            ? Declaracion.INSUFICIENTE : interpretar(texto, concepto, necesidad, tercero);
                    if (!necesidad && tipo == Declaracion.ACTUAL && condicionPrevia) {
                        tipo = condicionIdentificada ? Declaracion.CON_APOYO : Declaracion.AMBIGUA;
                    }
                    datos.agregar(concepto, new Evidencia(campo, fragmento.strip(), tipo, Origen.TEXTO));
                }
                String restante = texto;
                for (Concepto concepto : conceptos) {
                    restante = concepto.expresion().matcher(restante).replaceAll(" ");
                }
                if (necesidad && dominios.contains(Dominio.SALUD) && !tercero
                        && !necesidadNoConfirmada(fragmento, texto)
                        && !NECESIDAD_DESCARTADA.matcher(texto).find()
                        && necesidadSinEquivalencia(restante)) {
                    datos.agregar(VOCABULARIO.get("NECESIDAD_SIN_PRECISAR"),
                            new Evidencia(campo, fragmento.strip(), Declaracion.AMBIGUA, Origen.TEXTO));
                } else if (conceptos.isEmpty() && CAPACIDAD.matcher(texto).find()) {
                    datos.sinCorrespondencia(campo, fragmento.strip());
                }
                if (!conceptos.isEmpty()) {
                    anteriores = conceptos;
                    antecedentePropio = !tercero;
                }
            }
        }
    }

    private boolean antecedentesValidos(String texto, List<Concepto> anteriores, boolean propio) {
        return propio && anteriores.size() == 1 && ANAFORA.matcher(texto).find()
                && CAPACIDAD.matcher(texto).find();
    }

    private boolean necesidadNoConfirmada(String original, String texto) {
        return original.contains("¿") || original.contains("?")
                || RECOMENDACION_HIPOTETICA.matcher(texto).find()
                || RECOMENDACION_CONDICIONAL.matcher(original).find()
                || texto.matches(".*\\b(?:antecedentes de|historia de|antes tuvo|antes tenia)\\b.*");
    }

    private Declaracion interpretar(String texto, Concepto concepto, boolean necesidad, boolean tercero) {
        if (tercero && concepto.dominio() != Dominio.RED) return Declaracion.INSUFICIENTE;
        if (necesidad && NECESIDAD_DESCARTADA.matcher(texto).find()) return Declaracion.NEGATIVA;
        if (DUDA.matcher(texto).find()) return Declaracion.AMBIGUA;
        var mencion = concepto.expresion().matcher(texto);
        int posicion = mencion.find() ? mencion.start() : texto.length();
        String anterior = texto.substring(0, posicion);
        if (necesidad) {
            if (tercero || texto.matches(".*\\b(?:le gusta|disfruta|prefiere|le gustaria|quisiera)\\b.*")) {
                return Declaracion.INSUFICIENTE;
            }
            if (NEGACION_DIRECTA.matcher(anterior).find()) return Declaracion.NEGATIVA;
            var predicado = PREDICADO_NECESIDAD.matcher(anterior);
            boolean negado = false;
            while (predicado.find()) negado = predicado.group(1) != null;
            if (negado) return Declaracion.NEGATIVA;
            if (ACOMPANAMIENTO_COTIDIANO.contains(concepto.id())
                    && !NECESIDAD_EXPLICITA.matcher(anterior).find()) return Declaracion.INSUFICIENTE;
            return Declaracion.NECESIDAD;
        }
        var declaracionRed = RED_DECLARADA.matcher(texto);
        if (concepto.dominio() == Dominio.RED && declaracionRed.find()) {
            if (NEGACION_RED.matcher(texto).find()) return Declaracion.NEGATIVA;
            if (concepto.id().equals("CUIDADOS_DIARIOS")) {
                var capacidadRed = CAPACIDAD_CUIDADOS_RED.matcher(texto);
                if (capacidadRed.find()) {
                    if (capacidadRed.group(1) != null) return Declaracion.NEGATIVA;
                    if (capacidadRed.group(2).startsWith("podri")) return Declaracion.AMBIGUA;
                }
            }
            return interpretarCondicion(texto, declaracionRed.end());
        }
        if (tercero) return Declaracion.INSUFICIENTE;
        if (NEGACION_DIRECTA.matcher(anterior).find()) return Declaracion.NEGATIVA;
        var acciones = CAPACIDAD.matcher(texto).results().toList();
        if (acciones.isEmpty()) return Declaracion.INSUFICIENTE;
        var accion = acciones.get(0);
        for (var candidata : acciones) {
            if (candidata.start() <= posicion) accion = candidata;
        }
        if (accion.group(1) != null) return Declaracion.NEGATIVA;
        int inicioAmbito = 0;
        int finAmbito = texto.length();
        int finAccion = accion.end();
        boolean negada = false;
        boolean limitada = false;
        var coordinadas = ACCION_COORDINADA.matcher(texto);
        while (coordinadas.find()) {
            if (coordinadas.start() < accion.end()) continue;
            if (coordinadas.start() > posicion) {
                finAmbito = coordinadas.start();
                break;
            }
            inicioAmbito = coordinadas.start();
            finAccion = coordinadas.end();
            negada = coordinadas.group(1).equals("ni") || coordinadas.group(2) != null;
            limitada = coordinadas.group(3) != null;
        }
        if (negada) return Declaracion.NEGATIVA;
        String ambito = texto.substring(inicioAmbito, finAmbito);
        if (limitada || DISPONIBILIDAD_LIMITADA.matcher(ambito).find()) return Declaracion.AMBIGUA;
        if (esExperienciaPasada(ambito, finAccion - inicioAmbito)) return Declaracion.INSUFICIENTE;
        if (concepto.id().equals("ACOMPANAMIENTO_PSICOLOGICO")
                && PRESTACION_PSICOLOGICA.matcher(ambito).find()) return Declaracion.INSUFICIENTE;
        Declaracion condicion = interpretarCondicion(ambito, finAccion - inicioAmbito, concepto);
        if (condicion != Declaracion.ACTUAL) return condicion;
        if (accion.group().matches(".*\\b(?:podria|podriamos|podre|podremos)\\b.*")) {
            return Declaracion.AMBIGUA;
        }
        return Declaracion.ACTUAL;
    }

    private Declaracion interpretarCondicion(String texto, int finAccion) {
        return interpretarCondicion(texto, finAccion, null);
    }

    private Declaracion interpretarCondicion(String texto, int finAccion, Concepto concepto) {
        if (CONDICION_NEGADA.matcher(texto).find()) return Declaracion.AMBIGUA;
        if (CONDICION.matcher(texto).find()) return Declaracion.CON_APOYO;
        String posterior = texto.substring(finAccion);
        if (concepto != null) {
            var caso = CONDICION_DEL_CASO.matcher(posterior);
            if (caso.find()) {
                String descripcion = posterior.substring(caso.end()).replaceFirst("[.!;,]+$", "").strip();
                if (concepto.expresion().matcher(descripcion).matches()) return Declaracion.ACTUAL;
            }
        }
        return CONDICION_INCIERTA.matcher(posterior).find()
                ? Declaracion.AMBIGUA : Declaracion.ACTUAL;
    }

    private boolean esExperienciaPasada(String texto, int finAccion) {
        var marcadores = CONTEXTO_TEMPORAL.matcher(texto.substring(0, finAccion));
        boolean pasada = false;
        while (marcadores.find()) {
            pasada = Set.of("antes", "en el pasado", "anteriormente").contains(marcadores.group());
        }
        return pasada;
    }

    private boolean necesidadSinEquivalencia(String texto) {
        String concepto = "(?:\\w*terapias?|rehabilitacion|tratamientos?|medicacion|alergias?|"
                + "enfermedad|sindrome|(?:hemo)?dialisis|cuidados especificos|atencion medica|apoyo especializado|"
                + "apoyo escolar|apoyo emocional|acompanamiento psicologico|acompanamiento especializado|"
                + "intervencion (?:psicologica|especializada)|"
                + "ayuda para (?:vestirse|alimentarse|asearse|movilizarse))";
        boolean necesidadExpresa = NECESIDAD_EXPLICITA.matcher(texto).find()
                || texto.matches("^\\s*" + concepto + "\\b.*");
        return necesidadExpresa && texto.matches(".*\\b" + concepto + "\\b.*")
                && !texto.matches(".*\\b(?:no|nunca|jamas) (?:necesita|requiere|precisa)\\b.*");
    }

    private static String normalizarSeparadores(String texto) {
        return texto.replaceAll("(?i)\\bpero\\b", ";pero;")
                .replaceAll("(?i)\\bsin embargo\\b", ";sin embargo;")
                .replaceAll("(?i)\\baunque\\b", ";aunque;");
    }

    public static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private static List<Concepto> conceptosEn(String texto, Set<Dominio> dominios) {
        String normalizado = normalizar(texto);
        return VOCABULARIO.values().stream().filter(c -> dominios.contains(c.dominio()))
                .filter(c -> c.expresion().matcher(normalizado).find()).toList();
    }

    private static Map<String, Concepto> vocabulario() {
        Map<String, Concepto> conceptos = new LinkedHashMap<>();
        definir(conceptos, "ASMA", "Asma", Dominio.SALUD, "asma");
        definir(conceptos, "DIABETES", "Diabetes", Dominio.SALUD, "diabetes");
        definir(conceptos, "EPILEPSIA", "Epilepsia", Dominio.SALUD, "epilepsia");
        definir(conceptos, "TDAH", "TDAH", Dominio.SALUD,
                "tdah|(?:trastorno por )?deficit de atencion e hiperactividad");
        definir(conceptos, "TEA", "TEA", Dominio.SALUD, "tea|(?:trastorno del )?espectro autista");
        for (String tipo : List.of("FISICA", "INTELECTUAL", "VISUAL", "AUDITIVA")) {
            String nombre = tipo.equals("FISICA") ? "física" : normalizar(tipo);
            String plural = normalizar(tipo) + (tipo.endsWith("AL") ? "es" : "s");
            definir(conceptos, "DISCAPACIDAD_" + tipo, "Discapacidad " + nombre,
                    Dominio.SALUD, "discapacidad(?:es)? (?:" + normalizar(tipo) + "|" + plural + ")");
        }
        definir(conceptos, "INTOLERANCIA_LACTOSA", "Intolerancia a la lactosa", Dominio.SALUD,
                "intolerancia (?:a la |a )?lactosa|intolerantes? a la lactosa");
        definir(conceptos, "ALERGIA_ALIMENTARIA", "Alergia alimentaria", Dominio.SALUD, "alergias? alimentarias?|alergias? (?:a los |a )alimentos");
        definir(conceptos, "ALERGIA_MEDICAMENTOS", "Alergia a medicamentos", Dominio.SALUD,
                "alergias? (?:a los |a )medicamentos");
        definir(conceptos, "ALERGIA_AMBIENTAL_POLEN", "Alergia ambiental o al polen", Dominio.SALUD,
                "alergias? ambientales?|alergias? al polen|alergic[oa]s? al polen");
        definir(conceptos, "ALERGIA_PICADURA_INSECTOS", "Alergia a picaduras de insectos", Dominio.SALUD,
                "alergias? a (?:las )?picaduras? de insectos|alergic[oa]s? a (?:las )?picaduras? de insectos");
        definir(conceptos, "TERAPIA_LENGUAJE", "Terapia de lenguaje", Dominio.SALUD,
                "terapias? (?:de|del) lenguaje");
        definir(conceptos, "FISIOTERAPIA", "Fisioterapia", Dominio.SALUD, "fisioterapias?|terapias? fisicas?");
        definir(conceptos, "TERAPIA_OCUPACIONAL", "Terapia ocupacional", Dominio.SALUD,
                "terapias? ocupacional(?:es)?");
        definir(conceptos, "ACOMPANAMIENTO_PSICOLOGICO", "Acompañamiento psicológico", Dominio.SALUD,
                "acompanamiento psicologico|sesion(?:es)? psicologicas?|sesion(?:es)? de psicologia|"
                        + "terapias? psicologicas?|seguimiento psicologico|citas? con (?:el|la) psicolog[oa]|"
                        + "(?:atencion|tratamiento) psicologic[oa]|ir con (?:el|ella|el nino|la nina) al psicolog[oa]");
        definir(conceptos, "RUTINA_ESTABLE", "Rutinas estables", Dominio.SALUD,
                "rutinas? (?:(?:diarias?|cotidianas?) )?(?:estables?|constantes?|consistentes?|fijas?)|horarios? (?:estables?|constantes?|consistentes?|regulares?)");
        definir(conceptos, "ACOMPANAMIENTO_EMOCIONAL", "Acompañamiento emocional", Dominio.SALUD,
                "(?:acompanamiento|apoyo) emocional(?: especific[oa])?|apoyar(?:l[oa]s?)? emocionalmente");
        definir(conceptos, "APOYO_ADAPTACION", "Apoyo durante la adaptación", Dominio.SALUD,
                "proceso de adaptacion|(?:su|la) adaptacion|apoyo para adaptarse|adaptarse al hogar|se acostumbre al (?:nuevo )?hogar");
        definir(conceptos, "SUPERVISION_COTIDIANA", "Supervisión cotidiana", Dominio.SALUD,
                "supervision (?:cotidiana|diaria)|(?:supervision y acompanamiento|acompanamiento y supervision) (?:diario|cotidiano)|"
                        + "(?:vigilar(?:l[oa]s?)?|supervisar(?:l[oa]s?)?|(?:l[oa] )?vigil(?:e|en)) (?:todos los dias|diariamente|a diario)");
        definir(conceptos, "SUPERVISION_PERMANENTE", "Supervisión permanente", Dominio.SALUD,
                "supervision (?:permanente|continua|las 24 horas|24 ?h|24/7)|supervisar(?:l[oa]s?)? (?:las )?(?:veinticuatro|24) horas");
        definir(conceptos, "APOYO_EDUCATIVO_INDIVIDUALIZADO", "Apoyo educativo individualizado", Dominio.SALUD,
                "(?:apoyo|acompanamiento) (?:educativo|escolar) individualizado|apoyo uno a uno para aprender");
        definir(conceptos, "APOYO_EDUCATIVO_ESPECIALIZADO", "Apoyo educativo especializado", Dominio.SALUD,
                "(?:apoyo|acompanamiento) (?:educativo|escolar) especializado");
        definir(conceptos, "AYUDA_TAREAS", "Ayuda con tareas", Dominio.SALUD,
                "(?:ayuda|apoyo) (?:especific[oa] )?(?:con|en|para) (?:sus |las )?tareas|ayudar(?:l[oa]s?)? con (?:sus |las )?tareas|"
                        + "(?:ayudar(?:l[oa]s?)? a|(?:ayuda|apoyo) para) (?:hacer|realizar) (?:sus |las )?tareas|"
                        + "echar(?:le)? una mano con (?:sus |las )?tareas");
        definir(conceptos, "SEGUIMIENTO_ESCOLAR", "Seguimiento escolar", Dominio.SALUD,
                "seguimiento (?:educativo|escolar|de (?:sus |las )?actividades escolares)|seguir (?:sus |las )?actividades escolares|estar pendientes de (?:sus |las )?actividades escolares");
        definir(conceptos, "REFUERZO_ESCOLAR", "Refuerzo escolar", Dominio.SALUD, "refuerzo escolar");
        definir(conceptos, "HERMANOS", "Acompañamiento de hermanos", Dominio.HERMANOS,
                "herman[oa]s?|hermanit[oa]s|grupo de hermanos|ubicacion conjunta|permanecer juntos");
        definir(conceptos, "RED_APOYO", "Red de apoyo", Dominio.RED,
                "red de apoyo|persona de apoyo|apoyo familiar|ayuda familiar|familiares|mi hermana|mi madre|mi padre|personas? que nos pueden ayudar");
        definir(conceptos, "CUIDADOS_DIARIOS", "Apoyo en cuidados diarios", Dominio.RED,
                "cuidados? diarios?|cuidados? cotidianos?|actividades (?:cotidianas|diarias)|quehaceres (?:diarios|cotidianos)");
        definir(conceptos, "VIAJE", "Traslados", Dominio.LOGISTICA,
                "viajar|viajes|trasladarme|trasladarnos|traslados|desplazarme|desplazarnos");
        definir(conceptos, "VISITAS", "Recepción de visitas", Dominio.LOGISTICA, "visitas");
        definir(conceptos, "NECESIDAD_SIN_PRECISAR", "Necesidad sin equivalencia definida",
                Dominio.SALUD, "(?!)");
        return Collections.unmodifiableMap(conceptos);
    }

    private static void definir(Map<String, Concepto> conceptos, String id, String nombre,
                                Dominio dominio, String expresion) {
        conceptos.put(id, new Concepto(id, nombre, dominio,
                Pattern.compile("(?<![a-z0-9])(?:" + expresion + ")(?![a-z0-9])")));
    }

    private static final class Acumulador {
        private final Map<Concepto, List<Evidencia>> datos = new LinkedHashMap<>();
        private final List<Evidencia> sinCorrespondencia = new ArrayList<>();

        void sinCorrespondencia(String campo, String original) {
            boolean repetida = sinCorrespondencia.stream().anyMatch(e -> e.campo().equals(campo)
                    && normalizar(e.original()).equals(normalizar(original)));
            if (!repetida) sinCorrespondencia.add(new Evidencia(campo, original,
                    Declaracion.INSUFICIENTE, Origen.TEXTO));
        }

        void estructurado(String id, String campo, Declaracion tipo) {
            estructurado(id, campo, tipo, id);
        }

        void estructurado(String id, String campo, Declaracion tipo, String original) {
            agregar(VOCABULARIO.get(id), new Evidencia(campo, original, tipo, Origen.ESTRUCTURADO));
        }

        void agregar(Concepto concepto, Evidencia evidencia) {
            List<Evidencia> fuentes = datos.computeIfAbsent(concepto, c -> new ArrayList<>());
            boolean repetida = fuentes.stream().anyMatch(e -> e.campo().equals(evidencia.campo())
                    && e.declaracion() == evidencia.declaracion() && e.origen() == evidencia.origen());
            if (!repetida) fuentes.add(evidencia);
        }

        boolean tieneNecesidad(String id) {
            return datos.getOrDefault(VOCABULARIO.get(id), List.of()).stream()
                    .anyMatch(e -> e.declaracion() == Declaracion.NECESIDAD);
        }

        Analisis resultado() {
            Map<String, Hallazgo> conceptos = new LinkedHashMap<>();
            datos.forEach((concepto, fuentes) -> conceptos.put(concepto.id(),
                    new Hallazgo(concepto.id(), concepto.nombre(), concepto.dominio(), resolver(fuentes),
                            List.copyOf(fuentes))));
            List<String> limitaciones = conceptos.values().stream()
                    .filter(h -> h.declaracion() == Declaracion.AMBIGUA
                            || h.declaracion() == Declaracion.INSUFICIENTE)
                    .map(h -> h.nombre() + ": información ambigua o insuficiente").toList();
            return new Analisis(Collections.unmodifiableMap(conceptos), limitaciones, List.copyOf(sinCorrespondencia));
        }

        private Declaracion resolver(List<Evidencia> fuentes) {
            Set<Declaracion> tipos = EnumSet.noneOf(Declaracion.class);
            fuentes.forEach(e -> tipos.add(e.declaracion()));
            if (tipos.contains(Declaracion.AMBIGUA)) return Declaracion.AMBIGUA;
            Set<Declaracion> estructuradas = EnumSet.noneOf(Declaracion.class);
            fuentes.stream().filter(e -> e.origen() == Origen.ESTRUCTURADO)
                    .forEach(e -> estructuradas.add(e.declaracion()));
            if (estructuradas.contains(Declaracion.ACTUAL) && estructuradas.contains(Declaracion.CON_APOYO)) {
                return Declaracion.AMBIGUA;
            }
            if (tipos.contains(Declaracion.NEGATIVA) && (tipos.contains(Declaracion.ACTUAL)
                    || tipos.contains(Declaracion.CON_APOYO) || tipos.contains(Declaracion.NECESIDAD))) {
                return Declaracion.AMBIGUA;
            }
            if (tipos.contains(Declaracion.NEGATIVA)) return Declaracion.NEGATIVA;
            if (tipos.contains(Declaracion.NECESIDAD)) return Declaracion.NECESIDAD;
            if (tipos.contains(Declaracion.CON_APOYO)) return Declaracion.CON_APOYO;
            if (tipos.contains(Declaracion.ACTUAL)) return Declaracion.ACTUAL;
            return Declaracion.INSUFICIENTE;
        }
    }
}
