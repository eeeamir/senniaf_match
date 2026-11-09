package com.senniaf.match.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.senniaf.match.security.LimiteIntentos;

/** Únicamente entrega el código ya generado; no crea usuarios ni verifica identidades. */
@Service
public class CorreoVerificacionService {
    private final ObjectProvider<JavaMailSender> emisor;
    private final LimiteIntentos limites;
    private final Environment entorno;
    @Value("${senniaf.correo.habilitado:false}") private boolean habilitado;
    @Value("${senniaf.correo.remitente:}") private String remitente="";
    @Value("${senniaf.correo.limite-diario:200}") private int limiteDiario=200;
    public CorreoVerificacionService(ObjectProvider<JavaMailSender> emisor,LimiteIntentos limites,Environment entorno) {
        this.emisor=emisor;this.limites=limites;this.entorno=entorno;
    }
    public boolean habilitado() {return habilitado;}
    public boolean disponible() {
        return habilitado && !remitente.isBlank() && !entorno.getProperty("spring.mail.host","").isBlank() && emisor.getIfAvailable()!=null;
    }
    public void enviar(String destino,String codigo) {
        if(!disponible()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"El correo de verificación no está disponible. Inténtalo más tarde");
        if(!limites.admitir("smtp:global",limiteDiario,86400000))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Se alcanzó el límite temporal de correos. Inténtalo más tarde");
        try {
            if(!codigo.matches("[0-9]{6}")) throw new IllegalArgumentException("Código de verificación inválido");
            var transporte=emisor.getObject();var mime=transporte.createMimeMessage();
            var mensaje=new MimeMessageHelper(mime,true,"UTF-8");mensaje.setFrom(remitente);mensaje.setTo(destino);
            mensaje.setSubject("SENNIAF Match - código de verificación");
            String texto="Tu código de verificación de SENNIAF Match es: "+codigo+
                    "\n\nVence en 10 minutos y solo puede utilizarse una vez. No lo compartas.\nSi no solicitaste este registro, ignora este mensaje.";
            String html="""
                    <!doctype html><html lang="es"><head><meta charset="UTF-8"><title>Verificación de correo</title></head>
                    <body style="margin:0;padding:24px;background:#f5f8fc;font-family:Arial,sans-serif;color:#123047">
                    <table role="presentation" style="width:100%%;max-width:560px;margin:auto;background:#fff;border:1px solid #dce5ef;border-radius:12px"><tr><td style="padding:28px">
                    <p style="color:#006bff;font-weight:bold;font-size:20px">SENNIAF Match</p>
                    <h1 style="font-size:24px">Verifica tu correo</h1><p>Introduce este código en la pantalla de verificación:</p>
                    <p style="font-size:32px;font-weight:bold;letter-spacing:6px;color:#006bff">%s</p>
                    <p>Vence en <strong>10 minutos</strong> y solo puede utilizarse una vez. No lo compartas.</p>
                    <p style="font-size:13px;color:#52677a">Si no solicitaste este registro, ignora este mensaje.</p>
                    </td></tr></table></body></html>
                    """.formatted(codigo);
            mensaje.setText(texto,html);transporte.send(mime);
        } catch(MailException | MessagingException ex) {
            String referencia=java.util.UUID.randomUUID().toString();
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Correo no aceptado referencia={} tipo={}",referencia,ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No pudimos enviar el código. Inténtalo de nuevo más tarde. Referencia: "+referencia);
        }
    }
}
