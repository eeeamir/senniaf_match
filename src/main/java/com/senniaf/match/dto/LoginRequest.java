package com.senniaf.match.dto;
import jakarta.validation.constraints.NotBlank;
public class LoginRequest {
    @NotBlank @jakarta.validation.constraints.Size(max=160) private String identificador;
    @NotBlank @jakarta.validation.constraints.Size(max=72) private String password;
    public String getIdentificador(){return identificador;} public void setIdentificador(String v){identificador=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
}
