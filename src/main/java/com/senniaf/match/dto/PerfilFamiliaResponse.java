package com.senniaf.match.dto;

/**
 * Respuesta que recibe la familia al completar la entrevista de
 * idoneidad. Ejemplo:
 * {
 *   "mensaje": "Perfil registrado correctamente",
 *   "codigoCaso": "FAM-001",
 *   "perfilCompleto": true
 * }
 */
public class PerfilFamiliaResponse {

    private java.util.List<java.util.Map<String,Object>> actividad = java.util.List.of();
    public java.util.List<java.util.Map<String,Object>> getActividad(){return actividad;}
    public void setActividad(java.util.List<java.util.Map<String,Object>> v){actividad=java.util.List.copyOf(v);}
    private final String mensaje;
    private final String codigoCaso;
    private final boolean perfilCompleto;
    private final boolean reevaluacionPendiente;
    private final int numeroReevaluaciones;
    private final boolean vinculacionAceptada;

    public PerfilFamiliaResponse(String mensaje, String codigoCaso, boolean perfilCompleto) {
        this(mensaje, codigoCaso, perfilCompleto, false, 0);
    }

    public PerfilFamiliaResponse(String mensaje, String codigoCaso, boolean perfilCompleto, boolean pendiente, int numero) {
        this(mensaje,codigoCaso,perfilCompleto,pendiente,numero,false);
    }
    public PerfilFamiliaResponse(String mensaje,String codigoCaso,boolean perfilCompleto,boolean pendiente,int numero,boolean vinculacionAceptada) {
        this.vinculacionAceptada=vinculacionAceptada;
        this.mensaje = mensaje;
        this.codigoCaso = codigoCaso;
        this.perfilCompleto = perfilCompleto;
        this.reevaluacionPendiente = pendiente;
        this.numeroReevaluaciones = numero;
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
    public boolean isReevaluacionPendiente() { return reevaluacionPendiente; }
    public int getNumeroReevaluaciones() { return numeroReevaluaciones; }
    public boolean isVinculacionAceptada() { return vinculacionAceptada; }
    public boolean isRequiereRevisionTecnica() { return numeroReevaluaciones >= 3; }
}
