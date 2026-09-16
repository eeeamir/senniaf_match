package com.senniaf.match.security;

/**
 * Se lanza cuando un rol distinto a TRABAJADOR_SOCIAL o PSICOLOGO intenta
 * crear o editar el perfil de un niño o niña (regla 2.6 / HU01).
 */
public class RolNoAutorizadoException extends RuntimeException {

    public RolNoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
