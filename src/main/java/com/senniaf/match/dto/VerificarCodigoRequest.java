package com.senniaf.match.dto;
import jakarta.validation.constraints.NotBlank;
public class VerificarCodigoRequest {
    @NotBlank private String cedula;
    @NotBlank private String codigo;
    public String getCedula(){return cedula;} public void setCedula(String v){cedula=v;}
    public String getCodigo(){return codigo;} public void setCodigo(String v){codigo=v;}
}
