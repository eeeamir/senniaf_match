package com.senniaf.match.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Compara propuestas sin modificar textos guardados ni interpretar su significado. */
final class ContenidoComparable {
    private ContenidoComparable() {}

    static boolean iguales(Object anterior, Object nuevo) {
        if (sinTexto(anterior) && sinTexto(nuevo)) return true;
        if (anterior instanceof String a && nuevo instanceof String b) return texto(a).equals(texto(b));
        if (anterior instanceof Map<?,?> a && nuevo instanceof Map<?,?> b)
            return a.keySet().equals(b.keySet()) && a.keySet().stream().allMatch(k ->
                    "contextoCultural".equals(k) ? Objects.equals(a.get(k), b.get(k)) || sinTexto(a.get(k)) && sinTexto(b.get(k))
                    : java.util.Set.of("habilidades","gustos","personalidad","alergias","condicionesSalud","areas","metodos","apoyos").contains(k)
                        && a.get(k) instanceof List<?> x && b.get(k) instanceof List<?> y
                    ? x.size()==y.size() && x.stream().allMatch(v->java.util.Collections.frequency(x,v)==java.util.Collections.frequency(y,v))
                    : iguales(a.get(k), b.get(k)));
        if (anterior instanceof List<?> a && nuevo instanceof List<?> b) {
            if (a.size() != b.size()) return false;
            for (int i=0; i<a.size(); i++) if (!iguales(a.get(i), b.get(i))) return false;
            return true;
        }
        return Objects.equals(anterior, nuevo);
    }

    private static boolean sinTexto(Object valor) { return valor==null || valor instanceof String s && s.isBlank(); }

    private static String texto(String valor) {
        // Solo un punto o dos puntos al final de una frase; no se tocan
        // cifras, códigos, puntuación interna, mayúsculas ni negaciones.
        return valor.strip().replaceAll("(?<=\\p{L})[.:]$", "");
    }
}
