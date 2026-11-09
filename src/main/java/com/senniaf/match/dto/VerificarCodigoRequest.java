package com.senniaf.match.dto;
import jakarta.validation.constraints.NotBlank;
public class VerificarCodigoRequest {
    @NotBlank @jakarta.validation.constraints.Size(max=30) private String cedula;
    @NotBlank @jakarta.validation.constraints.Pattern(regexp="[0-9]{6}") private String codigo;
    public String getCedula(){return cedula;} public void setCedula(String v){cedula=v;}
    public String getCodigo(){return codigo;} public void setCodigo(String v){codigo=v;}
}
