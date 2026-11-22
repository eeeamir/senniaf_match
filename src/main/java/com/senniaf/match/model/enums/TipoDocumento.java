package com.senniaf.match.model.enums;

import java.util.Locale;
import java.util.regex.Pattern;

/** Validación de formato para registro; no verifica autenticidad ni idoneidad legal. */
public enum TipoDocumento {
    CEDULA_PANAMENA("(?:1[0-3]|[1-9])(?:AV|PI)?-[0-9]{1,4}-[0-9]{1,6}|(?:PE|P|N)-[0-9]{1,4}-[0-9]{1,6}",
            "Escribe la cédula con sus guiones. Ejemplo: 8-123-456; también se admiten las series PE, P, N, AV y PI."),
    CEDULA_RESIDENTE("E-[0-9]{1,4}-[0-9]{1,8}",
            "Escribe la cédula de extranjero residente con el prefijo E y sus guiones. Ejemplo: E-8-123456."),
    PASAPORTE("[A-Z0-9]{3,30}",
            "Escribe entre 3 y 30 letras o números, como aparecen en el pasaporte, sin espacios ni guiones.");

    private final Pattern formato;
    private final String mensaje;
    TipoDocumento(String formato, String mensaje) { this.formato = Pattern.compile(formato); this.mensaje = mensaje; }
    public boolean admite(String numero) { return numero != null && formato.matcher(normalizar(numero)).matches(); }
    public String mensaje() { return mensaje; }
    public static String normalizar(String numero) { return numero == null ? null : numero.strip().toUpperCase(Locale.ROOT); }
    public String identificador(String numero) { return (this == PASAPORTE ? "PAS-" : "") + normalizar(numero); }
}
