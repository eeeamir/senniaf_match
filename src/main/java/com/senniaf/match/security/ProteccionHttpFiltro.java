package com.senniaf.match.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Pattern;

/** Cabeceras, origen de escrituras y presupuesto HTTP; SesionFiltro conserva la autenticación. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProteccionHttpFiltro extends OncePerRequestFilter {
    private final LimiteIntentos limites;
    private final String politica;
    @Value("${senniaf.seguridad.origen-publico:}") private String origenPublico="";
    @Value("${senniaf.seguridad.limite-auth:120}") private int limiteAuth=120;
    public ProteccionHttpFiltro(LimiteIntentos limites) throws Exception {
        this.limites=limites;
        StringBuilder scripts=new StringBuilder("script-src 'self'");
        var patron=Pattern.compile("<script\\b[^>]*>([\\s\\S]*?)</script>",Pattern.CASE_INSENSITIVE);
        for(var recurso:new PathMatchingResourcePatternResolver().getResources("classpath*:static/*.html")) {
            String html=recurso.getContentAsString(StandardCharsets.UTF_8).replace("\r\n","\n").replace('\r','\n');
            var m=patron.matcher(html);
            while(m.find()) if(!m.group(1).isBlank()) scripts.append(" 'sha256-").append(Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(m.group(1).getBytes(StandardCharsets.UTF_8)))).append("'");
        }
        politica="default-src 'self'; "+scripts+"; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'";
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        res.setHeader("Content-Security-Policy",politica);
        res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","DENY");
        res.setHeader("Referrer-Policy","no-referrer");res.setHeader("Permissions-Policy","camera=(), microphone=(), geolocation=()");
        boolean logo = "/img/logo.png".equals(req.getServletPath());
        res.setHeader("Cache-Control", logo ? "public, max-age=86400" : "no-store");
        if(req.isSecure()) res.setHeader("Strict-Transport-Security","max-age=31536000");
        String ruta=req.getPathInfo()!=null?req.getPathInfo():req.getServletPath();
        boolean api=ruta.equals("/api") || ruta.startsWith("/api/");
        boolean escritura=!java.util.Set.of("GET","HEAD","OPTIONS").contains(req.getMethod());
        if(api && escritura) {
            String origen=req.getHeader("Origin");
            String esperado=origenPublico.isBlank()?req.getScheme()+"://"+req.getServerName()+
                    ((req.isSecure()&&req.getServerPort()==443 || !req.isSecure()&&req.getServerPort()==80)?"":":"+req.getServerPort()):origenPublico;
            if("cross-site".equals(req.getHeader("Sec-Fetch-Site")) || origen!=null&&!esperado.equals(origen)) {
                rechazar(res,403,"Origen de solicitud no autorizado");return;
            }
            if(req.getContentLengthLong()>131072) {rechazar(res,413,"La solicitud excede el tamaño permitido");return;}
            if(ruta.startsWith("/api/auth/") && !ruta.equals("/api/auth/logout") && !limites.admitir("ip:"+req.getRemoteAddr(),limiteAuth,60000)) {
                res.setHeader("Retry-After","60");rechazar(res,429,"Demasiadas solicitudes. Inténtalo más tarde");return;
            }
        }
        chain.doFilter(req,res);
    }
    private void rechazar(HttpServletResponse res,int estado,String mensaje) throws IOException {
        res.setStatus(estado);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"mensaje\":\""+mensaje+"\"}");
    }
}
