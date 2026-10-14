package com.senniaf.match.support;

import com.senniaf.match.model.Usuario;
import com.senniaf.match.security.AccesoNoAutorizadoException;
import com.senniaf.match.security.RolNoAutorizadoException;
import com.senniaf.match.service.AuditoriaService;
import com.senniaf.match.service.AutenticacionService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Dobles de prueba: evitan modificar clases de servicios con agentes de bytecode. */
public final class ServiciosDePrueba {
    private ServiciosDePrueba() { }

    /** Autenticación controlada para pruebas HTTP, sin token ni sesión de producción. */
    public static AutenticacionService autenticacion(Map<String, Usuario> cuentas) {
        return new AutenticacionService(null) {
            @Override
            public Usuario requerir(String authorization, String... roles) {
                if (authorization == null) {
                    throw new AccesoNoAutorizadoException("Sesión requerida o inválida");
                }
                Usuario usuario = cuentas.get(authorization);
                if (usuario == null) {
                    throw new AccesoNoAutorizadoException("Sesión requerida o inválida");
                }
                if (Arrays.stream(roles).noneMatch(usuario.getRol()::equals)) {
                    throw new RolNoAutorizadoException("Prohibido");
                }
                return usuario;
            }
        };
    }

    /** Registra eventos en memoria; no necesita base de datos ni un mock de clase. */
    public static final class AuditoriaEnMemoria extends AuditoriaService {
        public record Evento(String actor, String accion, String codigo, String detalle) { }
        private final List<Evento> eventos = new ArrayList<>();

        public AuditoriaEnMemoria() {
            super(null);
        }

        @Override
        public void registrar(String actor, String accion, String codigo, String detalle) {
            eventos.add(new Evento(actor, accion, codigo, detalle));
        }

        public boolean contiene(String actor, String accion, String codigo) {
            return eventos.stream().anyMatch(evento ->
                    evento.actor().equals(actor) &&
                    evento.accion().equals(accion) &&
                    evento.codigo().equals(codigo));
        }
    }
}
