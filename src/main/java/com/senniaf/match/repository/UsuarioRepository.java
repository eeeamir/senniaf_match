package com.senniaf.match.repository;

import com.senniaf.match.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(com.senniaf.match.model.enums.TipoDocumento tipoDocumento,String numeroDocumento);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.identificador=:identificador and u.rol='FAMILIA'")
    Optional<Usuario> bloquearPorIdentificador(@org.springframework.data.repository.query.Param("identificador") String identificador);
    Optional<Usuario> findByIdentificador(String identificador);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.cedula=:cedula and u.rol='FAMILIA'")
    Optional<Usuario> bloquearPorCedula(@org.springframework.data.repository.query.Param("cedula") String cedula);
    Optional<Usuario> findByCedula(String cedula);
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    boolean existsByCedula(String cedula);
    boolean existsByCorreoIgnoreCase(String correo);
}
