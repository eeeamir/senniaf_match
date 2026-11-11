package com.senniaf.match.dto;
import jakarta.validation.constraints.NotBlank;
public class LoginRequest {
    @NotBlank(message="Escribe tu documento, correo o ID") @jakarta.validation.constraints.Size(max=160,message="El identificador admite hasta 160 caracteres") private String identificador;
    @NotBlank(message="Escribe tu contraseña") @jakarta.validation.constraints.Size(max=72,message="La contraseña admite hasta 72 caracteres") private String password;
    public String getIdentificador(){return identificador;} public void setIdentificador(String v){identificador=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
}
