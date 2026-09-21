package com.senniaf.match.service;

import com.senniaf.match.dto.PerfilFamiliaRequest;
import com.senniaf.match.dto.PerfilFamiliaResponse;
import com.senniaf.match.model.PerfilFamilia;
import com.senniaf.match.repository.PerfilFamiliaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PerfilFamiliaService {

    private final PerfilFamiliaRepository repositorio;

    public PerfilFamiliaService(PerfilFamiliaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public PerfilFamiliaResponse registrarPerfil(PerfilFamiliaRequest request, String registradoPor) {
        String codigoCaso = repositorio.siguienteCodigoCaso();

        PerfilFamilia perfil = new PerfilFamilia();
        perfil.setCodigoCaso(codigoCaso);
        perfil.setTipoFamilia(request.getTipoFamilia());
        perfil.setNotaEstructuraHogar(request.getNotaEstructuraHogar());
        perfil.setRedApoyo(request.getRedApoyo());
        perfil.setNotaRedApoyo(request.getNotaRedApoyo());
        perfil.setProvincia(request.getProvincia());
        perfil.setDisponibilidadViaje(request.getDisponibilidadViaje());
        perfil.setNotaDistancia(request.getNotaDistancia());
        perfil.setEdadMinimaAceptada(request.getEdadMinimaAceptada());
        perfil.setEdadMaximaAceptada(request.getEdadMaximaAceptada());
        perfil.setNotaRangoEdad(request.getNotaRangoEdad());
        perfil.setDisposicionHermanos(request.getDisposicionHermanos());
        perfil.setNotaHermanos(request.getNotaHermanos());
        perfil.setCondicionesPuedeAcompanarHoy(request.getCondicionesPuedeAcompanarHoy());
        perfil.setCondicionesConApoyo(request.getCondicionesConApoyo());
        perfil.setNotaCondicionesSalud(request.getNotaCondicionesSalud());
        perfil.setContinuidadCultural(request.getContinuidadCultural());
        perfil.setRegistradoPor(registradoPor);
        perfil.setFechaRegistro(LocalDateTime.now());

        boolean completo = calcularSiEstaCompleto(request);
        perfil.setPerfilCompleto(completo);

        repositorio.guardar(perfil);

        String mensaje = completo
                ? "Perfil registrado correctamente"
                : "Perfil registrado, pero está incompleto: no se incluirá en el motor de match hasta completarse";

        return new PerfilFamiliaResponse(mensaje, codigoCaso, completo);
    }

    private boolean calcularSiEstaCompleto(PerfilFamiliaRequest r) {
        boolean rangoEdadValido = r.getEdadMinimaAceptada() != null
                && r.getEdadMaximaAceptada() != null
                && r.getEdadMinimaAceptada() <= r.getEdadMaximaAceptada();

        return r.getTipoFamilia() != null
                && noVacia(r.getRedApoyo())
                && r.getProvincia() != null
                && r.getDisponibilidadViaje() != null
                && rangoEdadValido
                && r.getDisposicionHermanos() != null;
    }

    private boolean noVacia(List<?> lista) {
        return lista != null && !lista.isEmpty();
    }
}
