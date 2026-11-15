package com.senniaf.match.controller;

import com.senniaf.match.security.RolNoAutorizadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Se dispara, por ejemplo, cuando una lista desplegable (enum) recibe
     * un valor que no está entre las opciones permitidas: en vez de una
     * excepción interna 500, se devuelve un 400 con un mensaje claro.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "El cuerpo de la solicitud tiene un formato inválido o incluye un valor "
                + "que no está entre las opciones permitidas de una lista desplegable");
        if(ex.getCause() instanceof com.fasterxml.jackson.databind.JsonMappingException detalle && !detalle.getPath().isEmpty()) {
            String campo=detalle.getPath().stream().map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.joining("."));
            if(!campo.isEmpty()) {
                Class<?> tipo=detalle instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException dato?dato.getTargetType():null;
                String mensaje=tipo==Integer.class||tipo==int.class?"Escribe un número entero, sin letras ni decimales."
                        :tipo!=null&&tipo.isEnum()?"Selecciona una de las opciones disponibles."
                        :"Revisa el formato de este campo.";
                cuerpo.put("errores",Map.of(campo,mensaje));
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Revisa los campos señalados para continuar");

        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField().equals("documentoValido")?"numeroDocumento":error.getField(), error.getDefaultMessage()));
        cuerpo.put("errores", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,Object>> manejarConflictoPersistencia(org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                "El registro entra en conflicto con un expediente existente. Comprueba el número institucional; no se creó un duplicado ni se cambiaron responsables"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> manejarCasoDuplicado(IllegalStateException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo);
    }

    @ExceptionHandler(com.senniaf.match.web.CampoInvalidoException.class)
    public ResponseEntity<Map<String,Object>> manejarCampo(com.senniaf.match.web.CampoInvalidoException ex) {
        return ResponseEntity.status(ex.isConflicto()?HttpStatus.CONFLICT:HttpStatus.BAD_REQUEST)
                .body(Map.of("mensaje",ex.getMessage(),"errores",Map.of(ex.getCampo(),ex.getMessage())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarArgumentoInvalido(IllegalArgumentException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    @ExceptionHandler(RolNoAutorizadoException.class)
    public ResponseEntity<Map<String, Object>> manejarRolNoAutorizado(RolNoAutorizadoException ex) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(cuerpo);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> manejarServicioNoDisponible(org.springframework.web.server.ResponseStatusException ex) {
        var respuesta=ResponseEntity.status(ex.getStatusCode());
        if(ex.getStatusCode().value()==429) respuesta.header("Retry-After","900");
        return respuesta.body(Map.of("mensaje",ex.getReason()==null?"Solicitud rechazada":ex.getReason()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String,Object>> manejarFalloInterno(RuntimeException ex) {
        String referencia=java.util.UUID.randomUUID().toString();
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Fallo interno referencia={} tipo={}",referencia,ex.getClass().getSimpleName());
        return ResponseEntity.status(500).body(Map.of("mensaje","No se pudo completar la operación. Referencia: "+referencia));
    }
}
