package com.senniaf.match.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.senniaf.match.model.*;
import com.senniaf.match.model.CitaVinculacion.*;
import com.senniaf.match.repository.*;
import com.senniaf.match.security.RolNoAutorizadoException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class CitaService {
    public record Solicitud(Long vinculacionId,String motivo,String mensaje,String fecha,String hora) {}
    public record Respuesta(String accion,Long version,String fecha,String hora,Integer duracion,String motivo,String mensaje) {}
    private static final Set<Estado> ABIERTAS=Set.of(Estado.PENDIENTE,Estado.CAMBIO_PROPUESTO);
    private static final Set<Estado> TERMINALES=Set.of(Estado.RECHAZADA,Estado.CANCELADA,Estado.REALIZADA);
    private static final Map<Motivo,String> PLANTILLAS=Map.of(
        Motivo.ORIENTACION,"Deseo solicitar una cita para recibir orientación sobre mi proceso de vinculación",
        Motivo.SEGUIMIENTO,"Me gustaría solicitar una cita para conversar sobre el seguimiento de mi proceso de vinculación",
        Motivo.ENCUENTRO,"Deseo solicitar una cita para coordinar un encuentro relacionado con mi proceso de vinculación",
        Motivo.OBSERVACION,"Me gustaría aclarar una observación relacionada con mi proceso de vinculación",
        Motivo.DUDAS,"Deseo solicitar una cita para resolver algunas dudas relacionadas con mi proceso de vinculación",
        Motivo.OTRO,"");
    private final CitaVinculacionRepository citas;
    private final CitaHistorialRepository historial;
    private final VinculacionRepository vinculaciones;
    private final InterrupcionConvivenciaRepository interrupciones;
    private final UsuarioRepository usuarios;
    private final EventoCasoRepository auditoria;
    private final HorarioCitas horario;
    private final Clock reloj;
    private final ObjectMapper json;
    private final jakarta.persistence.EntityManager entidades;
    public CitaService(CitaVinculacionRepository citas,CitaHistorialRepository historial,
                       VinculacionRepository vinculaciones,InterrupcionConvivenciaRepository interrupciones,
                       UsuarioRepository usuarios,EventoCasoRepository auditoria,HorarioCitas horario,Clock reloj,ObjectMapper json,
                       jakarta.persistence.EntityManager entidades) {
        this.citas=citas;this.historial=historial;this.vinculaciones=vinculaciones;this.interrupciones=interrupciones;
        this.usuarios=usuarios;this.auditoria=auditoria;this.horario=horario;this.reloj=reloj;this.json=json;
        this.entidades=entidades;
    }
    public List<Map<String,Object>> listar(String rol,String actor) {
        rol(rol);
        var datos="FAMILIA".equals(rol)?citas.findByVinculacionFamiliaRegistradoPorOrderByActualizadaDescIdDesc(actor)
                :citas.findByVinculacionNinoTrabajadorSocialAsignadoOrderByActualizadaDescIdDesc(actor);
        return datos.stream().sorted(Comparator.comparing((CitaVinculacion c)->!ABIERTAS.contains(c.getEstado()))
                .thenComparing(CitaVinculacion::getActualizada,Comparator.reverseOrder()).thenComparing(CitaVinculacion::getId,Comparator.reverseOrder()))
                .map(c->vista(c,rol,false)).toList();
    }
    public Map<String,Object> detalle(Long id,String rol,String actor) {
        rol(rol);var c=obtener(id);autorizar(c.getVinculacion(),rol,actor);return vista(c,rol,true);
    }
    public Map<String,Object> opciones(String rol,String actor) {
        rol(rol);var datos="FAMILIA".equals(rol)?vinculaciones.findByFamiliaRegistradoPorOrderByIdDesc(actor):citas.vinculacionesDelProfesional(actor);
        var permitidas=datos.stream().map(v->{Map<String,Object> r=new LinkedHashMap<>();r.put("id",v.getId());r.put("codigo","VIN-"+v.getId());
            r.put("habilitada",habilitada(v) && profesionalValido(v.getNino().getTrabajadorSocialAsignado()));return r;}).toList();
        return Map.of("vinculaciones",permitidas,"configurada",horario.configurado(),"hoy",LocalDate.now(reloj.withZone(HorarioCitas.ZONA)),
                "zona","America/Panama","duracionDefecto",120,"paso",horario.paso());
    }
    public List<String> disponibilidad(Long vin,String fecha,int duracion,String rol,String actor) {
        rol(rol);var v=vinculaciones.findById(vin).orElseThrow(this::sinAcceso);autorizar(v,rol,actor);exigirHabilitada(v);
        if("FAMILIA".equals(rol) && duracion!=120 && duracion!=60)throw new IllegalArgumentException("La duración no es válida");
        String profesional=v.getNino().getTrabajadorSocialAsignado();
        if(!profesionalValido(profesional))return List.of();
        var dia=horario.fecha(fecha);
        return horario.horas(dia,duracion,reloj).stream().filter(h->!citas.hayConflicto(profesional,dia.atTime(LocalTime.parse(h)),dia.atTime(LocalTime.parse(h)).plusMinutes(duracion),null)).toList();
    }
    @Transactional
    public Map<String,Object> solicitar(Solicitud r,String rol,String actor) {
        exigirRol(rol,"FAMILIA");
        if(r==null || r.vinculacionId()==null || r.vinculacionId()<1)throw new IllegalArgumentException("Selecciona tu vinculación");
        var v=bloquearVinculacion(r.vinculacionId(),rol,actor);exigirHabilitada(v);
        if(citas.existeAbierta(v.getId()))throw new IllegalStateException("Ya tienes una solicitud pendiente para esta vinculación. Consúltala en Mis solicitudes");
        Motivo motivo;
        try{motivo=Motivo.valueOf(r.motivo()==null?"":r.motivo());}catch(IllegalArgumentException e){throw new IllegalArgumentException("Selecciona un motivo válido");}
        var c=new CitaVinculacion();c.setVinculacion(v);c.setProfesional(v.getNino().getTrabajadorSocialAsignado());
        c.setCreadoPor(actor);c.setMotivo(motivo);c.setEstado(Estado.PENDIENTE);c.setCreada(reloj.instant());
        c.setActualizada(c.getCreada());c.setUltimoRol(rol);
        c.setMensajeInicial(mensaje(r.mensaje(),PLANTILLAS.get(motivo)));c.setMensaje(c.getMensajeInicial());
        intervalo(c,r.fecha(),r.hora(),120);citas.saveAndFlush(c);registrar(c,actor,rol,"SOLICITAR",null,null);
        return vista(c,rol,true);
    }
    @Transactional
    public Map<String,Object> responder(Long id,Respuesta r,String rol,String actor) {
        rol(rol);var inicial=obtener(id);Long vinculo=inicial.getVinculacion().getId();
        // El estado leído antes de esperar por la agenda no se reutiliza para decidir la transición.
        entidades.detach(inicial);
        var v=bloquearVinculacion(vinculo,rol,actor);
        citas.bloquearSolicitud(id).orElseThrow(this::sinAcceso);
        var c=obtener(id);entidades.refresh(c);autorizar(c.getVinculacion(),rol,actor);
        if(r==null || r.version()==null || r.version()!=c.getVersion())throw new IllegalStateException("La solicitud cambió. Actualiza la información antes de responder");
        String accion=r.accion()==null?"":r.accion();Estado anterior=c.getEstado();
        if(TERMINALES.contains(anterior))throw new IllegalStateException("Esta solicitud ya está cerrada y conserva su historial");
        String sugerencia;
        switch(accion) {
            case "ACEPTAR" -> {
                exigirRol(rol,"TRABAJADOR_SOCIAL");exigirEstado(c,Estado.PENDIENTE);exigirHabilitada(v);
                c.setProfesional(v.getNino().getTrabajadorSocialAsignado());
                intervalo(c,c.getInicio().toLocalDate().toString(),c.getInicio().toLocalTime().toString(),duracion(r));
                c.setEstado(Estado.CONFIRMADA);sugerencia="Su solicitud de cita ha sido confirmada para el "+cuando(c)+" Le esperamos";
            }
            case "RECHAZAR" -> {
                exigirRol(rol,"TRABAJADOR_SOCIAL");exigirEstado(c,Estado.PENDIENTE);c.setEstado(Estado.RECHAZADA);
                sugerencia=motivoRespuesta(r.motivo(),false);c.setProfesional(v.getNino().getTrabajadorSocialAsignado());
            }
            case "PROPONER" -> {
                exigirRol(rol,"TRABAJADOR_SOCIAL");exigirEstado(c,Estado.PENDIENTE);exigirHabilitada(v);
                sugerencia=motivoRespuesta(r.motivo(),true);c.setProfesional(v.getNino().getTrabajadorSocialAsignado());
                intervalo(c,r.fecha(),r.hora(),duracion(r));c.setEstado(Estado.CAMBIO_PROPUESTO);
                sugerencia=sugerencia.replace("{momento}",cuando(c));
            }
            case "ACEPTAR_PROPUESTA" -> {
                exigirRol(rol,"FAMILIA");exigirEstado(c,Estado.CAMBIO_PROPUESTO);exigirHabilitada(v);
                if(!Objects.equals(c.getProfesional(),v.getNino().getTrabajadorSocialAsignado()))throw new IllegalStateException("El profesional asignado ha cambiado. Solicita otro horario para que revise la propuesta");
                if(r.duracion()!=null)throw new IllegalArgumentException("La duración corresponde a la propuesta del profesional");
                intervalo(c,c.getInicio().toLocalDate().toString(),c.getInicio().toLocalTime().toString(),c.getDuracion());
                c.setEstado(Estado.CONFIRMADA);sugerencia="Acepto la nueva fecha y hora propuestas para la cita";
            }
            case "OTRO_HORARIO" -> {
                exigirRol(rol,"FAMILIA");exigirEstado(c,Estado.CAMBIO_PROPUESTO);exigirHabilitada(v);
                if(r.duracion()!=null)throw new IllegalArgumentException("La duración corresponde a la propuesta del profesional");
                c.setProfesional(v.getNino().getTrabajadorSocialAsignado());intervalo(c,r.fecha(),r.hora(),c.getDuracion());
                c.setEstado(Estado.PENDIENTE);sugerencia="Gracias por la propuesta. Me gustaría consultar la posibilidad de realizar la cita en otra fecha y hora";
            }
            case "CANCELAR" -> {
                if("TRABAJADOR_SOCIAL".equals(rol) && anterior==Estado.PENDIENTE)throw new IllegalStateException("Utiliza Rechazar para responder una solicitud pendiente");
                if(anterior==Estado.CONFIRMADA && !c.getInicio().isAfter(ahora()))throw new IllegalStateException("Una cita que ya comenzó no admite cancelación");
                c.setEstado(Estado.CANCELADA);sugerencia="La solicitud de cita ha sido cancelada";
            }
            case "REALIZADA" -> {
                exigirRol(rol,"TRABAJADOR_SOCIAL");exigirEstado(c,Estado.CONFIRMADA);
                if(c.getFin().isAfter(ahora()))throw new IllegalStateException("Solo puedes registrar la cita como realizada después de su finalización");
                c.setEstado(Estado.REALIZADA);sugerencia="La cita se realizó";
            }
            default -> throw new IllegalArgumentException("Selecciona una acción válida");
        }
        c.setMensaje(mensaje(r.mensaje(),sugerencia));
        if("TRABAJADOR_SOCIAL".equals(rol))c.setRespuesta(c.getMensaje());
        registrar(c,actor,rol,accion,anterior.name(),Set.of("RECHAZAR","PROPONER").contains(accion)?r.motivo():null);citas.saveAndFlush(c);
        return vista(c,rol,true);
    }
    private int duracion(Respuesta r){return r.duracion()==null?120:r.duracion();}
    private void intervalo(CitaVinculacion c,String fecha,String hora,int duracion) {
        var inicio=horario.inicio(fecha,hora,duracion,reloj);var fin=inicio.plusMinutes(duracion);
        if(citas.hayConflicto(c.getProfesional(),inicio,fin,c.getId()))throw new IllegalStateException("La fecha seleccionada ya no está disponible. Hay otra cita confirmada en ese intervalo; selecciona otro horario");
        c.setInicio(inicio);c.setFin(fin);c.setDuracion(duracion);
    }
    /** Orden de bloqueo: agenda → vinculación → expediente → solicitud; se refrescan las entidades ya leídas. */
    private Vinculacion bloquearVinculacion(Long id,String rol,String actor) {
        var vista=vinculaciones.findById(id).orElseThrow(this::sinAcceso);autorizar(vista,rol,actor);
        String profesional=vista.getNino().getTrabajadorSocialAsignado();
        if(profesional==null || citas.bloquearAgenda(profesional).isEmpty())throw new IllegalStateException("La vinculación todavía no tiene un trabajador social habilitado para las citas");
        citas.bloquearVinculo(id).orElseThrow(this::sinAcceso);
        var v=vista;entidades.refresh(v);
        citas.bloquearExpediente(v.getNino().getId()).orElseThrow(this::sinAcceso);
        entidades.refresh(v.getNino());
        autorizar(v,rol,actor);
        // Una reasignación concurrente no puede confirmar sobre una agenda distinta de la bloqueada.
        if(!Objects.equals(profesional,v.getNino().getTrabajadorSocialAsignado()))throw new IllegalStateException("Cambió el profesional asignado. Actualiza la información");
        return v;
    }
    private CitaVinculacion obtener(Long id){return citas.findById(id).orElseThrow(this::sinAcceso);}
    private RolNoAutorizadoException sinAcceso(){return new RolNoAutorizadoException("La solicitud de cita no está disponible para este acceso");}
    private void autorizar(Vinculacion v,String rol,String actor) {
        rol(rol);if(actor==null || !("FAMILIA".equals(rol)?actor.equals(v.getFamilia().getRegistradoPor()):actor.equals(v.getNino().getTrabajadorSocialAsignado())))throw sinAcceso();
    }
    private void rol(String rol){if(!Set.of("FAMILIA","TRABAJADOR_SOCIAL").contains(rol==null?"":rol))throw sinAcceso();}
    private void exigirRol(String actual,String esperado){if(!esperado.equals(actual))throw sinAcceso();}
    private void exigirEstado(CitaVinculacion c,Estado estado){if(c.getEstado()!=estado)throw new IllegalStateException("La solicitud ya no permite esta acción. Actualiza la información");}
    private boolean habilitada(Vinculacion v) {
        return v.getEstado()==Vinculacion.Estado.RESPUESTA_ACEPTADA && v.isReservaVigente()
                && !v.getNino().isCierrePorAdopcion() && !v.getNino().isBajaLogica()
                && interrupciones.findByVinculacionId(v.getId()).isEmpty();
    }
    private void exigirHabilitada(Vinculacion v){if(!habilitada(v))throw new IllegalStateException("Esta vinculación no está habilitada para solicitar o confirmar citas");}
    private boolean profesionalValido(String id){return id!=null && usuarios.findByIdentificador(id).filter(u->u.isVerificado() && "TRABAJADOR_SOCIAL".equals(u.getRol())).isPresent();}
    private LocalDateTime ahora(){return LocalDateTime.now(reloj.withZone(HorarioCitas.ZONA));}
    private String mensaje(String valor,String sugerido) {
        String texto=valor==null?sugerido:valor.strip();
        if(texto==null || texto.isBlank() || texto.length()<10 || texto.length()>1000
                || texto.matches("(?s).*[\\p{Cc}&&[^\\n\\r\\t]].*") || texto.matches("(?s).*\\p{Cf}.*")
                || texto.toLowerCase(Locale.ROOT).matches("(?s).*\\[(?:fecha|hora)\\].*"))
            throw new IllegalArgumentException("Escribe un mensaje de 10 a 1000 caracteres, sin marcadores pendientes ni caracteres ocultos");
        return texto;
    }
    private String cuando(CitaVinculacion c) {
        String hora=c.getInicio().toLocalTime().format(DateTimeFormatter.ofPattern("h:mm",Locale.ROOT))+(c.getInicio().getHour()<12?" a. m.":" p. m.");
        return c.getInicio().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"))+" a las "+hora;
    }
    private String motivoRespuesta(String motivo,boolean propuesta) {
        if(motivo==null)throw new IllegalArgumentException("Selecciona un motivo para la respuesta");
        if(motivo.equals("OTRO"))return "";
        if(propuesta)return switch(motivo){
            case "SIN_DISPONIBILIDAD"->"No contamos con disponibilidad en la fecha solicitada. Le proponemos realizar la cita el {momento}";
            case "AJUSTE_AGENDA"->"Por motivos de agenda, le proponemos una nueva fecha para su cita: {momento}";
            case "NUEVO_HORARIO"->"Hemos revisado su solicitud y le proponemos realizar la cita el {momento} Por favor, confirme si tiene disponibilidad";
            default->throw new IllegalArgumentException("Selecciona un motivo de cambio válido");};
        return switch(motivo){
            case "SIN_DISPONIBILIDAD"->"Por el momento no contamos con disponibilidad para la fecha y hora solicitadas. Puede presentar otra propuesta";
            case "COORDINACION_PREVIA"->"Antes de confirmar esta cita, es necesario realizar una coordinación previa. Le mantendremos informado por los medios disponibles en el sistema";
            case "NO_ATENDER"->"Por el momento no es posible atender esta solicitud. Le recomendamos comunicarse nuevamente mediante una nueva solicitud cuando corresponda";
            default->throw new IllegalArgumentException("Selecciona un motivo de rechazo válido");};
    }
    private void registrar(CitaVinculacion c,String actor,String rol,String accion,String anterior,String motivo) {
        if(anterior!=null)c.setActualizada(reloj.instant());
        c.setUltimoRol(rol);historial.save(new CitaHistorial(c,actor,rol,accion,anterior,motivo));
        EventoCaso evento=new EventoCaso();evento.setCodigoCaso(c.getVinculacion().getNino().getCodigoCaso());evento.setActor(actor);
        evento.setAccion("CITA_"+accion);evento.setFecha(LocalDateTime.ofInstant(reloj.instant(),ZoneId.systemDefault()));
        try{evento.setDescripcion(json.writeValueAsString(Map.of("vinculacionId",c.getVinculacion().getId(),"citaId",c.getId(),"resumen","Cita: "+c.getEstado().name().toLowerCase(Locale.ROOT).replace('_',' ')+".")));}
        catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("No se pudo registrar la trazabilidad de la cita");}
        auditoria.save(evento);
    }
    private Map<String,Object> vista(CitaVinculacion c,String rol,boolean incluirHistorial) {
        Map<String,Object> r=new LinkedHashMap<>();r.put("id",c.getId());r.put("version",c.getVersion());r.put("vinculacionId",c.getVinculacion().getId());
        r.put("vinculacion","VIN-"+c.getVinculacion().getId());r.put("motivo",c.getMotivo());r.put("estado",c.getEstado());
        r.put("fecha",c.getInicio().toLocalDate());r.put("hora",c.getInicio().toLocalTime());r.put("inicio",c.getInicio().atZone(HorarioCitas.ZONA).toOffsetDateTime());
        r.put("fin",c.getFin().atZone(HorarioCitas.ZONA).toOffsetDateTime());r.put("duracion",c.getDuracion());
        r.put("mensajeInicial",c.getMensajeInicial());r.put("mensaje",c.getMensaje());r.put("respuesta",c.getRespuesta());r.put("ultimoRol",c.getUltimoRol());
        r.put("creada",c.getCreada());r.put("actualizada",c.getActualizada());boolean activa=habilitada(c.getVinculacion()) && horario.configurado();
        List<String> acciones=new ArrayList<>();
        if(!TERMINALES.contains(c.getEstado())) {
            if("TRABAJADOR_SOCIAL".equals(rol) && c.getEstado()==Estado.PENDIENTE){if(activa){acciones.add("ACEPTAR");acciones.add("PROPONER");}acciones.add("RECHAZAR");}
            if("FAMILIA".equals(rol) && c.getEstado()==Estado.CAMBIO_PROPUESTO && activa){acciones.add("ACEPTAR_PROPUESTA");acciones.add("OTRO_HORARIO");}
            if(("FAMILIA".equals(rol) || c.getEstado()!=Estado.PENDIENTE) && (c.getEstado()!=Estado.CONFIRMADA || c.getInicio().isAfter(ahora())))acciones.add("CANCELAR");
            if("TRABAJADOR_SOCIAL".equals(rol) && c.getEstado()==Estado.CONFIRMADA && !c.getFin().isAfter(ahora()))acciones.add("REALIZADA");
        }
        r.put("acciones",acciones);
        if("TRABAJADOR_SOCIAL".equals(rol))r.put("familia",c.getVinculacion().getFamilia().getCodigoCaso());
        if(incluirHistorial)r.put("historial",historial.findByCitaIdOrderByIdAsc(c.getId()).stream().map(e->{
            Map<String,Object> h=new LinkedHashMap<>();h.put("accion",e.getAccion());h.put("anterior",e.getAnterior());h.put("estado",e.getNuevo());
            h.put("actor","FAMILIA".equals(e.getRol())?"Familia":"Trabajo Social");h.put("fechaAccion",e.getFecha());h.put("mensaje",e.getMensaje());
            h.put("inicio",e.getInicio().atZone(HorarioCitas.ZONA).toOffsetDateTime());h.put("fin",e.getFin().atZone(HorarioCitas.ZONA).toOffsetDateTime());h.put("duracion",e.getDuracion());return h;}).toList());
        return r;
    }
}
