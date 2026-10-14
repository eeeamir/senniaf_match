package com.senniaf.match.repository;

import com.senniaf.match.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByIdentificador(String identificador);
    Optional<Usuario> findByCedula(String cedula);
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    boolean existsByCedula(String cedula);
    boolean existsByCorreoIgnoreCase(String correo);
}
