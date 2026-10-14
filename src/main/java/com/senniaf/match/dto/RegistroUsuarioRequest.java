package com.senniaf.match.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistroUsuarioRequest {
    @NotBlank private String nombre;
    @NotBlank private String apellido;
    @NotBlank private String cedula;
    @NotBlank @Email private String correo;
    @NotBlank @Size(min=8, message="La contraseña debe tener al menos 8 caracteres") private String password;
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getApellido(){return apellido;} public void setApellido(String v){apellido=v;}
    public String getCedula(){return cedula;} public void setCedula(String v){cedula=v;}
    public String getCorreo(){return correo;} public void setCorreo(String v){correo=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
}
