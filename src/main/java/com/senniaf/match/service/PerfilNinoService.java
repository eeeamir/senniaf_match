package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilNinoRequest;
import com.senniaf.match.dto.PerfilNinoResponse;
import com.senniaf.match.dto.EvaluacionPsicologicaRequest;
import com.senniaf.match.dto.EvaluacionPsicologicaResponse;
import com.senniaf.match.model.EvaluacionPsicologica;
import com.senniaf.match.model.SolicitudEstadoCaso;
import com.senniaf.match.repository.SolicitudEstadoCasoRepository;
import com.senniaf.match.dto.SolicitudEstadoRequest;
import com.senniaf.match.dto.SolicitudEstadoResponse;
import com.senniaf.match.dto.ResolverSolicitudRequest;
import com.senniaf.match.repository.EvaluacionPsicologicaRepository;
import org.springframework.transaction.annotation.Transactional;
import com.senniaf.match.model.PerfilNino;
import com.senniaf.match.model.EventoCaso;
import com.senniaf.match.model.enums.Alergia;
import com.senniaf.match.model.enums.CondicionSalud;
import com.senniaf.match.model.enums.RespuestaSiNoNoSabe;
import com.senniaf.match.repository.PerfilNinoRepository;
import com.senniaf.match.repository.EventoCasoRepository;
import com.senniaf.match.security.RolNoAutorizadoException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

/** Expedientes y actuaciones profesionales: identidad de sesión, vinculación por especialidad e historial sin sobrescrituras. */
@Service
public class PerfilNinoService {

    @org.springframework.beans.factory.annotation.Autowired
    private com.senniaf.match.repository.UsuarioRepository usuarios;
    @org.springframework.beans.factory.annotation.Value("${senniaf.demo.asignacion-automatica:false}")
    private boolean asignacionDemo;
    private final PerfilNinoRepository repositorio;
    private final EventoCasoRepository auditoria;
    private final EvaluacionPsicologicaRepository evaluaciones;
    private final SolicitudEstadoCasoRepository solicitudes;

    public PerfilNinoService(PerfilNinoRepository repositorio) { this(repositorio,null,null,null); }
    public PerfilNinoService(PerfilNinoRepository repositorio, EventoCasoRepository auditoria) { this(repositorio,auditoria,null,null); }
    @org.springframework.beans.factory.annotation.Autowired
    public PerfilNinoService(PerfilNinoRepository repositorio, EventoCasoRepository auditoria,
                             EvaluacionPsicologicaRepository evaluaciones, SolicitudEstadoCasoRepository solicitudes) {
        this.repositorio=repositorio;this.auditoria=auditoria;this.evaluaciones=evaluaciones;this.solicitudes=solicitudes;
    }

    @Transactional
    public synchronized PerfilNinoResponse registrarPerfil(PerfilNinoRequest request, String registradoPor) {
        request.validarCoherencia();
        String referencia = request.getNumeroExpedienteInstitucional();
        referencia = referencia == null || referencia.isBlank() ? null
                : referencia.strip().replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT);
        if (referencia != null && (referencia.length() > 100 || !referencia.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} ._/-]{0,99}")))
            throw new IllegalArgumentException("Revisa la referencia institucional: utiliza hasta 100 caracteres alfanuméricos");
        repositorio.bloquearAltas();
        if (referencia != null && repositorio.existsByNumeroExpedienteInstitucional(referencia))
            throw new IllegalStateException("Ya existe un expediente con esa referencia institucional");
        String codigoCaso = siguienteCodigoCaso();

        PerfilNino perfil = new PerfilNino();
        perfil.setCodigoCaso(codigoCaso);
        perfil.setNumeroExpedienteInstitucional(referencia);
        copiarDatos(perfil, request);
        perfil.setRegistradoPor(registradoPor);
        perfil.setTrabajadorSocialAsignado(registradoPor);
        if (asignacionDemo) {
            String psicologo = usuarios.findAll().stream().filter(u -> u.isVerificado() && "PSICOLOGO".equals(u.getRol()))
                    .map(com.senniaf.match.model.Usuario::getIdentificador)
                    .min(Comparator.<String>comparingLong(id -> repositorio.countByPsicologoAsignado(id)).thenComparing(id -> id))
                    .orElseThrow(() -> new IllegalStateException("La demostración requiere un psicólogo habilitado antes de crear expedientes"));
            perfil.setPsicologoAsignado(psicologo);
        }
        perfil.setFechaRegistro(LocalDateTime.now());

        boolean completo = calcularSiEstaCompleto(request);
        perfil.setPerfilCompleto(completo);

        boolean alertaMedica = calcularAlertaMedica(request);
        perfil.setAlertaMedica(alertaMedica);

        repositorio.save(perfil);
        auditar(codigoCaso, registradoPor, "CREACION", "Se creó el perfil infantil. Trabajo social: " + registradoPor + "; psicología: " + (perfil.getPsicologoAsignado() == null ? "pendiente" : perfil.getPsicologoAsignado()) + (asignacionDemo ? ". Asignación exclusiva de demostración." : ""));

        String mensaje = completo
                ? "Perfil registrado correctamente"
                : "Perfil registrado, pero está incompleto: no se incluirá en el motor de match hasta completarse";
        if (alertaMedica) mensaje += ". Este perfil requiere atención y revisión médica por el Comité.";
        return new PerfilNinoResponse(mensaje, codigoCaso, completo, alertaMedica);
    }

    private void copiarDatos(PerfilNino perfil, PerfilNinoRequest request) {
        perfil.setEdad(request.getEdad());
        perfil.setNivelEscolar(request.getNivelEscolar());
        boolean noEscolarizado = request.getNivelEscolar() == com.senniaf.match.model.enums.NivelEscolar.NO_ESCOLARIZADO;
        perfil.setGradoEscolar(noEscolarizado ? "No aplica" : request.getGradoEscolar());
        perfil.setRendimientoAcademico(noEscolarizado ? com.senniaf.match.model.enums.RendimientoAcademico.NO_APLICA : request.getRendimientoAcademico());
        perfil.setHabilidades(request.getHabilidades());
        perfil.setHabilidadesDetalle(request.getHabilidadesDetalle());
        perfil.setGustos(request.getGustos());
        perfil.setGustosDetalle(request.getGustosDetalle());
        perfil.setPersonalidad(request.getPersonalidad());
        perfil.setPersonalidadDetalle(request.getPersonalidadDetalle());
        perfil.setAlergias(request.getAlergias());
        perfil.setAlergiasDetalle(request.getAlergiasDetalle());
        perfil.setIntoleranciaLactosa(request.getIntoleranciaLactosa());
        perfil.setCondicionesSalud(request.getCondicionesSalud());
        perfil.setCondicionesSaludDetalle(request.getCondicionesSaludDetalle());
        perfil.setNecesidadesApoyo(request.getNecesidadesApoyo());
        perfil.setTieneHermanos(request.getTieneHermanos());
        perfil.setCantidadHermanos(request.getTieneHermanos() == com.senniaf.match.model.enums.SiNo.NO ? 0
                : request.getTieneHermanos() == com.senniaf.match.model.enums.SiNo.SI ? request.getCantidadHermanos() : null);
        perfil.setHermanosDetalle(request.getHermanosDetalle());
        perfil.setContextoCultural(request.getContextoCultural());
    }

    public List<PerfilNino> listarPerfiles(String actor, String rol, boolean incluirInactivos) {
        return switch (rol) {
            case "TRABAJADOR_SOCIAL", "PSICOLOGO" -> repositorio.findAsignados(actor, rol, incluirInactivos);
            case "COMITE" -> repositorio.findAll().stream()
                    .sorted(Comparator.comparing(PerfilNino::getCodigoCaso)).toList();
            default -> throw new RolNoAutorizadoException("Rol no autorizado para consultar expedientes");
        };
    }

    public PerfilNino consultarExpediente(String codigo, String actor, String rol) {
        PerfilNino p = repositorio.findByCodigoCaso(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El caso no existe"));
        if (!"COMITE".equals(rol)) comprobarAsignacion(p, actor, rol);
        return p;
    }

    private void comprobarAsignacion(PerfilNino p, String actor, String rol) {
        boolean asignado = "TRABAJADOR_SOCIAL".equals(rol) && actor.equals(p.getTrabajadorSocialAsignado())
                || "PSICOLOGO".equals(rol) && actor.equals(p.getPsicologoAsignado());
        if (!asignado) throw new RolNoAutorizadoException("No tienes asignado este expediente en tu especialidad");
    }

    private PerfilNino vinculadoParaActualizar(String codigo, String actor, String rol) {
        PerfilNino p = repositorio.bloquear(codigo).orElseThrow(() -> new IllegalArgumentException("El caso no existe"));
        comprobarAsignacion(p, actor, rol);
        return p;
    }

    @Transactional
    public PerfilNinoResponse actualizar(String codigo, String actor, PerfilNinoRequest request) {
        request.validarCoherencia();
        PerfilNino p = vinculadoParaActualizar(codigo, actor, "TRABAJADOR_SOCIAL");
        var anteriores=datosSociales(p);
        copiarDatos(p, request);
        p.setPerfilCompleto(calcularSiEstaCompleto(request));
        p.setAlertaMedica(calcularAlertaMedica(request));
        repositorio.save(p);
        auditar(codigo, actor, "EDICION", cambiosSociales(anteriores,datosSociales(p)));
        return new PerfilNinoResponse("Perfil actualizado correctamente", codigo, p.isPerfilCompleto(), p.isAlertaMedica());
    }

    public List<EventoCaso> historial(String codigo, String actor, String rol) {
        consultarExpediente(codigo, actor, rol);
        return auditoria == null ? List.of() : auditoria.findByCodigoCasoOrderByFechaAsc(codigo);
    }

    @Transactional(readOnly = true)
    public List<EvaluacionPsicologicaResponse> consultarEvaluaciones(String codigo, String actor, String rol) {
        consultarExpediente(codigo, actor, rol);
        var lista = evaluaciones.findByExpedienteCodigoCasoOrderByFechaDescIdDesc(codigo);
        var anteriores = lista.stream().filter(e -> e.getRevisionDe()!=null)
                .map(e -> e.getRevisionDe().getId()).collect(java.util.stream.Collectors.toSet());
        return lista.stream().map(e -> EvaluacionPsicologicaResponse.de(e,!anteriores.contains(e.getId()))).toList();
    }

    @Transactional
    public EvaluacionPsicologicaResponse registrarEvaluacion(String codigo, String actor, String rol,
                                                             EvaluacionPsicologicaRequest request) {
        if (!"PSICOLOGO".equals(rol)) throw new RolNoAutorizadoException("Solo psicología registra evaluaciones psicológicas");
        PerfilNino p = vinculadoParaActualizar(codigo, actor, rol);
        EvaluacionPsicologica e = new EvaluacionPsicologica();
        e.setExpediente(p); e.setProfesional(actor); e.setFecha(LocalDateTime.now()); e.setNumeroVersion(1);
        e.setEvaluacion(request.evaluacion().trim()); e.setObservaciones(request.observaciones().trim());
        e.setRecomendaciones(request.recomendaciones().trim());
        copiarValoracion(e,request); evaluaciones.save(e);
        auditar(codigo, actor, "EVALUACION_PSICOLOGICA", "Evaluación registrada: " + e.getId());
        return EvaluacionPsicologicaResponse.de(e,true);
    }

    @Transactional
    public EvaluacionPsicologicaResponse corregirEvaluacion(String codigo, Long id, String actor, String rol,
                                                            EvaluacionPsicologicaRequest request) {
        if (!"PSICOLOGO".equals(rol)) throw new RolNoAutorizadoException("Solo el autor psicológico vinculado puede corregir su informe");
        PerfilNino p = vinculadoParaActualizar(codigo,actor,rol);
        EvaluacionPsicologica anterior = evaluaciones.findById(id).orElseThrow(() -> new IllegalArgumentException("La evaluación no existe"));
        if (!p.getId().equals(anterior.getExpediente().getId()) || !actor.equals(anterior.getProfesional()))
            throw new RolNoAutorizadoException("No puedes corregir una evaluación de otro expediente o profesional");
        if (evaluaciones.existsByRevisionDeId(id)) throw new IllegalStateException("El informe ya fue corregido. Recarga su versión vigente");
        String motivo = request.motivoCorreccion()==null || request.motivoCorreccion().isBlank()?null:request.motivoCorreccion().strip();
        if(motivo!=null && motivo.length()>2000)throw new IllegalArgumentException("El detalle de actualización admite hasta 2000 caracteres");
        // Cada actualización conserva el informe previo; el motivo histórico es opcional.
        EvaluacionPsicologica nueva = new EvaluacionPsicologica();
        nueva.setExpediente(p); nueva.setProfesional(actor); nueva.setFecha(LocalDateTime.now());
        nueva.setEvaluacion(request.evaluacion().trim()); nueva.setObservaciones(request.observaciones().trim());
        nueva.setRecomendaciones(request.recomendaciones().trim()); nueva.setRevisionDe(anterior);
        nueva.setNumeroVersion((anterior.getNumeroVersion()==null ? 1 : anterior.getNumeroVersion())+1);
        nueva.setMotivoCorreccion(motivo); copiarValoracion(nueva,request);
        if(request.contextoEvaluacion()==null)nueva.setContextoEvaluacion(anterior.getContextoEvaluacion());
        evaluaciones.save(nueva);
        auditar(codigo,actor,"CORRECCION_PSICOLOGICA","Informe #"+id+" -> revisión #"+nueva.getId());
        return EvaluacionPsicologicaResponse.de(nueva,true);
    }

    private void copiarValoracion(EvaluacionPsicologica e,EvaluacionPsicologicaRequest r) {
        if(r.contextoEvaluacion()!=null){
            // Defensa también para llamadas internas que no pasan por @Valid del controlador.
            try(var fabrica=jakarta.validation.Validation.buildDefaultValidatorFactory()){
                var errores=fabrica.getValidator().validate(r.contextoEvaluacion());
                if(!errores.isEmpty())throw new IllegalArgumentException(errores.stream().map(jakarta.validation.ConstraintViolation::getMessage).sorted().findFirst().orElse("Revisa el contexto de la evaluación"));
            }
            e.setContextoEvaluacion(r.contextoEvaluacion().serializar());
        }
        var areas=r.areasObservadas()==null?java.util.List.<String>of():r.areasObservadas();
        var permitidas=java.util.Set.of("ADAPTACION_EMOCIONAL","INTERACCION_SOCIAL","CONDUCTA","APRENDIZAJE","RELACIONES_AFECTIVAS","ACOMPANAMIENTO");
        if(areas.size()>6 || areas.stream().anyMatch(a->a==null||!permitidas.contains(a)) || new java.util.HashSet<>(areas).size()!=areas.size())
            throw new IllegalArgumentException("Revisa las áreas evaluadas seleccionadas");
        String diagnostico=r.diagnosticoProfesional()==null?null:r.diagnosticoProfesional().strip();
        if(diagnostico!=null && diagnostico.isEmpty())diagnostico=null;
        if(Boolean.TRUE.equals(r.sinDiagnostico()) && diagnostico!=null)
            throw new IllegalArgumentException("No registres un diagnóstico si indicas que no se establece");
        if(Boolean.FALSE.equals(r.sinDiagnostico()) && diagnostico==null)
            throw new IllegalArgumentException("Registra el diagnóstico sustentado o selecciona que no se establece diagnóstico");
        if(diagnostico!=null && r.sinDiagnostico()==null)
            throw new IllegalArgumentException("Confirma si se establece un diagnóstico");
        e.setSinDiagnostico(r.sinDiagnostico());e.setDiagnosticoProfesional(diagnostico);
        e.setHallazgosProfesionales(r.hallazgosProfesionales()==null?null:r.hallazgosProfesionales().strip());
        try{e.setAreasObservadas(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(areas));}
        catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new IllegalStateException("No se pudo guardar la valoración",error);}
    }

    public long numeroSolicitudesPendientes(PerfilNino perfil) {
        return solicitudes.countByExpedienteIdAndEstado(perfil.getId(),SolicitudEstadoCaso.Estado.PENDIENTE);
    }

    @Transactional(readOnly=true)
    public List<SolicitudEstadoResponse> solicitudesPendientes(String rol) {
        if (!"COMITE".equals(rol)) throw new RolNoAutorizadoException("Solo el Comité consulta la bandeja de solicitudes");
        return solicitudes.findByEstadoOrderByFechaSolicitudAscIdAsc(SolicitudEstadoCaso.Estado.PENDIENTE).stream()
                .map(SolicitudEstadoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudEstadoResponse> consultarSolicitudes(String codigo, String actor, String rol) {
        consultarExpediente(codigo, actor, rol);
        return solicitudes.findByExpedienteCodigoCasoOrderByFechaSolicitudDescIdDesc(codigo).stream()
                .map(SolicitudEstadoResponse::de).toList();
    }

    @Transactional
    public SolicitudEstadoResponse solicitarCambioEstado(String codigo, String actor, String rol, SolicitudEstadoRequest request) {
        if (!"TRABAJADOR_SOCIAL".equals(rol))
            throw new RolNoAutorizadoException("Solo trabajo social responsable puede solicitar el cambio de estado");
        PerfilNino p = repositorio.bloquear(codigo).orElseThrow(() -> new IllegalArgumentException("El caso no existe"));
        comprobarAsignacion(p, actor, rol);
        comprobarEstado(p, request.tipo());
        if (solicitudes.existsByExpedienteIdAndEstado(p.getId(), SolicitudEstadoCaso.Estado.PENDIENTE))
            throw new IllegalStateException("Ya existe una solicitud pendiente para este expediente");
        SolicitudEstadoCaso s = new SolicitudEstadoCaso();
        if (request.categoria() == null || !request.categoria().admite(request.tipo()))
            throw new IllegalArgumentException("El motivo seleccionado no corresponde al tipo de solicitud");
        s.setCategoria(request.categoria());
        s.setExpediente(p); s.setTipo(request.tipo()); s.setSolicitante(actor);
        s.setFechaSolicitud(LocalDateTime.now()); s.setMotivo(motivoValido(request.motivo()));
        solicitudes.save(s);
        auditar(codigo, actor, "SOLICITUD_" + s.getTipo(), "Solicitud #" + s.getId() + "; el estado todavía no cambia");
        return SolicitudEstadoResponse.de(s);
    }

    @Transactional
    public SolicitudEstadoResponse resolverSolicitud(Long id, String actor, String rol, ResolverSolicitudRequest request) {
        if (!"COMITE".equals(rol)) throw new RolNoAutorizadoException("Solo el Comité puede resolver solicitudes");
        SolicitudEstadoCaso s = solicitudes.bloquear(id).orElseThrow(() -> new IllegalArgumentException("La solicitud no existe"));
        if (s.getEstado() != SolicitudEstadoCaso.Estado.PENDIENTE)
            throw new IllegalStateException("Esta solicitud ya fue resuelta");
        PerfilNino p = repositorio.bloquearPorId(s.getExpediente().getId()).orElseThrow();
        String motivo = motivoValido(request.motivo());
        if (request.aprobar()) {
            comprobarEstado(p, s.getTipo());
            if (s.getTipo() == SolicitudEstadoCaso.Tipo.REACTIVACION && !p.isPerfilCompleto())
                throw new IllegalStateException("Completa y revisa el expediente antes de aprobar su reactivación al Match");
            p.setBajaLogica(s.getTipo() == SolicitudEstadoCaso.Tipo.DESACTIVACION);
            if (s.getCategoria() == SolicitudEstadoCaso.Categoria.ADOPCION_FORMALIZADA)
                p.setCierrePorAdopcion(true);
        }
        s.setEstado(request.aprobar() ? SolicitudEstadoCaso.Estado.APROBADA : SolicitudEstadoCaso.Estado.RECHAZADA);
        s.setResueltoPor(actor); s.setFechaResolucion(LocalDateTime.now()); s.setMotivoResolucion(motivo);
        auditar(p.getCodigoCaso(), actor, s.getTipo() + "_" + s.getEstado(), "{\"resumen\":\"Resolución de solicitud #" + s.getId() + "\",\"justificacion\":"
                + jsonTexto(motivo) + "}");
        return SolicitudEstadoResponse.de(s);
    }

    private String jsonTexto(String valor) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(valor); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("No se pudo conservar la justificación", e); }
    }

    private void comprobarEstado(PerfilNino p, SolicitudEstadoCaso.Tipo tipo) {
        if (tipo == SolicitudEstadoCaso.Tipo.REACTIVACION && p.isCierrePorAdopcion())
            throw new IllegalStateException("La participación se cerró por adopción formalizada y no admite una reactivación ordinaria");
        if ((tipo == SolicitudEstadoCaso.Tipo.DESACTIVACION) == p.isBajaLogica())
            throw new IllegalStateException("El estado actual del expediente no permite esa solicitud");
    }

    private String motivoValido(String motivo) {
        if (motivo == null || motivo.trim().length() < 10 || motivo.length() > 2000)
            throw new IllegalArgumentException("El motivo debe contener entre 10 y 2000 caracteres");
        return motivo.trim();
    }

    private void auditar(String codigo, String actor, String accion, String descripcion) {
        if (auditoria == null) return; // soporte a pruebas unitarias antiguas sin repositorio de auditoría
        EventoCaso e = new EventoCaso(); e.setCodigoCaso(codigo); e.setActor(actor);
        e.setAccion(accion);
        String rol=accion.equals("CREACION")||accion.equals("EDICION")||accion.startsWith("SOLICITUD_")?"TRABAJADOR_SOCIAL"
                :accion.contains("PSICOLOGICA")?"PSICOLOGO":"COMITE";
        var detalles=new java.util.LinkedHashMap<String,Object>();detalles.put("rol",rol);
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            if(descripcion.startsWith("{"))detalles.putAll(mapper.readValue(descripcion,new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){}));
            else {detalles.put("resumen",descripcion);var m=java.util.regex.Pattern.compile("(?:Evaluación registrada: |revisión #|Solicitud #|solicitud #)(\\d+)").matcher(descripcion);while(m.find())detalles.put("referencia",Long.valueOf(m.group(1)));}
            String registro=mapper.writeValueAsString(detalles);
            e.setDescripcion(registro);
        }catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new IllegalStateException("No se pudo registrar la actuación",error);} e.setFecha(LocalDateTime.now()); auditoria.save(e);
    }

    private java.util.Map<String,Object> datosSociales(PerfilNino p) {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        java.util.Map<String,Object> datos=mapper.convertValue(p,new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){});
        datos.keySet().removeAll(java.util.Set.of("id","codigoCaso","fechaRegistro","registradoPor","trabajadorSocialAsignado","psicologoAsignado","numeroExpedienteInstitucional","bajaLogica","cierrePorAdopcion"));return datos;
    }
    private String cambiosSociales(java.util.Map<String,Object> antes,java.util.Map<String,Object> despues) {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var campos=new java.util.ArrayList<String>();
        despues.forEach((k,v)->{if(!java.util.Objects.equals(antes.get(k),v))campos.add(k);});
        var contenido=new java.util.LinkedHashMap<String,Object>();contenido.put("resumen","Se actualizó la información social.");contenido.put("campos",campos);
        var cambios=new java.util.LinkedHashMap<String,Object>();contenido.put("cambios",cambios);
        try {
            for(String campo:campos){var valores=new java.util.LinkedHashMap<String,Object>();valores.put("anterior",antes.get(campo));valores.put("nuevo",despues.get(campo));cambios.put(campo,valores);}
            return mapper.writeValueAsString(contenido);
        }catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new IllegalStateException("No se pudo registrar el detalle del cambio",error);}
    }

    private synchronized String siguienteCodigoCaso() {
        long numero = repositorio.count() + 1;
        while (repositorio.existsByCodigoCaso(String.format("NNA-%03d", numero))) numero++;
        return String.format("NNA-%03d", numero);
    }

    private boolean calcularSiEstaCompleto(PerfilNinoRequest r) {
        return r.getNivelEscolar() != null
                && (r.getNivelEscolar() == com.senniaf.match.model.enums.NivelEscolar.NO_ESCOLARIZADO
                    || r.getNivelEscolar() == com.senniaf.match.model.enums.NivelEscolar.INFORMACION_NO_DISPONIBLE
                    || r.getRendimientoAcademico() != null)
                && r.isEscolaridadCoherente() && r.isHermanosCoherentes()
                && noVacia(r.getHabilidades())
                && noVacia(r.getGustos())
                && noVacia(r.getPersonalidad())
                && noVacia(r.getAlergias())
                && r.getIntoleranciaLactosa() != null
                && noVacia(r.getCondicionesSalud())
                && noVacio(r.getNecesidadesApoyo())
                && r.getTieneHermanos() != null;
    }

    /**
     * Un perfil requiere atención médica particular del Comité cuando
     * declara alguna alergia o condición de salud distinta de "ninguna
     * conocida", o cuando la intolerancia a la lactosa es "SI". No es un
     * criterio de exclusión; es solo una señal de revisión prioritaria.
     */
    private boolean calcularAlertaMedica(PerfilNinoRequest r) {
        boolean tieneAlergiaRelevante = tieneValorDistintoDe(r.getAlergias(), Alergia.NINGUNA_CONOCIDA);
        boolean tieneCondicionRelevante = tieneValorDistintoDe(r.getCondicionesSalud(), CondicionSalud.NINGUNA_CONOCIDA);
        boolean intoleranteALactosa = r.getIntoleranciaLactosa() == RespuestaSiNoNoSabe.SI;
        return tieneAlergiaRelevante || tieneCondicionRelevante || intoleranteALactosa;
    }

    private <T> boolean tieneValorDistintoDe(List<T> lista, T valorNeutro) {
        return lista != null && lista.stream().anyMatch(valor -> valor != null && valor != valorNeutro
                && !(valor instanceof Enum<?> e && "INFORMACION_NO_DISPONIBLE".equals(e.name())));
    }

    private boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private boolean noVacia(List<?> lista) {
        return lista != null && !lista.isEmpty();
    }
}
