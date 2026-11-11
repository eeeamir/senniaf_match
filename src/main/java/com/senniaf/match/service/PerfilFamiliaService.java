package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilFamiliaRequest;
import com.senniaf.match.dto.PerfilFamiliaResponse;
import com.senniaf.match.model.PerfilFamilia;
import com.senniaf.match.repository.PerfilFamiliaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PerfilFamiliaService {

    private final PerfilFamiliaRepository repositorio;
    private final com.senniaf.match.repository.EventoCasoRepository auditoria;
    private final com.senniaf.match.repository.DecisionComiteRepository decisiones;

    public PerfilFamiliaService(PerfilFamiliaRepository repositorio) {
        this(repositorio,null);
    }
    public PerfilFamiliaService(PerfilFamiliaRepository repositorio, com.senniaf.match.repository.DecisionComiteRepository decisiones) {this(repositorio,decisiones,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public PerfilFamiliaService(PerfilFamiliaRepository repositorio, com.senniaf.match.repository.DecisionComiteRepository decisiones,com.senniaf.match.repository.EventoCasoRepository auditoria) {
        this.repositorio=repositorio;this.decisiones=decisiones;this.auditoria=auditoria;
    }

    @Transactional
    public synchronized PerfilFamiliaResponse registrarPerfil(PerfilFamiliaRequest request, String registradoPor) {
        request.validarCoherencia();
        repositorio.bloquearAltas();
        String cedula = registradoPor == null ? "" : registradoPor.trim();

        if (cedula.isBlank()) {
            throw new IllegalArgumentException("El identificador de la persona responsable es obligatorio");
        }

        if (repositorio.existsByRegistradoPor(cedula)) {
            throw new IllegalStateException(
                    "Ya existe un caso de familia registrado con el identificador " + cedula);
        }

        String codigoCaso = siguienteCodigoCaso();

        PerfilFamilia perfil = new PerfilFamilia();
        perfil.setCodigoCaso(codigoCaso);
        copiarDatos(perfil, request);
        perfil.setRegistradoPor(cedula);
        perfil.setFechaRegistro(LocalDateTime.now());

        boolean completo = calcularSiEstaCompleto(request);
        perfil.setPerfilCompleto(completo);

        repositorio.save(perfil);

        String mensaje = completo
                ? "Perfil registrado correctamente"
                : "Perfil registrado, pero está incompleto: no se incluirá en el motor de match hasta completarse";

        return respuesta(perfil, mensaje);
    }

    /** La familia actualiza su entrevista propia; una revisión solicitada se contabiliza al completarse. */
    @Transactional
    public PerfilFamiliaResponse actualizarPropio(PerfilFamiliaRequest request, String cedula) {
        request.validarCoherencia();
        PerfilFamilia perfil = repositorio.findByRegistradoPor(cedula)
                .orElseThrow(() -> new IllegalArgumentException("Todavía no existe una entrevista para modificar"));
        var antes=datosAuditoria(perfil);
        copiarDatos(perfil, request);
        boolean completo = calcularSiEstaCompleto(request);
        perfil.setPerfilCompleto(completo);
        // Abrir o abandonar el formulario nunca cuenta. Una solicitud pendiente
        // cuenta una sola vez al recibirse la corrección completa y válida.
        if (perfil.isReevaluacionPendiente() && completo) {
            perfil.setNumeroReevaluaciones(perfil.getNumeroReevaluaciones() + 1);
            perfil.setReevaluacionPendiente(false);
        }
        repositorio.save(perfil);
        auditarCorreccion(perfil,cedula,antes);
        return respuesta(perfil, perfil.isPerfilCompleto() ? "Entrevista actualizada para revisión del Comité"
                : "Los cambios se guardaron, pero la entrevista sigue incompleta y requiere corrección");
    }

    public java.util.Optional<PerfilFamilia> consultarDetallePropio(String cedula) {
        return repositorio.findByRegistradoPor(cedula == null ? "" : cedula.trim());
    }

    private PerfilFamiliaResponse respuesta(PerfilFamilia p, String mensaje) {
        var respuesta=new PerfilFamiliaResponse(mensaje, p.getCodigoCaso(), p.isPerfilCompleto(),
                p.isReevaluacionPendiente(), p.getNumeroReevaluaciones(), decisiones!=null && decisiones.existsByCodigoFamiliaAndDecision(p.getCodigoCaso(),"ACEPTADA"));
        var actividad=new java.util.ArrayList<java.util.Map<String,Object>>();
        if(p.getFechaRegistro()!=null)actividad.add(actividad("Entrevista registrada","Se recibió la información de tu hogar.",p.getFechaRegistro()));
        if(decisiones!=null)for(var d:decisiones.findByCodigoFamiliaOrderByFechaAsc(p.getCodigoCaso())) {
            if(d.getFecha()==null)continue;
            if("ACEPTADA".equals(d.getDecision()))actividad.add(actividad("Propuesta aceptada","El equipo SENNIAF te orientará sobre los siguientes pasos.",d.getFecha()));
            else if("REEVALUACION".equals(d.getDecision()))actividad.add(actividad("Revisión de entrevista solicitada","Se solicitó ampliar la información de la entrevista.",d.getFecha()));
        }
        if(auditoria!=null)for(var e:auditoria.findByCodigoCasoOrderByFechaAsc(p.getCodigoCaso())) {
            if(e.getFecha()!=null && "CORRECCION_ENTREVISTA".equals(e.getAccion()))actividad.add(actividad(tituloCorreccion(e.getDescripcion()),"Se recibió una actualización de tu entrevista.",e.getFecha()));
            if(e.getFecha()!=null && "CONTENIDO_ACADEMICO_ACTUALIZADO".equals(e.getAccion()))actividad.add(actividad("Información del hogar actualizada","Se completó la descripción del hogar conservando las respuestas de la entrevista.",e.getFecha()));
        }
        actividad.sort(java.util.Comparator.comparing(a->a.get("fecha").toString()));
        respuesta.setActividad(actividad);
        if (!actividad.isEmpty()) respuesta.setFechaActualizacionComunicada((java.time.OffsetDateTime)actividad.get(actividad.size()-1).get("fecha"));
        respuesta.setProximosPasos(respuesta.isVinculacionAceptada()
                ? List.of("Espera el contacto del equipo responsable para recibir orientación sobre la vinculación.", "Acuerda visitas o encuentros únicamente cuando el equipo los autorice.", "Mantén actualizada la información de tu hogar y comunica cambios relevantes.")
                : p.isReevaluacionPendiente() || !p.isPerfilCompleto()
                ? List.of("Revisa y completa tu entrevista para que el equipo pueda continuar el análisis.")
                : List.of("Espera la revisión del Comité. Las actualizaciones autorizadas se comunicarán en esta pantalla."));
        return respuesta;
    }

    private String tituloCorreccion(String descripcion) {
        try {
            var datos=new com.fasterxml.jackson.databind.ObjectMapper().readTree(descripcion);
            return "REEVALUACION".equals(datos.path("tipo").asText())?"Corrección por reevaluación recibida":"ACTUALIZACION".equals(datos.path("tipo").asText())?"Entrevista actualizada":"Entrevista corregida";
        }catch(com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException error){return "Entrevista corregida";}
    }
    private java.util.Map<String,Object> actividad(String titulo,String descripcion,LocalDateTime fecha) {
        return java.util.Map.of("titulo",titulo,"descripcion",descripcion,"fecha",fecha.atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
    }

    private java.util.Map<String,Object> datosAuditoria(PerfilFamilia p) {
        var m=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        java.util.Map<String,Object> datos=m.convertValue(p,new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){});
        datos.keySet().removeAll(java.util.Set.of("id","registradoPor","fechaRegistro","codigoCaso"));return datos;
    }
    private void auditarCorreccion(PerfilFamilia p,String actor,java.util.Map<String,Object> antes) {
        if(auditoria==null)return;
        var cambios=new java.util.LinkedHashMap<String,Object>();
        datosAuditoria(p).forEach((k,v)->{if(!java.util.Objects.equals(antes.get(k),v)){var c=new java.util.LinkedHashMap<String,Object>();c.put("anterior",antes.get(k));c.put("nuevo",v);cambios.put(k,c);}});
        var e=new com.senniaf.match.model.EventoCaso();e.setCodigoCaso(p.getCodigoCaso());e.setActor(actor);e.setFecha(LocalDateTime.now());e.setAccion("CORRECCION_ENTREVISTA");
        try{e.setDescripcion(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("rol","FAMILIA","resumen","Se actualizó la entrevista familiar.","tipo",Boolean.TRUE.equals(antes.get("reevaluacionPendiente"))?"REEVALUACION":"ACTUALIZACION","cambios",cambios)));}
        catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new IllegalStateException("No se pudo conservar la corrección",error);}
        auditoria.save(e);
    }

    private void copiarDatos(PerfilFamilia perfil, PerfilFamiliaRequest request) {
        perfil.setTipoFamilia(request.getTipoFamilia());
        perfil.setEspecificacionHogar(request.getTipoFamilia() == com.senniaf.match.model.enums.TipoFamilia.OTRA ? request.getEspecificacionHogar() : null);
        perfil.setNotaEstructuraHogar(request.getNotaEstructuraHogar());
        perfil.setRedApoyo(request.getRedApoyo());
        perfil.setNotaRedApoyo(request.getNotaRedApoyo());
        perfil.setProvincia(request.getProvincia());
        perfil.setDisponibilidadViaje(request.getDisponibilidadViaje());
        perfil.setNotaDistancia(request.getNotaDistancia());
        perfil.setEdadMinimaAceptada(request.getEdadMinimaAceptada());
        perfil.setEdadMaximaAceptada(request.getEdadMaximaAceptada());
        perfil.setNotaRangoEdad(request.getNotaRangoEdad());
        perfil.setDisposicionHermanos(request.getDisposicionHermanos());
        perfil.setNotaHermanos(request.getNotaHermanos());
        perfil.setCondicionesPuedeAcompanarHoy(request.getCondicionesPuedeAcompanarHoy());
        perfil.setCondicionesConApoyo(request.getCondicionesConApoyo());
        perfil.setNotaCondicionesSalud(request.getNotaCondicionesSalud());
        // Sprint 8: la sección está retirada; una actualización no borra ni reemplaza información histórica.
        if (perfil.getId() == null && request.getContinuidadCultural() != null)
            perfil.setContinuidadCultural(request.getContinuidadCultural());
    }

    /** Perfil que la familia ya registró con su cédula, si existe. */
    public java.util.Optional<PerfilFamiliaResponse> consultarPorCedula(String cedula) {
        return repositorio.findByRegistradoPor(cedula == null ? "" : cedula.trim())
                .map(p -> respuesta(p, "Perfil encontrado"));
    }

    private synchronized String siguienteCodigoCaso() {
        long numero = repositorio.count() + 1;
        while (repositorio.existsByCodigoCaso(String.format("FAM-%03d", numero))) numero++;
        return String.format("FAM-%03d", numero);
    }

    private boolean calcularSiEstaCompleto(PerfilFamiliaRequest r) {
        boolean rangoEdadValido = r.getEdadMinimaAceptada() != null
                && r.getEdadMaximaAceptada() != null
                && r.getEdadMinimaAceptada() <= r.getEdadMaximaAceptada();

        return r.getTipoFamilia() != null
                && noVacia(r.getRedApoyo())
                && r.getProvincia() != null
                && r.getDisponibilidadViaje() != null
                && rangoEdadValido
                && r.getDisposicionHermanos() != null
                && r.isHogarCoherente() && r.isRedApoyoCoherente() && r.isDisposicionHermanosCoherente();
    }

    private boolean noVacia(List<?> lista) {
        return lista != null && !lista.isEmpty();
    }
}
