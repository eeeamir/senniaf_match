package com.senniaf.match.dto;

/**
 * Respuesta que recibe el trabajador social al registrar un perfil.
 * Ejemplo:
 * {
 *   "mensaje": "Perfil registrado correctamente",
 *   "codigoCaso": "NNA-001",
 *   "perfilCompleto": true,
 *   "alertaMedica": false
 * }
 *
 * "alertaMedica" es true cuando el perfil declara alguna alergia,
 * condición de salud o intolerancia distinta de "ninguna/no". No bloquea
 * el registro; es una señal para que el Comité priorice su revisión.
 */
public class PerfilNinoResponse {

    private final String mensaje;
    private final String codigoCaso;
    private final boolean perfilCompleto;
    private final boolean alertaMedica;

    public PerfilNinoResponse(String mensaje, String codigoCaso, boolean perfilCompleto, boolean alertaMedica) {
        this.mensaje = mensaje;
        this.codigoCaso = codigoCaso;
        this.perfilCompleto = perfilCompleto;
        this.alertaMedica = alertaMedica;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getCodigoCaso() {
        return codigoCaso;
    }

    public boolean isPerfilCompleto() {
        return perfilCompleto;
    }

    public boolean isAlertaMedica() {
        return alertaMedica;
    }
}
