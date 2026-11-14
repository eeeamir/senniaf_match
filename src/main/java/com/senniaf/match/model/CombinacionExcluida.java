package com.senniaf.match.model;
@jakarta.persistence.Entity @jakarta.persistence.Table(name="combinaciones_excluidas",uniqueConstraints=@jakarta.persistence.UniqueConstraint(columnNames={"codigoNino","codigoFamilia"}))
public class CombinacionExcluida {
 @jakarta.persistence.Id @jakarta.persistence.GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY) public Long id;
 @jakarta.persistence.Column(nullable=false,length=24) public String codigoNino;
 @jakarta.persistence.Column(nullable=false,length=24) public String codigoFamilia;
 @jakarta.persistence.Column(nullable=false,length=20) public String origen;
 @jakarta.persistence.Column(nullable=false,length=40) public String responsable;
 @jakarta.persistence.Column(nullable=false) public java.time.LocalDateTime fecha;
}
