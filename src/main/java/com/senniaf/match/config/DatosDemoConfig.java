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
    CommandLineRunner crearFuncionariosDemo(UsuarioRepository repo, PasswordEncoder encoder,
                                             @Value("${SENNIAF_DEMO_PASSWORD:Cambio123!}") String password) {
        return args -> {
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
