package senniaf.match;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProfileController {

    @GetMapping("/perfiles/nino/{id}")
    public ResponseEntity<String> obtenerPerfil(
            @PathVariable String id,
            @RequestHeader(value = "X-Role", defaultValue = "") String role) {

        if (!role.equals("TRABAJADOR_SOCIAL")) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Acceso denegado");
        }

        return ResponseEntity.ok(
                "Perfil ficticio del niño - ID: " + id
        );
    }
}