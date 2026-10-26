package com.senniaf.match.security;

import com.senniaf.match.model.Usuario;
import com.senniaf.match.service.SesionAccesoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/** La identidad proviene de PostgreSQL, nunca de cabeceras declaradas por el cliente. */
@Component
public class SesionFiltro extends OncePerRequestFilter {
    private final SesionAccesoService sesiones;
    public SesionFiltro(SesionAccesoService sesiones) { this.sesiones = sesiones; }

    @Override protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/auth/");
    }

    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                               FilterChain chain) throws ServletException, IOException {
        Usuario usuario = sesiones.autenticar(req.getHeader("Authorization")).orElse(null);
        if (usuario == null) {
            res.setStatus(401);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"mensaje\":\"Sesión ausente, inválida o vencida\"}");
            return;
        }
        HttpServletRequest segura = new HttpServletRequestWrapper(req) {
            @Override public String getHeader(String nombre) {
                if ("X-User-Role".equalsIgnoreCase(nombre)) return usuario.getRol();
                if ("X-User-Id".equalsIgnoreCase(nombre)) return usuario.getIdentificador();
                return super.getHeader(nombre);
            }
            @Override public Enumeration<String> getHeaders(String nombre) {
                if ("X-User-Role".equalsIgnoreCase(nombre) || "X-User-Id".equalsIgnoreCase(nombre))
                    return Collections.enumeration(Collections.singleton(getHeader(nombre)));
                return super.getHeaders(nombre);
            }
            @Override public Enumeration<String> getHeaderNames() {
                Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
                names.add("X-User-Role"); names.add("X-User-Id");
                return Collections.enumeration(names);
            }
        };
        chain.doFilter(segura, res);
    }
}
