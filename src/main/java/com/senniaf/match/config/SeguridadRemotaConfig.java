package com.senniaf.match.config;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.net.URI;

/** Prevalidación antes de JPA; después comprueba los privilegios de la conexión. */
@Configuration
@Profile("remoto")
public class SeguridadRemotaConfig {
    @Bean
    static BeanFactoryPostProcessor validarConfiguracionRemota(Environment env) {
        return fabrica -> {
            exigir(!env.getProperty("senniaf.auth.codigo-demo",Boolean.class,false),"No se permite devolver códigos de verificación en remoto");
            exigir(!env.getProperty("senniaf.demo.habilitado",Boolean.class,false),"Las cuentas institucionales remotas se preparan de forma controlada");
            exigir("validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")),"El esquema remoto debe usar validate");
            exigir("127.0.0.1".equals(env.getProperty("server.address")),"La aplicación remota debe escuchar solo en loopback");
            if(env.getProperty("senniaf.auth.registro-publico",Boolean.class,false)) {
                exigir(env.getProperty("senniaf.correo.habilitado",Boolean.class,false)
                        && !env.getProperty("senniaf.correo.remitente","").isBlank()
                        && !env.getProperty("spring.mail.host","").isBlank(),"El registro público requiere un canal SMTP configurado");
                exigir(!env.getProperty("spring.mail.username","").isBlank() && !env.getProperty("spring.mail.password","").isBlank(),"Faltan credenciales SMTP");
                exigir(env.getProperty("spring.mail.properties.mail.smtp.auth",Boolean.class,false)
                        && env.getProperty("spring.mail.properties.mail.smtp.starttls.enable",Boolean.class,false)
                        && env.getProperty("spring.mail.properties.mail.smtp.starttls.required",Boolean.class,false)
                        && env.getProperty("spring.mail.properties.mail.smtp.ssl.checkserveridentity",Boolean.class,false),"SMTP remoto exige autenticación, STARTTLS y validación de certificado");
            }
            exigir(!env.getProperty("spring.mail.properties.mail.debug",Boolean.class,false)
                    && !env.getProperty("spring.mail.properties.mail.debug.auth",Boolean.class,false),"No se permite depuración SMTP que pueda exponer credenciales o códigos");
            exigir(env.getProperty("spring.datasource.password","").length()>=24,"La credencial de PostgreSQL debe ser explícita y robusta");
            exigir(!"postgres".equals(env.getProperty("spring.datasource.username")),"No debe ejecutarse con el superusuario PostgreSQL");
            exigir(env.getProperty("spring.datasource.url","").matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/[a-zA-Z0-9_]+"),"PostgreSQL debe permanecer en loopback y sin credenciales en la URL");
            URI origen=URI.create(env.getProperty("senniaf.seguridad.origen-publico",""));
            exigir("https".equals(origen.getScheme()) && origen.getHost()!=null && origen.getUserInfo()==null
                    && (origen.getPath()==null||origen.getPath().isEmpty()) && origen.getQuery()==null && origen.getFragment()==null
                    && !origen.getHost().endsWith(".invalid"),"Configura el origen HTTPS definitivo sin ruta");
        };
    }
    @Bean
    InitializingBean validarPrivilegiosRemotos(DataSource datos,EntityManagerFactory jpa) {
        return () -> {
            // La dependencia jpa exige que Hibernate haya validado el esquema antes de servir HTTP.
            try(var c=datos.getConnection();var q=c.createStatement();var r=q.executeQuery(
                    "select rolsuper or rolcreatedb or rolcreaterole or has_schema_privilege(current_user,'public','CREATE') from pg_roles where rolname=current_user")) {
                exigir(r.next()&&!r.getBoolean(1),"La cuenta de aplicación no debe poder administrar usuarios, bases ni esquema");
            }
        };
    }
    private static void exigir(boolean condicion,String mensaje) {if(!condicion) throw new IllegalStateException(mensaje);}
}
