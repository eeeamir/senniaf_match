package com.senniaf.match.dto;
import jakarta.validation.constraints.NotBlank;
public class VerificarCodigoRequest {
    @com.fasterxml.jackson.annotation.JsonAlias("identificador")
    @NotBlank(message="Indica la cuenta que deseas verificar") @jakarta.validation.constraints.Size(max=40) private String cedula;
    @NotBlank @jakarta.validation.constraints.Pattern(regexp="[0-9]{6}",message="Escribe los seis dígitos del código") private String codigo;
    public String getCedula(){return cedula;} public void setCedula(String v){cedula=v;}
    public String getCodigo(){return codigo;} public void setCodigo(String v){codigo=v;}
}
