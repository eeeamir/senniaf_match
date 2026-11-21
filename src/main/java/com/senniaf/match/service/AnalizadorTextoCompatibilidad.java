package com.senniaf.match.service;

import com.senniaf.match.model.PerfilFamilia;
import com.senniaf.match.model.PerfilNino;
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
    private static final Pattern CLAUSULAS = Pattern.compile(
            "\\b(?:pero|sin embargo|aunque)\\b|(?:,|\\by\\b)\\s*(?=(?:no\\s+)?"
                    + "(?:puedo|podemos|podria|podriamos|necesita|requiere|tiene|presenta|el nino|la nina)\\b)"
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
                    + "tengo capacidad para|tenemos capacidad para|"
                    + "me comprometo a|nos comprometemos a)\\s+"
                    + "(?:acompanar|atender|cuidar|apoyar|asistir|llevar|seguir|administrar|ayudar|"
                    + "recibir|acoger|criar|viajar|trasladar|desplazar)(?:l[oa]s?|me|nos)?"
                    + "|acompano|acompanamos|atiendo|atendemos|cuido|cuidamos|apoyamos|"
                    + "asistimos|llevamos|ayudamos|recibimos|viajo|viajamos)\\b");
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
                    + "|nos (?:ayuda|apoya|ayudan|apoyan))\\b");
    private static final Pattern CONDICION_NEGADA = Pattern.compile(
            "\\bsi (?:no|nunca|tampoco)\\s+(?:contamos|cuento|recibimos|recibo|tenemos|tengo|"
                    + "disponemos|dispongo|hay)\\b");
    private static final Pattern CONDICION_INCIERTA = Pattern.compile(
            "\\b(?:si|siempre que|a condicion de|solo cuando|a menos que|salvo que)\\b");
    private static final Pattern ANAFORA = Pattern.compile(
            "\\b(?:acompanarl[oa]s?|atenderl[oa]s?|cuidarl[oa]s?|esas terapias|esa condicion)\\b");
    private static final Pattern RED_DECLARADA = Pattern.compile(
            "\\b(?:(?:no|nunca|jamas|tampoco)\\s+)?(?:contamos|cuento|disponemos|dispongo) con\\b"
                    + "|\\b(?:(?:no|nunca|jamas|tampoco)\\s+)?(?:tenemos|tengo) (?:una )?red de apoyo\\b"
                    + "|\\bnos (?:ayuda|apoya|ayudan|apoyan)\\b");

    public Analisis necesidades(PerfilNino n) {
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
        analizarCampo(datos, "notaHermanos", f.getNotaHermanos(), EnumSet.of(Dominio.HERMANOS), false);
        analizarCampo(datos, "notaRedApoyo", f.getNotaRedApoyo(), EnumSet.of(Dominio.RED), false);
        analizarCampo(datos, "notaDistancia", f.getNotaDistancia(), EnumSet.of(Dominio.LOGISTICA), false);
        return datos.resultado();
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
        for (String oracion : original.split("[.!?;\\n]+")) {
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
                    Declaracion tipo = interpretar(texto, concepto, necesidad, tercero);
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

    private Declaracion interpretar(String texto, Concepto concepto, boolean necesidad, boolean tercero) {
        if (tercero && concepto.dominio() != Dominio.RED) return Declaracion.INSUFICIENTE;
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
            if (texto.matches(".*\\b(?:antecedentes de|historia de|antes tuvo|antes tenia)\\b.*")) {
                return Declaracion.INSUFICIENTE;
            }
            return Declaracion.NECESIDAD;
        }
        var declaracionRed = RED_DECLARADA.matcher(texto);
        if (concepto.dominio() == Dominio.RED && declaracionRed.find()) {
            if (NEGACION_RED.matcher(texto).find()) return Declaracion.NEGATIVA;
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
        Declaracion condicion = interpretarCondicion(texto, accion.end());
        if (condicion != Declaracion.ACTUAL) return condicion;
        if (accion.group().matches(".*\\b(?:podria|podriamos|podre|podremos)\\b.*")) {
            return Declaracion.AMBIGUA;
        }
        return Declaracion.ACTUAL;
    }

    private Declaracion interpretarCondicion(String texto, int finAccion) {
        if (CONDICION_NEGADA.matcher(texto).find()) return Declaracion.AMBIGUA;
        if (CONDICION.matcher(texto).find()) return Declaracion.CON_APOYO;
        return CONDICION_INCIERTA.matcher(texto.substring(finAccion)).find()
                ? Declaracion.AMBIGUA : Declaracion.ACTUAL;
    }

    private boolean necesidadSinEquivalencia(String texto) {
        String concepto = "(?:\\w*terapias?|rehabilitacion|tratamientos?|medicacion|alergias?|"
                + "enfermedad|sindrome|cuidados especificos|atencion medica|apoyo especializado|"
                + "apoyo escolar|apoyo emocional|acompanamiento psicologico|"
                + "ayuda para (?:vestirse|alimentarse|asearse|movilizarse))";
        boolean necesidadExpresa = texto.matches(".*\\b(?:necesita|requiere|precisa)\\b.*")
                || texto.matches("^\\s*" + concepto + "\\b.*");
        return necesidadExpresa && texto.matches(".*\\b" + concepto + "\\b.*")
                && !texto.matches(".*\\bno (?:necesita|requiere|precisa)\\b.*");
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
                "tdah|trastorno por deficit de atencion e hiperactividad");
        definir(conceptos, "TEA", "TEA", Dominio.SALUD, "tea|trastorno del espectro autista");
        for (String tipo : List.of("FISICA", "INTELECTUAL", "VISUAL", "AUDITIVA")) {
            String nombre = tipo.equals("FISICA") ? "física" : normalizar(tipo);
            definir(conceptos, "DISCAPACIDAD_" + tipo, "Discapacidad " + nombre,
                    Dominio.SALUD, "discapacidad " + normalizar(tipo));
        }
        definir(conceptos, "INTOLERANCIA_LACTOSA", "Intolerancia a la lactosa", Dominio.SALUD,
                "intolerancia (?:a la |a )?lactosa");
        definir(conceptos, "ALERGIA_ALIMENTARIA", "Alergia alimentaria", Dominio.SALUD, "alergias? alimentarias?");
        definir(conceptos, "ALERGIA_MEDICAMENTOS", "Alergia a medicamentos", Dominio.SALUD,
                "alergias? (?:a los |a )medicamentos");
        definir(conceptos, "ALERGIA_AMBIENTAL_POLEN", "Alergia ambiental o al polen", Dominio.SALUD,
                "alergias? ambientales?|alergias? al polen");
        definir(conceptos, "ALERGIA_PICADURA_INSECTOS", "Alergia a picaduras de insectos", Dominio.SALUD,
                "alergias? a (?:las )?picaduras? de insectos");
        definir(conceptos, "TERAPIA_LENGUAJE", "Terapia de lenguaje", Dominio.SALUD,
                "terapias? (?:de|del) lenguaje");
        definir(conceptos, "FISIOTERAPIA", "Fisioterapia", Dominio.SALUD, "fisioterapias?|terapias? fisicas?");
        definir(conceptos, "TERAPIA_OCUPACIONAL", "Terapia ocupacional", Dominio.SALUD,
                "terapias? ocupacionales?");
        definir(conceptos, "HERMANOS", "Acompañamiento de hermanos", Dominio.HERMANOS,
                "herman[oa]s?|grupo de hermanos|ubicacion conjunta|permanecer juntos");
        definir(conceptos, "RED_APOYO", "Red de apoyo", Dominio.RED,
                "red de apoyo|apoyo familiar|ayuda familiar|familiares|mi hermana|mi madre|mi padre");
        definir(conceptos, "CUIDADOS_DIARIOS", "Apoyo en cuidados diarios", Dominio.RED,
                "cuidados? diarios?|cuidados? cotidianos?");
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
