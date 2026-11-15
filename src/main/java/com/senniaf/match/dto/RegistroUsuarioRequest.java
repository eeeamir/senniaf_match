package com.senniaf.match.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.senniaf.match.model.enums.TipoDocumento;
import jakarta.validation.constraints.*;

public class RegistroUsuarioRequest {
    @NotBlank(message="Escribe tu nombre") @Size(max=80,message="El nombre admite hasta 80 caracteres")
    @Pattern(regexp="[\\p{L}][\\p{L}\\p{M} .'‘’·‐‑\\-]*|",message="Usa letras, espacios, tildes, apóstrofos o guiones en el nombre; no números")
    private String nombre;
    @NotBlank(message="Escribe tu apellido") @Size(max=80,message="El apellido admite hasta 80 caracteres")
    @Pattern(regexp="[\\p{L}][\\p{L}\\p{M} .'‘’·‐‑\\-]*|",message="Usa letras, espacios, tildes, apóstrofos o guiones en el apellido; no números")
    private String apellido;
    private TipoDocumento tipoDocumento;
    @Size(max=30,message="El documento admite hasta 30 caracteres") private String numeroDocumento;
    /** Alias legado; las cuentas y los clientes anteriores continúan funcionando. */
    @Size(max=30,message="El documento admite hasta 30 caracteres") private String cedula;
    @NotBlank(message="Escribe tu correo electrónico") @Email(message="Escribe un correo electrónico válido") @Size(max=160,message="El correo admite hasta 160 caracteres") private String correo;
    @NotBlank(message="Escribe tu contraseña") @Size(min=8,max=72,message="La contraseña debe tener entre 8 y 72 caracteres") private String password;
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=nombre(v);}
    public String getApellido(){return apellido;} public void setApellido(String v){apellido=nombre(v);}
    public TipoDocumento getTipoDocumento(){return tipoDocumento;} public void setTipoDocumento(TipoDocumento v){tipoDocumento=v;}
    public String getNumeroDocumento(){return numeroDocumento;} public void setNumeroDocumento(String v){numeroDocumento=TipoDocumento.normalizar(v);}
    public String getCedula(){return cedula;} public void setCedula(String v){cedula=TipoDocumento.normalizar(v);}
    public String getCorreo(){return correo;} public void setCorreo(String v){correo=v==null?null:v.strip();}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    private static String nombre(String v){return v==null?null:java.text.Normalizer.normalize(v,java.text.Normalizer.Form.NFC).strip();}
    @JsonIgnore public String numeroNormalizado(){return numeroDocumento!=null?numeroDocumento:cedula;}
    @JsonIgnore public TipoDocumento tipoEfectivo(){
        if(tipoDocumento!=null)return tipoDocumento;
        return cedula!=null&&cedula.startsWith("E-")?TipoDocumento.CEDULA_RESIDENTE:TipoDocumento.CEDULA_PANAMENA;
    }
    @JsonIgnore @AssertTrue(message="Selecciona el tipo y escribe un número de documento válido; revisa el ejemplo indicado")
    public boolean isDocumentoValido(){
        if(numeroDocumento!=null&&tipoDocumento==null)return false;
        if(cedula!=null&&numeroDocumento!=null&&!cedula.equals(numeroDocumento))return false;
        return tipoEfectivo().admite(numeroNormalizado());
    }
    /** También valida llamadas internas sin el controlador HTTP. */
    public void validar(){
        try(var fabrica=jakarta.validation.Validation.buildDefaultValidatorFactory()){
            var errores=fabrica.getValidator().validate(this);
            if(!errores.isEmpty()){
                var error=errores.stream().sorted(java.util.Comparator.comparing(e->e.getPropertyPath().toString())).findFirst().orElseThrow();
                String campo=error.getPropertyPath().toString();
                throw new com.senniaf.match.web.CampoInvalidoException(campo.equals("documentoValido")?"numeroDocumento":campo,
                        campo.equals("documentoValido")?tipoEfectivo().mensaje():error.getMessage(),false);
            }
        }
    }
}
