package com.senniaf.match.repository;

import com.senniaf.match.model.PerfilNino;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repositorio en memoria para el Sprint 2. Suficiente mientras se trabaja
 * solo con datos sintéticos (sección 2.9); antes de manejar información
 * real debe sustituirse por persistencia real cifrada en reposo (2.11),
 * por ejemplo un repositorio JPA sobre una base de datos.
 */
@Repository
public class PerfilNinoRepository {

    private final Map<String, PerfilNino> perfiles = new ConcurrentHashMap<>();
    private final AtomicInteger secuencia = new AtomicInteger(0);

    public String siguienteCodigoCaso() {
        int numero = secuencia.incrementAndGet();
        return String.format("NNA-%03d", numero);
    }

    public PerfilNino guardar(PerfilNino perfil) {
        perfiles.put(perfil.getCodigoCaso(), perfil);
        return perfil;
    }

    public PerfilNino buscarPorCodigoCaso(String codigoCaso) {
        return perfiles.get(codigoCaso);
    }

    public boolean existe(String codigoCaso) {
        return perfiles.containsKey(codigoCaso);
    }
}
