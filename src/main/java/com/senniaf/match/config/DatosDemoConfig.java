package com.senniaf.match.config;

import com.senniaf.match.model.Usuario;
import com.senniaf.match.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DatosDemoConfig {
    @Bean
    CommandLineRunner crearFuncionariosDemo(UsuarioRepository repo, PasswordEncoder encoder, com.senniaf.match.repository.PesosScoringRepository pesos,
                                             @Value("${SENNIAF_DEMO_PASSWORD:}") String password,
                                             @Value("${senniaf.demo.habilitado:false}") boolean habilitado) {
        return args -> {
            pesos.inicializarSiFalta();
            if(!habilitado) return;
            if(password.length()<16 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
                throw new IllegalStateException("La configuración de cuentas ficticias requiere una contraseña explícita de 16 a 72 bytes");
            crear(repo, encoder, password, "TS-001", "Trabajador", "Social", "ts001@senniaf.demo", "TRABAJADOR_SOCIAL");
            crear(repo, encoder, password, "PS-014", "Psicólogo", "Demo", "ps014@senniaf.demo", "PSICOLOGO");
            crear(repo, encoder, password, "CM-001", "Comité", "Demo", "cm001@senniaf.demo", "COMITE");
        };
    }
    private void crear(UsuarioRepository repo, PasswordEncoder encoder, String password, String id,
                       String nombre, String apellido, String correo, String rol) {
        if (repo.findByIdentificador(id).isPresent()) return;
        Usuario u=new Usuario(); u.setIdentificador(id); u.setNombre(nombre); u.setApellido(apellido);
        u.setCorreo(correo); u.setPasswordHash(encoder.encode(password)); u.setRol(rol); u.setVerificado(true); u.setFechaCreacion(LocalDateTime.now()); repo.save(u);
    }
}
