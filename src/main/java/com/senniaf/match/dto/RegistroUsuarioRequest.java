package com.senniaf.match.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistroUsuarioRequest {
    @NotBlank @Size(max=80) private String nombre;
    @NotBlank @Size(max=80) private String apellido;
    @NotBlank @Size(max=30) private String cedula;
    @NotBlank @Email @Size(max=160) private String correo;
    @NotBlank @Size(min=8, max=72, message="La contraseña debe tener al menos 8 caracteres") private String password;
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getApellido(){return apellido;} public void setApellido(String v){apellido=v;}
    public String getCedula(){return cedula;} public void setCedula(String v){cedula=v;}
    public String getCorreo(){return correo;} public void setCorreo(String v){correo=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
}
