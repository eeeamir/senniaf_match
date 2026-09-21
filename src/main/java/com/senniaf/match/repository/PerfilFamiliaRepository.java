package com.senniaf.match.repository;

import com.senniaf.match.model.PerfilFamilia;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repositorio en memoria para el Sprint 3. Igual que
 * PerfilNinoRepository, es un punto de partida mientras se trabaja con
 * datos sintéticos; antes de manejar información real debe sustituirse
 * por persistencia real cifrada en reposo (sección 2.11).
 */
@Repository
public class PerfilFamiliaRepository {

    private final Map<String, PerfilFamilia> perfiles = new ConcurrentHashMap<>();
    private final AtomicInteger secuencia = new AtomicInteger(0);

    public String siguienteCodigoCaso() {
        int numero = secuencia.incrementAndGet();
        return String.format("FAM-%03d", numero);
    }

    public PerfilFamilia guardar(PerfilFamilia perfil) {
        perfiles.put(perfil.getCodigoCaso(), perfil);
        return perfil;
    }

    public PerfilFamilia buscarPorCodigoCaso(String codigoCaso) {
        return perfiles.get(codigoCaso);
    }

    public boolean existe(String codigoCaso) {
        return perfiles.containsKey(codigoCaso);
    }
}
