package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilNinoRequest;
import com.senniaf.match.dto.PerfilNinoResponse;
import com.senniaf.match.model.PerfilNino;
import com.senniaf.match.repository.PerfilNinoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PerfilNinoService {

    private final PerfilNinoRepository repositorio;

    public PerfilNinoService(PerfilNinoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public PerfilNinoResponse registrarPerfil(PerfilNinoRequest request, String registradoPor) {
        String codigoCaso = repositorio.siguienteCodigoCaso();

        PerfilNino perfil = new PerfilNino();
        perfil.setCodigoCaso(codigoCaso);
        perfil.setEdad(request.getEdad());
        perfil.setEscolaridad(request.getEscolaridad());
        perfil.setRendimientoAcademico(request.getRendimientoAcademico());
        perfil.setHabilidades(request.getHabilidades());
        perfil.setGustos(request.getGustos());
        perfil.setPersonalidad(request.getPersonalidad());
        perfil.setNecesidades(request.getNecesidades());
        perfil.setHermanos(request.getHermanos());
        perfil.setContextoCultural(request.getContextoCultural());
        perfil.setRegistradoPor(registradoPor);
        perfil.setFechaRegistro(LocalDateTime.now());
        perfil.setPerfilCompleto(calcularSiEstaCompleto(request));

        repositorio.guardar(perfil);

        // Comportamiento esperado según 2.4: un perfil incompleto no se
        // rechaza, pero queda marcado para que el motor de scoring lo
        // excluya del ranking hasta que se complete.
        String mensaje = perfil.isPerfilCompleto()
                ? "Perfil registrado correctamente"
                : "Perfil registrado, pero está incompleto: no se incluirá en el motor de match hasta completarse";

        return new PerfilNinoResponse(mensaje, codigoCaso, perfil.isPerfilCompleto());
    }

    private boolean calcularSiEstaCompleto(PerfilNinoRequest r) {
        return noVacio(r.getEscolaridad())
                && noVacio(r.getRendimientoAcademico())
                && noVacio(r.getHabilidades())
                && noVacio(r.getGustos())
                && noVacio(r.getPersonalidad())
                && noVacio(r.getNecesidades())
                && noVacio(r.getHermanos());
    }

    private boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }
}
